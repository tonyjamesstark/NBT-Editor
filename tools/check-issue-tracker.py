#!/usr/bin/env python3
"""Holds `.scratch/` to docs/agents/issue-tracker.md.

There is no `gh` CLI on this host, so a ticket that drifts out of the shape the skills expect is
not a cosmetic problem: it is a ticket the next agent does not find. The conventions were prose
with no instance for a day, and the one Status line that existed was stale, which is what this
checks instead.

Usage: check-issue-tracker.py <scratch-dir>
"""

import re
import sys
from pathlib import Path

# docs/agents/triage-labels.md, plus the two states the wayfinder operations use.
ROLES = {"needs-triage", "needs-info", "ready-for-agent", "ready-for-human", "wontfix",
		"claimed", "resolved"}
NAME = re.compile(r"^(\d\d)-[a-z0-9]+(?:-[a-z0-9]+)*\.md$")
STATUS = re.compile(r"^Status:\s*`?([a-z-]+)`?", re.MULTILINE)
COMBINED = {"tickets.md", "issues.md"}


def check_issue(path):
	"""The Status line and the conversation heading, which are what a skill reads back."""
	problems = []
	text = path.read_text()
	head = "".join(text.splitlines(keepends=True)[:12])
	found = STATUS.search(head)
	if not found:
		problems.append(f"{path}: no `Status:` line in the first 12 lines")
	elif found.group(1) not in ROLES:
		problems.append(f"{path}: Status `{found.group(1)}` is not one of {sorted(ROLES)}")
	if "## Comments" not in text and "## Answer" not in text:
		problems.append(f"{path}: no `## Comments` or `## Answer` heading to append under")
	return problems


def check_effort(effort):
	problems = []
	for stray in COMBINED:
		if (effort / stray).exists():
			problems.append(f"{effort / stray}: a combined ticket file, not one file per ticket")
	issues = effort / "issues"
	if not issues.is_dir():
		return problems
	if not (effort / "spec.md").exists() and not (effort / "map.md").exists():
		problems.append(f"{effort}: has issues/ but neither spec.md nor map.md")
	numbers = []
	for path in sorted(issues.iterdir()):
		if path.name.startswith("."):
			continue
		found = NAME.match(path.name)
		if not found:
			problems.append(f"{path}: not NN-slug.md, lowercase and hyphenated")
			continue
		numbers.append(int(found.group(1)))
		problems += check_issue(path)
	# Numbered from 01 and contiguous, so "the next number" is never ambiguous and a deleted
	# ticket is visible as a gap rather than silently renumbering the ones after it.
	if numbers and numbers != list(range(1, len(numbers) + 1)):
		problems.append(f"{issues}: numbers {numbers} are not 01..{len(numbers):02d}")
	return problems


def main():
	if len(sys.argv) != 2:
		sys.exit(__doc__)
	scratch = Path(sys.argv[1])
	if not scratch.is_dir():
		print(f"no {scratch}, nothing to check")
		return
	problems = []
	for effort in sorted(scratch.iterdir()):
		if effort.is_dir():
			problems += check_effort(effort)
	if problems:
		print("\n".join(problems))
		sys.exit(f"{len(problems)} issue tracker problem"
				f"{'' if len(problems) == 1 else 's'}")
	print(f"issue tracker clean under {scratch}")


main()
