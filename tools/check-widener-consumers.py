#!/usr/bin/env python3
"""Map every nbteditor.accesswidener line to the mod classes that use it.

ADR-0003 wants two things the compiler cannot say on its own: no widener line without a
consumer, and every access to a widened member inside util/AccessWidenedApi, save the exceptions
the ADR names and EXEMPT repeats. Both are read off the constant pools of the mod's own classes,
so a reference through a variable or a lambda counts the same as a direct one.

A consumer only counts when vanilla's own modifiers would have refused it. A mod screen calling
Screen.addRenderableWidget, which is protected, is not using the widener; a mod widget touching
AbstractWidget.x, which is private, is. Reading those modifiers needs the *un-widened* game jar,
so pass it ahead of the compile classpath, which holds the jar Loom already widened. Handing this
only the widened jar would make every line look reachable and every check pass, so it refuses to
run when nothing on the classpath is still private.

Usage: check-widener-consumers.py <widener> <classes-dir|classpath-jar>...
"""
import os
import re
import subprocess
import sys
from collections import defaultdict
from pathlib import Path

OWNER = "com/luneruniverse/minecraft/mod/nbteditor"
API = OWNER + "/util/AccessWidenedApi"

# ADR-0003's one exception. SuggestingTextFieldWidget drives a vanilla CommandSuggestions and
# overrides updateCommandInfo, whose body reads and writes the fields that method owns. A static
# helper cannot stand in for `this.keepSuggestions` inside an override. An entry that matches
# nothing fails the run too, so the list shrinks when the code stops needing it.
EXEMPT = [(OWNER + "/screens/widgets/SuggestingTextFieldWidget",
		"net/minecraft/client/gui/components/CommandSuggestions")]

REF = re.compile(r"=\s+(?:Fieldref|Methodref|InterfaceMethodref)\s+\S+\s+//\s+"
		r"([\w/$]+)\.\"?([\w$<>]+)\"?:(\S+)")
# A Class entry also appears for the owner of any field or method reference, so this
# over-counts a consumer rather than under-counting one. It is here to find lines with no
# consumer at all, which is the direction that stays sound.
CLS = re.compile(r"=\s+Class\s+\S+\s+//\s+([\w/$]+)\s*$")


def parse_widener(path):
	entries = []
	for lineno, raw in enumerate(path.read_text().splitlines(), 1):
		line = raw.strip()
		if not line or line.startswith("#") or line.startswith("accessWidener"):
			continue
		parts = line.split()
		kind, what = parts[0], parts[1]
		if what == "class":
			entries.append((lineno, kind, "class", parts[2], None, None))
		else:
			entries.append((lineno, kind, what, parts[2], parts[3], parts[4]))
	return entries


def read_refs(names):
	"""Member and class references per mod class, keyed by name and descriptor.

	javac writes an inherited field under the *qualifying* type, so an anonymous subclass of
	CommandSuggestions reading its own keepSuggestions names itself as the owner, not the class
	that declares the field. References are therefore indexed by name and descriptor, and the
	owner is matched against the widened class through the hierarchy in `owns`.
	"""
	members = defaultdict(lambda: defaultdict(set))
	classes = defaultdict(set)
	class_files = sorted(names)
	for chunk in [class_files[i:i + 200] for i in range(0, len(class_files), 200)]:
		out = subprocess.run(["javap", "-v", "-p", *[str(c) for c in chunk]],
				capture_output=True, text=True).stdout
		current = None
		for line in out.splitlines():
			if line.startswith("Classfile "):
				current = names[Path(line[len("Classfile "):].strip())]
			elif current:
				m = REF.search(line)
				if m:
					owner, name, desc = m.group(1, 2, 3)
					members[(name, desc)][owner].add(current)
					continue
				m = CLS.search(line)
				if m:
					classes[m.group(1)].add(current)
	return members, classes


def javap(classpath, name, cache={}):
	key = (classpath, name)
	if key not in cache:
		cache[key] = subprocess.run(["javap", "-p", "-s", "-cp", classpath, name.replace("/", ".")],
				capture_output=True, text=True).stdout
	return cache[key]


def parents(classpath, name):
	head = javap(classpath, name).split("{", 1)[0]
	found = []
	for keyword in ("extends", "implements"):
		if keyword in head:
			tail = head.split(keyword, 1)[1]
			if keyword == "extends" and "implements" in tail:
				tail = tail.split("implements", 1)[0]
			for parent in tail.split(","):
				parent = parent.strip().split()[0].replace(".", "/") if parent.strip() else ""
				if parent and parent != name:
					found.append(parent)
	return found


def declaration(classpath, name, member, desc):
	"""The declaration line for a member a class declares itself, or None if it inherits it."""
	lines = javap(classpath, name).splitlines()
	simple = name.rsplit("/", 1)[-1].rsplit("$", 1)[-1]
	for decl, following in zip(lines, lines[1:]):
		if "descriptor: " not in following:
			continue
		if following.split("descriptor: ")[1].strip() != desc:
			continue
		head = decl.strip().rstrip(";").split("(")[0].strip()
		if not head:
			continue
		last = head.split()[-1]
		if last == member or (member == "<init>" and last.rsplit(".", 1)[-1] == simple):
			return head
	return None


