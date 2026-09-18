# Item 12 — Fixes from the 2026-09-17 composite review

Working branch `fix/review-2026-09-17`, cut from `main` at `c8e08ee`. Findings and evidence
live in [`../REVIEW-2026-09-17.md`](../REVIEW-2026-09-17.md); this file tracks the work.

## What shapes the plan

Two of the three highest-impact defects are one-line bookkeeping errors that no check was
watching, and the third is inherited from upstream rather than introduced here. That ordering
is deliberate. The cheap release-correctness fixes land first and each ends in a check, so the
branch is shippable at every point rather than only at the end.

The `ConfigScreen` work is the one item that is a redesign rather than a repair. Its data-loss
path and its god-object shape are the same problem seen from two directions: 26 settings each
restated in five places, with no structure that makes a partial load unrepresentable. Fixing
the crash without fixing the shape would leave the next setting free to reintroduce it.

## Scope

In scope, in priority order: the merged priority list from the review, items 1 through 7.

Out of scope for this branch: the ADR-0003 completion sweep (roughly 35 widener entries, its
own migration), the `ConcatContainerIO` array-walk deduplication, and the Roadmap 2.1
per-keystroke entity scan. Each is real and each wants its own branch.

Not being done at all, with reasons recorded in the review: the `MAIN_THREAD` to
`isSameThread()` swap, pending the init-ordering check named in the review's Method and limits
section.

## Constraints

