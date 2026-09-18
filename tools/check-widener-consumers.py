#!/usr/bin/env python3
"""Map every nbteditor.accesswidener line to the mod classes that use it.

ADR-0003 wants two things the compiler cannot say on its own: no widener line without a
consumer, and every access to a widened field or method inside util/AccessWidenedApi. Both are
read off the constant pools of the mod's own classes, so a reference through a variable or a
lambda counts the same as a direct one.

Usage: check-widener-consumers.py <widener> <classes-dir>...
"""
import re
import subprocess
import sys
from collections import defaultdict
from pathlib import Path

OWNER = "com/luneruniverse/minecraft/mod/nbteditor"
API = OWNER + "/util/AccessWidenedApi"

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


def read_refs(class_files):
	"""owner/name/desc and class references per mod class, from the constant pools."""
	members = defaultdict(set)
	classes = defaultdict(set)
	for chunk in [class_files[i:i + 200] for i in range(0, len(class_files), 200)]:
		out = subprocess.run(["javap", "-v", "-p", *[str(c) for c in chunk]],
				capture_output=True, text=True).stdout
		current = None
		for line in out.splitlines():
			if line.startswith("Classfile "):
				current = line.split("build/classes/java/")[-1].split("/", 1)[-1][:-6]
			elif current:
				m = REF.search(line)
				if m:
					members[m.group(1, 2, 3)].add(current)
					continue
				m = CLS.search(line)
				if m:
					classes[m.group(1)].add(current)
	return members, classes


def main():
	widener, dirs = Path(sys.argv[1]), [Path(d) for d in sys.argv[2:]]
	class_files = sorted(f for d in dirs for f in d.rglob("*.class"))
	if not class_files:
		sys.exit("no compiled classes; run compileJava first")
	members, classes = read_refs(class_files)

	dead, stray = [], []
	for lineno, kind, what, owner, name, desc in parse_widener(widener):
		if what == "class":
			users = classes.get(owner, set())
			label = f"class {owner}"
		elif kind == "extendable":
			continue  # An override is not a reference; the subclass is its own consumer.
		else:
			users = members.get((owner, name, desc), set())
			label = f"{what} {owner}.{name}"
		users = {u for u in users if u.startswith(OWNER)}
		if not users:
			dead.append((lineno, label))
		outside = sorted(u for u in users if not u.startswith(API))
		if what != "class" and outside:
			stray.append((lineno, label, outside))

	for lineno, label in dead:
		print(f"DEAD   :{lineno} {label}")
	for lineno, label, outside in stray:
		print(f"STRAY  :{lineno} {label}")
		for u in outside:
			print(f"           {u}")
	if not dead and not stray:
		print("every widener line has a consumer, and every access is in AccessWidenedApi")
		return 0
	print(f"\n{len(dead)} line(s) with no consumer, {len(stray)} accessed outside AccessWidenedApi")
	return 1


if __name__ == "__main__":
	sys.exit(main())