def declarer(classpath, start, member, desc, cache={}):
	"""The class a reference written against `start` actually resolves to.

	A widget that declares its own private int x shadows AbstractWidget's, and both are `I`, so
	matching on name and descriptor alone credits the wrong widener line. Walking up from the
	qualifying type and stopping at the first class that declares the member separates them.
	"""
	key = (start, member, desc)
	if key not in cache:
		cache[key] = start
		queue, seen = [start], set()
		while queue:
			cls = queue.pop(0)
			if cls in seen:
				continue
			seen.add(cls)
			if declaration(classpath, cls, member, desc):
				cache[key] = cls
				break
			queue.extend(parents(classpath, cls))
	return cache[key]


def in_mixin(consumer):
	"""A mixin is compiled into the class it targets, so its access has nowhere else to go."""
	return "/mixin/" in consumer


def nested(name, outer):
	return name == outer or name.startswith(outer + "$")


def exemption(owner, consumer):
	"""The EXEMPT entry covering this access, or None."""
	for entry in EXEMPT:
		if nested(consumer, entry[0]) and nested(owner, entry[1]):
			return entry
	return None


def reachable_unwidened(classpath, owner, member, desc, consumer):
	"""Whether the consumer could touch this member with the widener line deleted.

	Every line in the file is `accessible`, which makes the member public, so a consumer only
	proves the line is earning its place if vanilla's own modifiers would refuse the access.
	Screen.addRenderableWidget is protected, so a mod screen calling its own inherited copy is
	not a consumer; AbstractWidget.x is private, so a mod widget touching it is.
	"""
	decl = declaration(classpath, owner, member, desc) or ""
	words = decl.split()
	if "public" in words:
		return True
	if "private" in words:
		return False
	while consumer:  # protected, or package-private, which no mod class can be a peer of
		if consumer == owner or owner in parents_closure(classpath, consumer):
			return True
		consumer = consumer.rsplit("$", 1)[0] if "$" in consumer else None
	return False


def parents_closure(classpath, name, cache={}):
	if name not in cache:
		cache[name] = set()
		found = set()
		for parent in parents(classpath, name):
			found.add(parent)
			found |= parents_closure(classpath, parent)
		cache[name] = found
	return cache[name]


def check_unwidened(classpath, entries):
	"""Refuse a classpath whose game jar has already had the widener applied to it.

	Loom puts the widened jar on compileClasspath, and against that one every widened member
	reads as public, every consumer looks like it could have reached the member anyway, and the
	whole check passes while proving nothing.
	"""
	# Only `accessible` lines, since `extendable` promotes a member to protected and leaves it
	# looking closed on either jar.
	closed = [e for e in entries if e[1] == "accessible" and e[2] != "class"
			and "public" not in (declaration(classpath, e[3], e[4], e[5]) or "").split()]
	if not closed:
		sys.exit("every widened member is already public on this classpath: pass the un-widened "
				"game jar ahead of the compile classpath")


def main():
	widener = Path(sys.argv[1])
	entries = [Path(p) for a in sys.argv[2:] for p in a.split(os.pathsep) if p]
	names = {f.resolve(): str(f.relative_to(d))[:-len(".class")]
			for d in entries if d.is_dir() for f in d.rglob("*.class")}
	if not names:
		sys.exit("no compiled classes; run compileJava and compileDevJava first")
	# The game jar has to be here too: a mod widget writing this.x names itself as the owner,
	# and reaching AbstractWidget from there means walking a chain that runs through vanilla.
	classpath = os.pathsep.join(str(e) for e in entries)
	members, classes = read_refs(names)
	check_unwidened(classpath, parse_widener(widener))

	dead, stray, used = [], [], set()
	for lineno, kind, what, owner, name, desc in parse_widener(widener):
		if what == "class":
			users = classes.get(owner, set())
			label = f"class {owner}"
		elif kind == "extendable":
			continue  # An override is not a reference; the subclass is its own consumer.
		else:
			users = set()
			for ref_owner, consumers in members.get((name, desc), {}).items():
				if declarer(classpath, ref_owner, name, desc) == owner:
					users |= consumers
			label = f"{what} {owner}.{name}"
		users = {u for u in users if u.startswith(OWNER)}
		if not users:
			dead.append((lineno, label))
		outside = []
		for u in sorted(users):
			if u.startswith(API) or in_mixin(u) or reachable_unwidened(classpath, owner, name, desc, u):
				continue
			covered = exemption(owner, u)
			if covered:
				used.add(covered)
			else:
				outside.append(u)
		if what != "class" and outside:
			stray.append((lineno, label, outside))

	for lineno, label in dead:
		print(f"DEAD   :{lineno} {label}")
	for lineno, label, outside in stray:
		print(f"STRAY  :{lineno} {label}")
		for u in outside:
			print(f"           {u}")
	rotten = [e for e in EXEMPT if e not in used]
	for consumer, owner in rotten:
		print(f"UNUSED exemption {consumer} -> {owner}")
	if not dead and not stray and not rotten:
		print(f"every widener line has a consumer, and every access is in AccessWidenedApi "
				f"or one of the {len(EXEMPT)} exemption(s)")
		return 0
	print(f"\n{len(dead)} line(s) with no consumer, {len(stray)} accessed outside "
			f"AccessWidenedApi, {len(rotten)} exemption(s) matching nothing")
	return 1


if __name__ == "__main__":
	sys.exit(main())
