"""Resolve every mixin reference against the loom-mapped Minecraft jar.

A game bump renames the members mixins point at, and nothing in the build notices:
`method = "render"` and `@At(target = "L...;setScreen(...)V")` are plain strings that
compile and remap clean. Each one then fails at launch, one crash per run, and a
mixin on a screen class does not fail until that screen opens.

This resolves the same four things mixin resolves, before launch:

  - the `@Mixin` target class exists
  - the `method` selector matches exactly one non-bridge method on it
  - the `@At` target member exists, with that descriptor
  - the selected method's bytecode actually contains that call or field access

Runs as part of `./gradlew check`, which passes the whole compile classpath, so
library owners like `com.mojang.serialization.DynamicOps` resolve too. Standalone
it falls back to the mapped jar alone, found from `minecraft_version`, and reports
the library owners as unchecked:

    python3 tools/check-mixin-targets.py [classpath]

An owner that is on the classpath and still missing the member is a failure. An
owner that is not on the classpath at all is unchecked.

Intermediary names are failures, not exemptions. The jar ships no refMap, so mixin
never translates one and the injection dies at apply time. Reflection through
`Reflection.java` is the opposite case and stays intermediary, because Fabric's
MappingResolver does translate at runtime.
"""
import collections
import os
import re
import subprocess
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "src/main/java")
JAVAP = os.path.expanduser("~/.sdkman/candidates/java/current/bin/javap")
INTERMEDIARY = re.compile(r'^(method|field|comp)_\d+$')

Member = collections.namedtuple("Member", "name desc bridge refs")


def find_classpath():
    if len(sys.argv) > 1:
        return sys.argv[1]
    base = os.path.expanduser("~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft")
    version = re.search(r'^\s*minecraft_version\s*=\s*(\S+)',
                        open(os.path.join(ROOT, "gradle.properties")).read(), re.M).group(1)
    jar = os.path.join(base, "minecraft-merged-deobf", version, f"minecraft-merged-deobf-{version}.jar")
    if not os.path.exists(jar):
        sys.exit(f"mapped jar not found at {jar}; run ./gradlew build first")
    return jar


CLASSPATH = find_classpath()
_classes = {}


def load(name):
    """Parse one class out of the jar. Returns None when it is not there."""
    if name in _classes:
        return _classes[name]
    p = subprocess.run([JAVAP, "-p", "-v", "-classpath", CLASSPATH, name], capture_output=True, text=True)
    if p.returncode != 0 or "Error:" in p.stderr:
        _classes[name] = None
        return None
    lines = p.stdout.splitlines()
    decl = next((l for l in lines if re.match(r'^(public |final |abstract )*(class|interface|enum) ', l)), "")
    sup = next((m.group(1).replace("/", ".") for l in lines
                for m in [re.match(r'\s*super_class:.*//\s*(\S+)', l)] if m), None)
    impl = []
    if (m := re.search(r'\bimplements\s+([^{]+)', decl)):
        impl = [c.strip().split("<")[0] for c in m.group(1).split(",")]
    simple = name.split(".")[-1]

    methods, fields, member, in_code = collections.defaultdict(list), collections.defaultdict(set), None, False
    body = lines[lines.index("{") + 1:] if "{" in lines else []
    for line in body:
        if re.match(r'^  \S', line):
            member, in_code = {"sig": line.strip(), "refs": set()}, False
            continue
        if member is None:
            continue
        if (m := re.match(r'\s*descriptor:\s*(\S+)', line)):
            member["desc"] = m.group(1)
        elif (m := re.match(r'\s*flags:\s*(.*)', line)):
            member["flags"] = m.group(1)
            name_match = re.match(r'.*?([\w$<>]+)\s*\(', member["sig"]) if "(" in member["sig"] \
                else re.search(r'([\w$]+)\s*;\s*$', member["sig"])
            if not name_match or "desc" not in member:
                member = None
                continue
            nm = name_match.group(1)
            if nm.split(".")[-1] == simple:
                nm = "<init>"
            member["name"] = nm
            if member["desc"].startswith("("):
                methods[nm].append(member)
            else:
                fields[nm].add(member["desc"])
                member = None
        elif line.strip() == "Code:":
            in_code = True
        elif in_code and (m := re.search(r'//\s*(?:Interface)?Method\s+(\S+)', line)):
            member["refs"].add(m.group(1))
        elif in_code and (m := re.search(r'//\s*Field\s+(\S+)', line)):
            member["refs"].add(m.group(1))

    info = {
        "methods": {k: [Member(v["name"], v["desc"], "ACC_BRIDGE" in v.get("flags", ""), v["refs"])
                        for v in vs] for k, vs in methods.items()},
        "fields": fields, "super": sup, "impl": impl,
    }
    _classes[name] = info
    return info


