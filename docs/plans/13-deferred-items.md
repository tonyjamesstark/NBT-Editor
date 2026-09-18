# Item 13 — The items the review-fixes branch deferred

Working branch `fix/deferred-2026-09-18`, cut from `fix/review-2026-09-17` at `5ec1416` rather
than from `main`, because item 10 on that branch is the base this one edits further. Findings
live in [`../REVIEW-2026-09-17.md`](../REVIEW-2026-09-17.md); the previous branch's plan is
[`12-review-fixes.md`](12-review-fixes.md).

## Scope

The three items [`12-review-fixes.md`](12-review-fixes.md) put out of scope, the one it refused,
and the housekeeping the review asked for and that plan never took in:

1. ADR-0003 completion, plus the widener lines with no consumer.
2. `ConcatContainerIO` array-walk deduplication.
3. Roadmap 2.1, the per-keystroke world-entity scan.
4. `MixinLink.MAIN_THREAD`: run the init-ordering check the refusal was pending on, then decide.
5. `.scratch/<slug>/issues/` per `docs/agents/issue-tracker.md`.

Each of 1 through 3 was recorded as wanting its own branch. They share one branch here with one
commit per item, because none of them touches another's files and the branch they build on is
itself unpushed; stacking three branches on an unpublished base buys nothing a reviewer can use.

## What shapes the plan

Item 1 was described as a sweep of roughly 35 widener entries into `util/AccessWidenedApi`. That
description does not survive measurement, so the item is being done from its purpose rather than
its wording. See the Log.

Item 3 is the only one that starts with a measurement rather than a change, because the roadmap
says so: the cost is proportional to loaded entities, so it is invisible on a test world.

Item 4 is a check, not a change. The outcome may well be that the refusal stands.

## Constraints

Unchanged from `12-review-fixes.md`. `docs/`, `scripts/` and `tools/` are gitignored, so tracked
files there need `git add -f`. JUnit covers the pure-JVM slice only (ADR-0004). `JAVA_HOME` is
not exported in a plain shell; use `$HOME/.sdkman/candidates/java/current`. Do not run a Gradle
build and a dev client at once.

## Checklist

- [ ] 1. ADR-0003 completion and the dead widener lines
- [ ] 2. `ConcatContainerIO` array-walk deduplication
- [ ] 3. Roadmap 2.1 per-keystroke entity scan, measured then fixed
- [ ] 4. The `MAIN_THREAD` init-ordering check
- [ ] 5. `.scratch/<slug>/issues/` housekeeping

## Verification per item

1. `tools/check-widener-consumers.py`, which reads the constant pools of the mod's own compiled
   classes and reports every widener line with no consumer and every widened member touched
   outside `AccessWidenedApi`. Wired into `check`, so it is the durable answer and not a one-off.
   Then `./gradlew check`, where Loom's own `validateAccessWidener` proves the surviving lines
   still name members that exist, and `scripts/dev-client.sh --screens`.
2. Behaviour-preserving, so the check is the existing suite plus new cases over the walk, and a
   dev-client run because no unit test opens a container.
3. A baseline first, then the same measurement after. Without a before number there is nothing
   to claim.
4. Instrumentation in a dev client that records the two orderings against each other. A source
   read cannot settle it; that is why the refusal was left pending.
5. The files exist and say what `docs/agents/issue-tracker.md` requires of them.

## Log
