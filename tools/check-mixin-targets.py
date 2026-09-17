"""Check every mixin target and member reference against the loom-mapped Minecraft jar.

A game bump renames or moves the members mixins point at, and nothing in the build
notices: `method = "render"` and `@At(target = "L...;setScreen(...)V")` are plain
strings. The mismatch surfaces as a launch crash, one per run. This reports them all
at once.

Run it after bumping `minecraft_version`, against the jar loom already produced:

    python3 tools/check-mixin-targets.py

Known false positives, all from javap output this does not fully model:
  - `method = "init"` on a Screen subclass. Ambiguous only because `Screen.init(II)V`
    is inherited; the subclass declares `init()V` and mixin resolves there first.
  - `getId` on `Registry$1`. The `(Ljava/lang/Object;)I` arm is ACC_BRIDGE.
  - `<init>` on a nested class. The constructor is named for the outer class too, so
    it is not recognised as a constructor.
  - `method_*` names. Deliberate intermediary names with no Mojang equivalent; they
    are remapped at build time and cannot be resolved against a Mojang-mapped jar.
"""
import re, os, subprocess, collections

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "src/main/java")
JAR = os.path.expanduser("~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged-deobf/26.2/minecraft-merged-deobf-26.2.jar")
JAVAP = os.path.expanduser("~/.sdkman/candidates/java/current/bin/javap")

_cache = {}
def klass(name):
    """{'methods': {name: {desc: bridge?}}, 'fields': {name: {desc}}, 'super': str} or None."""
    if name in _cache: return _cache[name]
    p = subprocess.run([JAVAP, "-p", "-v", "-classpath", JAR, name], capture_output=True, text=True)
    if p.returncode != 0 or "Error:" in p.stderr:
        _cache[name] = None
        return None
    simple = name.split(".")[-1].split("$")[-1]
    methods, fields, sup = collections.defaultdict(dict), collections.defaultdict(set), None
    lines = p.stdout.splitlines()
    for i, line in enumerate(lines):
        m = re.match(r'\s{2}\S.*?([\w$<>]+)\s*(\(|;\s*$)', line)
        if not m or i + 2 >= len(lines) or "descriptor:" not in lines[i+1]:
            continue
        desc = lines[i+1].split("descriptor:")[1].strip()
        flags = lines[i+2] if "flags:" in lines[i+2] else ""
        nm = m.group(1)
        if desc.startswith("("):
            if nm == simple: nm = "<init>"
            if nm == "<clinit>" or "static {" in line: continue
            methods[nm][desc] = ("ACC_BRIDGE" in flags or "ACC_SYNTHETIC" in flags)
        else:
            fields[nm].add(desc)
    sm = re.search(r'^\s*super_class:.*//\s*(\S+)', p.stdout, re.M)
    if sm: sup = sm.group(1).replace("/", ".")
    ifaces = re.findall(r'^\s*#\d+.*', "")  # interfaces walked via javap class line below
    cm = re.search(r'^\w.*\bimplements\s+([^{]+)\{', p.stdout, re.M)
    impl = [c.strip().split("<")[0] for c in cm.group(1).split(",")] if cm else []
    out = {"methods": methods, "fields": fields, "super": sup, "impl": impl}
    _cache[name] = out
    return out

def resolve(name, member, want):
    if member == "<init>":
        info = klass(name)
        return None if info is None else (dict(info["methods"].get("<init>", {})) or None)
    """Walk the class, its superclasses and interfaces for `member`. Returns dict/set or None."""
    seen, queue = set(), [name]
    found = {} if want == "methods" else set()
    missing_root = klass(name) is None
    while queue:
        c = queue.pop(0)
        if c in seen: continue
        seen.add(c)
        info = klass(c)
        if not info: continue
        hit = info[want].get(member)
        if hit:
            if want == "methods": found.update(hit)
            else: found |= hit
        if info["super"]: queue.append(info["super"])
        queue.extend(info["impl"])
    return None if missing_root else (found or None)