def lookup(cls, member, kind):
    """First class in the hierarchy declaring `member`, and what it declares.

    Constructors are not inherited, so the walk stops at `cls` for `<init>`.
    """
    queue, seen = [cls], set()
    while queue:
        c = queue.pop(0)
        if c in seen:
            continue
        seen.add(c)
        info = load(c)
        if not info:
            continue
        if (hit := info[kind].get(member)):
            return c, hit
        if member == "<init>":
            return None, None
        if info["super"]:
            queue.append(info["super"])
        queue.extend(info["impl"])
    return None, None


def parse_at(target):
    """"Lowner;name(args)ret" or "Lowner;name:Ldesc;" -> (owner, name, desc, kind)."""
    if (m := re.match(r'L([\w/$]+);([\w<>$]+)(\(.*)$', target)):
        return m.group(1).replace("/", "."), m.group(2), m.group(3), "methods"
    if (m := re.match(r'L([\w/$]+);([\w$]+):(.+)$', target)):
        return m.group(1).replace("/", "."), m.group(2), m.group(3), "fields"
    return None


INJECTOR = (r'@(?:Inject|Redirect|ModifyArg|ModifyArgs|ModifyVariable|ModifyConstant|ModifyReturnValue'
            r'|ModifyExpressionValue|WrapOperation|WrapWithCondition)\s*\((.*?)\)\s*\n\s*'
            r'(?:private|public|protected|static)')

problems, unchecked = set(), set()


def report(where, message, checkable=True):
    (problems if checkable else unchecked).add(f"{where}: {message}")