`docs/` and `scripts/` are gitignored (`.gitignore:14-18`, "Local to this fork, deliberately
not shared upstream"), so documentation files need `git add -f` to be tracked, as their
tracked siblings already are.

JUnit covers the pure-JVM slice only (ADR-0004). Anything touching registries,
`Minecraft.getInstance()`, or the run directory needs `scripts/dev-client.sh`. `JAVA_HOME` is
not exported in a plain shell; use `$HOME/.sdkman/candidates/java/current`, as the repo's own
scripts do. Do not run a Gradle build and a dev client at once, the host has 7 GB.

## Checklist

- [x] 1. `fabric.mod.json` version placeholder, so the jar stops declaring 2.0.4.999
- [x] 2. `HOPPER_MINECART_IO` entity type, via the single-source-of-truth registration form
- [x] 3. `ConfigScreen.loadSettings` partial-write path, closed by item 5 rather than patched
- [ ] 4. `DevScreenSweep` out of `src/main`
- [x] 5. `ConfigScreen` to a `Setting<T>` table
- [ ] 6. `Drawing` pass-throughs and the `renderItem` signature
- [ ] 7. `PartitionedLockImpl` to Guava `Striped`
- [ ] 8. `MVTextEvents` placement against ADR-0001
- [ ] 9. `MixinLink` members against its own docstring
- [ ] 10. Documentation reconciliation: ROADMAP Unresolved, 4.4, 4.5, ADR-0003 counts
- [ ] 11. Tests for `util/Futures` and `util/DataFixes`

## Verification per item

1. `./gradlew processResources`, then read `build/resources/main/fabric.mod.json`. The
   substituted value is the artifact, the source file is not.
2. `scripts/dev-client.sh --screens`, whose sweep now writes contents through the registered
   container io for a hopper minecart and reads the entity id back out of the item.
   `EntityType.getKey` is what reaches NBT, so only the runtime value settles it.
3. Folded into item 5, which is where the test lives.
4. `./gradlew build`, then confirm the class is absent from the jar.
5. `src/test/java/.../screens/SettingsTest.java`, nine cases over the load contract. Then
   `.scratch/review-2026-09-17/partial-config-check.sh`, which boots the dev client against a
   settings.json missing one key and reports what the client wrote back.
6. Compile plus a dev-client screen sweep, since the z-order question is a render behaviour.
7. The existing `PartitionedReadWriteLockTest` stress case is the check.

## Log

- **Item 1 done.** `src/main/resources/fabric.mod.json:4` was the literal `2.0.4.999` against a
  `processResources` `expand` that had nothing to substitute. Now `${version}`, fed by
  `build.gradle:6` `version = project.mod_version`. Verified: `build/resources/main/fabric.mod.json`
  read `2.0.4.999` before and `3.0.0` after, from the same task that builds the jar.
- **Item 2 done.** `CHEST_MINECART_IO` and `HOPPER_MINECART_IO` were the only two entries in
  `ContainerIOs` that baked an `EntityType` into the constant while the registration passed one
  separately. `ITEM_FRAME_IO` and `EQUIPMENT_IO` already used the `Function<EntityType<?>, ...>`
  form that takes the type from the registration, so both minecarts were converted to it rather
  than having the wrong constant swapped. The entity type now has one source of truth per
  registration and cannot drift again.
- **Item 2 verified.** The value that reaches NBT comes from `EntityType.getKey` at registration,
  so neither `compileJava` nor a screen sweep can settle it. `DevScreenSweep.checkEntityIds` now
  takes the registered `ContainerIO` for a hopper minecart, writes contents through it the way the
  container screen does, and reads the `id` back out of the item's `entity_data`. The chest minecart
  is checked the same way as a control, since the same commit converted it. The probe rides the
  existing `SWEEP` log contract, so `scripts/dev-client.sh` needed no new plumbing beyond a message
  that no longer says every failure is a screen.
- **The probe was made to fail before it was trusted.** `scripts/dev-client.sh --screens` passed in
  160s reading `id=minecraft:hopper_minecart`. Reinstating the old `EntityTypes.FURNACE_MINECART`
  registration made it print `SWEEP fail minecraft:hopper_minecart writes id=minecraft:furnace_minecart`
  and the harness exit 1 in 133s.
- **The earlier manual plan is dropped.** The previous note said this needed a hand-played `/open`,
  because the sweep hardcodes `Items.DIAMOND_SWORD` and opens factory screens. That was true of the
  sweep and false of the harness: the container path is reachable from a tick handler with no screen
  at all, which is where the probe went.
- **Items 3 and 5 done, as one change.** They were the same defect seen from two sides, and the
  test item 3 asked for needed the table item 5 asked for, so the cheap per-field `try` would have
  been written and then deleted. `screens/Settings.java` holds 25 rows, one per setting, each
  carrying its key, its default and how it is read and written. A row that finds nothing usable
  takes its own default and reports it; `Settings.load` returns whether the file was complete, and
  `ConfigScreen.loadSettings` writes back only then. One bad key can no longer reach another
  setting, so the data-loss path is gone by construction rather than caught.
- **Where the lines went.** `ConfigScreen` 625 to 503, plus 197 lines of table and 143 of test. The
  review's "about 300 lines" counted the widget block, which this change leaves alone beyond
  repointing each widget at its row. The saving is in kind, not in volume: five restatements per
  setting became one, and the seven legacy key mappings and the two negations now sit in the row
  that owns them instead of being paired up across two lists by eye.
- **It stayed in `screens/`.** A `config/` package would have imported `ConfigScreen.Alias` and the
  four config enums straight back, and moving those means touching every external
  `ConfigScreen.ItemSizeFormat`. Beside `ConfigScreen`, with no package cycle, is where it earns
  its place. It loads under JUnit without a game, which is what ADR-0004 needs of it.
- **The defect was reproduced first and the test was made to fail.**
  `.scratch/review-2026-09-17/partial-config-check.sh` boots the dev client against a config missing
  `jsonText` and carrying a hand-typed shortcut and alias. Before: `LOST shortcuts`, `LOST aliases`,
  `LOST checkUpdates`, `LOST creativeTabsPos`, all four overwritten in the user's own file. After:
  nothing lost, all 25 keys written back, `jsonText` alone at its default. Separately, restoring the
  old abort-and-save loop inside `Settings.load` fails three of the nine tests, so they are checks
  and not decoration.
- **The config screen is now opened by the sweep.** 25 settings each wire a widget to a row, and a
  row wired to the wrong widget compiles. The pairing was audited key by key, and
  `DevScreenSweep.checkConfigScreen` opens the screen so the widget construction runs on the build
  host. `scripts/dev-client.sh --screens` passes in 139s with all three checks and four factory
  screens green; `./gradlew test` is 99 tests, 0 failures.