mixin_files = [os.path.join(d, f) for d, _, fs in os.walk(SRC) for f in fs
               if f.endswith(".java") and "/mixin" in d.replace(os.sep, "/")]

def imports(text):
    return {i.split(".")[-1]: i for i in re.findall(r'^import\s+(?:static\s+)?([\w.$]+);', text, re.M)}

def targets_of(text, imp):
    mx = re.search(r'@Mixin\s*\((.*?)\)\s*(?:public|abstract|class|interface|@)', text, re.S)
    if not mx: return []
    body, out = mx.group(1), []
    for cls in re.findall(r'([\w.]+)\.class', body):
        parts = cls.split(".")
        out.append(imp.get(parts[0], parts[0]) + ("$" + "$".join(parts[1:]) if len(parts) > 1 else ""))
    out += re.findall(r'"([\w.$]+)"', body)
    return out

INJECTORS = r'@(?:Inject|Redirect|ModifyArg|ModifyArgs|ModifyVariable|ModifyConstant|ModifyReturnValue|ModifyExpressionValue|WrapOperation|WrapWithCondition)'
problems, skipped = set(), set()
for path in mixin_files:
    text, rel = open(path).read(), os.path.relpath(path, ROOT)
    imp = imports(text)
    tgts = targets_of(text, imp)
    for t in tgts:
        if klass(t) is None:
            (problems if t.startswith("net.minecraft") else skipped).add(f"{rel}: @Mixin target not in jar: {t}")
    # @At(target = "Lowner;name(desc)ret" | "Lowner;name:Ldesc;")
    for tgt in re.findall(r'target\s*=\s*"(L[^"]+)"', text):
        m = re.match(r'L([\w/$]+);([\w<>$]+)(\(.*)$', tgt) or re.match(r'L([\w/$]+);([\w$]+):(.+)$', tgt)
        if not m: continue
        owner, name, desc = m.group(1).replace("/", "."), m.group(2), m.group(3)
        want = "methods" if desc.startswith("(") else "fields"
        if klass(owner) is None:
            (problems if owner.startswith("net.minecraft") else skipped).add(f"{rel}: @At owner not in jar: {owner}")
            continue
        have = resolve(owner, name, want)
        if not have:
            problems.add(f"{rel}: @At {owner}.{name} -- no such {want[:-1]}")
        elif desc not in have:
            problems.add(f"{rel}: @At {owner}.{name}{desc} -- descriptor mismatch, jar has {sorted(have)}")
    # method = "..." on injector annotations
    for ann in re.findall(INJECTORS + r'\s*\((.*?)\)\s*\n?\s*(?:private|public|protected)', text, re.S):
        for grp in re.findall(r'method\s*=\s*(?:\{([^}]*)\}|"([^"]*)")', ann):
            for name in (re.findall(r'"([^"]*)"', grp[0]) if grp[0] else [grp[1]]):
                if "(" in name: continue          # already fully qualified
                for t in tgts:
                    if klass(t) is None: continue
                    have = resolve(t, name, "methods")
                    if not have:
                        problems.add(f"{rel}: method=\"{name}\" -- {t} has no such method")
                        continue
                    real = [d for d, bridge in have.items() if not bridge]
                    if len(real) > 1:
                        problems.add(f"{rel}: method=\"{name}\" -- AMBIGUOUS in {t}: {sorted(real)}")
    # @Accessor("x") / @Invoker("x")
    for name in re.findall(r'@(?:Accessor|Invoker)\s*\(\s*"([\w$]+)"', text):
        for t in tgts:
            if klass(t) is None: continue
            if not (resolve(t, name, "fields") or resolve(t, name, "methods")):
                problems.add(f"{rel}: @Accessor/@Invoker \"{name}\" -- {t} has no such member")

print("PROBLEMS")
for p in sorted(problems): print("  " + p)
print(f"\nSKIPPED (owner not a Minecraft class, cannot check)")
for s in sorted(skipped): print("  " + s)
print(f"\n{len(problems)} problem(s), {len(skipped)} unchecked, across {len(mixin_files)} mixin files")