def check(path):
    text = open(path).read()
    where = os.path.relpath(path, ROOT)
    imports = {i.split(".")[-1]: i for i in re.findall(r'^import\s+(?:static\s+)?([\w.$]+);', text, re.M)}

    mx = re.search(r'@Mixin\s*\((.*?)\)\s*(?:public|abstract|class|interface|@)', text, re.S)
    if not mx:
        return
    targets = []
    for cls in re.findall(r'([\w.]+)\.class', mx.group(1)):
        head, *nested = cls.split(".")
        targets.append(imports.get(head, head) + ("$" + "$".join(nested) if nested else ""))
    targets += re.findall(r'"([\w.$]+)"', mx.group(1))
    for t in targets:
        if load(t) is None:
            report(where, f"@Mixin target not on the classpath: {t}", False)
    targets = [t for t in targets if load(t)]

    for ann in re.findall(INJECTOR, text, re.S):
        selectors = []
        for braced, single in re.findall(r'method\s*=\s*(?:\{([^}]*)\}|"([^"]*)")', ann):
            selectors += re.findall(r'"([^"]*)"', braced) if braced else [single]
        selectors += re.findall(r'@Desc\s*\(\s*value\s*=\s*"([^"]*)"', ann)
        # An @At target holds its own parentheses, so split on the annotation name
        # rather than trying to balance them.
        ats = [(re.search(r'value\s*=\s*"(\w+)"', at), re.search(r'target\s*=\s*"(L[^"]+)"', at))
               for at in re.split(r'@At\b', ann)[1:]]

        for selector in selectors:
            name = selector.split("(")[0]
            if INTERMEDIARY.match(name):
                report(where, f'method = "{selector}" is an intermediary name, and the jar '
                              f'ships no refMap for mixin to resolve it with')
                continue
            for t in targets:
                owner, found = lookup(t, name, "methods")
                if not found:
                    report(where, f'method = "{selector}" -- {t} has no method {name}')
                    continue
                real = [m for m in found if not m.bridge]
                if "(" not in selector and len(real) > 1:
                    report(where, f'method = "{selector}" -- AMBIGUOUS on {owner}, '
                                  f'name a descriptor: {sorted(m.desc for m in real)}')
                    continue
                wanted = selector[len(name):]
                picked = [m for m in real if not wanted or m.desc == wanted]
                if wanted and not picked:
                    report(where, f'method = "{selector}" -- {owner}.{name} has no such descriptor, '
                                  f'jar has {sorted(m.desc for m in real)}')
                    continue
                for value, target in ats:
                    if not target or (value and value.group(1) not in ("INVOKE", "FIELD", "NEW",
                                                                      "INVOKE_ASSIGN")):
                        continue
                    at = parse_at(target.group(1))
                    if not at:
                        continue
                    at_owner, at_name, at_desc, kind = at
                    if load(at_owner) is None:
                        report(where, f"@At owner not on the classpath: {at_owner}", False)
                        continue
                    _, have = lookup(at_owner, at_name, kind)
                    if not have:
                        report(where, f"@At {at_owner}.{at_name} -- no such {kind[:-1]}")
                        continue
                    descs = [m.desc for m in have] if kind == "methods" else sorted(have)
                    if at_desc not in descs:
                        report(where, f"@At {at_owner}.{at_name}{at_desc} -- descriptor mismatch, "
                                      f"jar has {sorted(descs)}")
                        continue
                    # javap omits the owner on a self-reference, so a call inside the
                    # declaring class prints as a bare `name:desc`.
                    shown = f'"{at_name}"' if at_name == "<init>" else at_name
                    refs = {f"{at_owner.replace('.', '/')}.{shown}:{at_desc}"}
                    if at_owner == owner:
                        refs.add(f"{shown}:{at_desc}")
                    if not any(r in m.refs for m in picked for r in refs):
                        report(where, f'@At {at_owner}.{at_name} is never reached from '
                                      f'{owner}.{name}{picked[0].desc if picked else ""}')

    for name in re.findall(r'@(?:Accessor|Invoker)\s*\(\s*"([\w$]+)"', text):
        for t in targets:
            if not (lookup(t, name, "fields")[1] or lookup(t, name, "methods")[1]):
                report(where, f'@Accessor/@Invoker "{name}" -- {t} has no such member')

    for decl in re.findall(r'@Shadow[^;]*?\n\s*(?:public|private|protected)[^;{]*', text):
        shadow = re.search(r'([\w$]+)\s*\(', decl) or re.search(r'([\w$]+)\s*$', decl.strip())
        if not shadow:
            continue
        name, kind = shadow.group(1), "methods" if "(" in decl else "fields"
        for t in targets:
            if not lookup(t, name, kind)[1]:
                report(where, f'@Shadow {kind[:-1]} "{name}" -- {t} has no such member')


for directory, _, files in os.walk(SRC):
    if "/mixin" not in directory.replace(os.sep, "/"):
        continue
    for f in sorted(files):
        if f.endswith(".java"):
            check(os.path.join(directory, f))

if not any(_classes.values()):
    sys.exit("nothing on the classpath resolved; every check would pass vacuously")

for p in sorted(problems):
    print(p)
if unchecked:
    print("\nunchecked:")
    for u in sorted(unchecked):
        print("  " + u)
print(f"\n{len(problems)} problem(s), {len(unchecked)} unchecked")
sys.exit(1 if problems else 0)
