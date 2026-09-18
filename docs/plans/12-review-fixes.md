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
- [x] 4. `DevScreenSweep` out of `src/main`
- [x] 5. `ConfigScreen` to a `Setting<T>` table
- [x] 6. `Drawing` pass-throughs and the `renderItem` signature
- [x] 7. `PartitionedLockImpl` to Guava `Striped` -- declined, and the counter race under it fixed
- [x] 8. `MVTextEvents` placement against ADR-0001
- [x] 9. `MixinLink` members against its own docstring
- [ ] 10. Documentation reconciliation: ROADMAP Unresolved, 4.4, 4.5, ADR-0003 counts
- [ ] 11. Tests for `util/Futures` and `util/DataFixes`

## Verification per item

1. `./gradlew processResources`, then read `build/resources/main/fabric.mod.json`. The
   substituted value is the artifact, the source file is not.
2. `scripts/dev-client.sh --screens`, whose sweep now writes contents through the registered
   container io for a hopper minecart and reads the entity id back out of the item.
   `EntityType.getKey` is what reaches NBT, so only the runtime value settles it.
3. Folded into item 5, which is where the test lives.
4. `./gradlew clean build`, then confirm the class is absent from the jar, and
   `scripts/dev-client.sh --screens` to confirm it still installs from the dev source set.
5. `src/test/java/.../screens/SettingsTest.java`, nine cases over the load contract. Then
   `.scratch/review-2026-09-17/partial-config-check.sh`, which boots the dev client against a
   settings.json missing one key and reports what the client wrote back.
6. The z-order question is settled from history rather than by rendering, see the log. Then
   compile plus a dev-client screen sweep, and a hunk-by-hunk read of the diff, since an
   identity transform is what makes the change safe and no test here looks at pixels.
7. A standalone probe, `.scratch/review-2026-09-17/counter-race-probe.java`, which runs the
   `lockAll`/`unlockAll` body under both counter types and reports the drift each leaves behind.
   The unit suite cannot pin this one; see the log.
8. A rename and a package move, so the compiler is most of the check: `./gradlew check`
   resolves every call site, and `checkMixinTargets` and `validateReflectiveNames` rule out the
   two kinds of reference that survive a move silently. Then `scripts/dev-client.sh --screens`,
   because `EventEditorWidget` and `BookScreen` are the heaviest callers and nothing opens them
   at compile time.
9. `src/test/java/.../util/TextEventsTest.java`, three cases, one of which is the handler
   lifetime the old map got wrong. Then `./gradlew check`, which is what proves the two moved
   members still resolve from the mixins, and `scripts/dev-client.sh --screens` as the smoke
   test that the client still boots and every screen still opens.

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
- **Item 4 done.** `misc/DevScreenSweep` is now `src/dev/java/.../dev/DevScreenSweep`, in a `dev`
  source set on the client run's classpath and compiled by `check`. The jar carries no class under
  `dev/` at all, which was the review's blocker: a player's jar should not hold a 200-line screen
  robot driven by system properties.
- **Why a source set and not a `jar { exclude }`.** Excluding the file from the archive would have
  been one line, and it would have left `src/main` able to call a class the jar does not have. The
  source set makes that a compile error. `src/main` gets in by `Class.forName` from
  `NBTEditorClient.installDevScreenSweep`, which catches `ClassNotFoundException` and does nothing,
  because in a production launch there is nothing to do.
- **The seam got a check rather than a warning.** A `Class.forName` string is exactly the kind of
  reference a rename breaks silently, so `validateReflectiveNames` resolves every `Class.forName`
  on one of the mod's own classes against both source trees and fails `check` on one that is not
  there. Proved by renaming the string: the build fails naming the file and the class. It is
  general, not a single hardcoded pair, so the next reflective reference is covered too.
- **ADR-0005 now describes four layers.** The `--screens` sweep arrived in 25dd620 against an ADR
  that named three, which the review recorded as scope creep. The ADR names the layer, what it
  catches, and why the sweep lives outside `src/main`. That closes review finding 7; the rest of
  the documentation reconciliation is still item 10.
- **Verified.** `./gradlew clean build` green, 99 tests, `validateReflectiveNames` and both mixin
  validations in `check`. The jar holds nothing under `dev/` and declares 3.0.0.
  `scripts/dev-client.sh --screens` passes in 124s with all three checks and four factory screens
  green, so the sweep still installs from its new home.
- **Item 6 done, and the review's caveat does not hold.** The review warned that `renderItem`'s
  `200.0F` and `100.0F` might be z-layering the render-state rewrite stopped honouring, and said to
  check before deleting. `git show v2.0.3:.../MVDrawableHelper.java` settles it: the parameters were
  read only on the `null..1.19.3` branch of a `Version.newSwitch`, and the `1.20.0..` branch above
  it already drew straight to the draw context and ignored both. They died with 1.19.3 support,
  several releases before this fork existed, so there is nothing to regress and nothing to restore.
  The method keeps its name and loses the two parameters; its javadoc now records why.
- **Six pure delegations inlined, five kept.** Audit A1's rule is that a method whose body is one
  vanilla call under another name gives a reader nothing per unit of interface. `fill`, `drawText`,
  `drawTextWithoutShadow`, `drawTextWithShadow`, `drawCenteredTextWithShadow` and `disableScissor`
  were exactly that, 48 call sites, now calling the `GuiGraphicsExtractor` method directly.
  `drawTexture`, `renderTooltip`, `renderItem`, `drawSlotHighlight` and `renderLogo` supply an
  argument a caller should not have to know, the render pipeline, the font, a slot's 16 by 16 box,
  or compose two calls, so they stay. `Drawing` went 120 lines to 101.
- **`enableScissor` went too, for a reason the ponytail pass did not raise.** It converted width and
  height to x2 and y2, so it was not a pure delegation, but `disableScissor` was, and once that went
  both call sites read `Drawing.enableScissor(...)` against `context.disableScissor()`. A pair split
  across two receivers is worse than either wrapper, so both sides now sit on the context.
- **The rewrite was a script, not an afternoon of hand edits.**
  `.scratch/review-2026-09-17/inline-drawing.py` matches parentheses rather than guessing at commas,
  refuses any call whose first argument is not a plain context, and reports what it skipped. It
  skipped nothing. It missed two calls inside `Drawing` itself, which were unqualified and so had no
  `Drawing.` prefix to match; the compiler caught both.
- **Verified.** Every hunk read as an identity transform, which is the real check here since nothing
  in the build looks at pixels. `./gradlew compileJava compileDevJava` green.
  `scripts/dev-client.sh --screens` passes in 243s with all three checks and four factory screens.
  The screens the sweep opens exercise the inlined text, fill and scissor calls; it would catch a
  throw, not a misplaced pixel, and the diff is what rules that out.

- **Item 7 declined, and a defect under it fixed.** The review reads
  `util/lock/PartitionedLockImpl.java` as hand-rolled striping and asks for Guava `Striped.lock(64)`.
  It is not striping. It is a per-key registry that gives every partition its own lock, so it has no
  hash collisions at all, and `Striped` is fixed-stripe by construction with no configuration that
  changes that. `NBTEditorClient.java:76` builds `new SmallClientChestPageCache(100)`, so 100 pages
  against 64 stripes collide by pigeonhole and pages that are unrelated today would start
  serialising against each other. The locks here are also fair on purpose, `new ReentrantLock(true)`
  on both the global lock and every partition lock, where `Striped.lock` vends unfair ones. And the
  global lock the review would delete is load-bearing: it is what stops a new partition appearing
  between `lockAll` and `unlockAll`, which the comment at `:62-64` already records. The trade is
  roughly 13 lines against two concurrency properties, so the answer is no.
- **The counter race the review missed.** `globallyLocked` was a `volatile int` raised with `++` at
  `lockAll` and `stop`, lowered with `--` at `unlockAll`, all three outside the mutex, since the
  point of raising it early is that a *pending* `lockAll` already reads as locked. Two callers can
  therefore collide inside one read-modify-write and lose an update. The drift is permanent and
  negative, so afterwards a `lockAll` that genuinely holds every partition reports
  `isAllLocked() == false`, and `isLocked(anything)` with it. `.scratch/review-2026-09-17/counter-race-probe.java`
  reproduces it: over five runs of 8 threads by 20000 iterations the `volatile` counter finished at
  -3, 0, -1, 0, 0, and the `AtomicInteger` finished at 0 every time. The field is now an
  `AtomicInteger`.
- **Why there is no unit test for it.** The race needs two threads inside one `++`, which measured
  at roughly one occurrence per 100k iterations, so a test that waited for it would pass on the
  broken code most of the time. A test that reports green on the defect it names is worse than
  none. `concurrentGlobalLocksDoNotDeadlockOrLeakState` was added for the coverage that *is*
  deterministic, concurrent `lockAll`/`unlockAll` neither deadlocking nor leaving state behind, and
  its docstring says plainly that the counter race is not what it pins. The field's type is what
  rules that out. Suite is 100 tests, up from 99.
- **Item 8 done.** `multiversion/MVTextEvents` is now `util/TextEvents`. Nothing in it spans game
  versions: the two nested descriptor classes name vanilla `ClickEvent` and `HoverEvent` subtypes
  directly and the only version-flavoured thing about the file was the package it sat in. ADR-0001
  already names that case and sends it to `util/`, which is where `MVDrawableHelper` went as
  `util/Drawing` and where `MVMisc`'s halves went. It lands beside `util/TextUtil`, which already
  owns the rest of the text serialization and was importing it across the package boundary; that
  import is now gone.
- **The `MV` prefix went with the package.** Keeping the name would have left a class called `MV*`
  outside `multiversion/`, which reads as a wrapper that is not one. ADR-0001's count moves 21 to
  20 and `CLAUDE.md`'s placement paragraph now names this move alongside the other two, so the
  rule and its worked examples stay together.
- **A script did the move, not an editor.** `.scratch/review-2026-09-17/move-textevents.sh` git-mvs
  the file, rewrites its package and class name, swaps the now-cross-package
  `DynamicRegistryManagerHolder` import in for the now-same-package `TextUtil` one, and then walks
  every referencing file: a file in `util/` loses the import outright, any other has it repointed,
  and a reference with no import at all aborts the run rather than being guessed at. It ends by
  failing if the old name survives anywhere. Thirteen files were repointed. One unused
  `ItemStack` import that predated the move went with it.
- **Verified.** `./gradlew check` green, 100 tests, with `validateMixinConfigs`,
  `checkMixinTargets` and `validateReflectiveNames`. `scripts/dev-client.sh --screens` passes in
  117s: both minecart entity-id checks, the config screen, and all four factory screens. The diff
  is an identity transform outside the moved file's header, which is what the hunk-by-hunk read is
  for, and `git` recorded it as a rename rather than an add and a delete.
- **Item 9 done.** The docstring `e5965f1` added says what belongs in `MixinLink` is thread-keyed
  state coupling one mixin to another, and that mod behaviour merely invoked from a mixin belongs
  in the package that owns the concept. Two members contradicted it and both moved out.
  `renderChatLimitWarning` was screen drawing, and is now `screens/ChatLimitWarning.render`,
  beside `screens.ItemTooltips`, which the docstring already names as the precedent. The
  `withRunClickEvent` / `tryRunClickEvent` pair and the map behind them are the mod's own clickable
  text, and are now on `util/TextEvents`, which owns click events after item 8 and is where
  `ScreenMixin` already looked to decide the click was an `OPEN_FILE` in the first place.
- **The docstring was also wrong by omission.** It listed `specialNumbers`,
  `hiddenExceptionHandlers` and `SET_CHANGES` as "all that shape" while `ITEM_BEING_RENDERED`, a
  `Map<Thread, ItemStack>`, sat ten lines below it. It is named now. The file is 155 lines to 114.
- **The unbounded map was a real leak, not a style complaint.** `events` was a plain `HashMap` that
  nothing ever removed from, so every handler lived until the game closed, and with it everything
  the handler captured. `BookScreen.makePreviewStyle` captures `this`, so each book preview pinned
  a whole editor screen for the session. It is now a `WeakHashMap` behind
  `Collections.synchronizedMap`. The id string inside the `ClickEvent` is the only strong reference
  to the key, so a handler lives exactly as long as the text that can still invoke it and not one
  frame longer.
- **The lookup still works because the id instance is the one the event holds.**
  `ClickEvent.OpenFile::path` hands back the same `String` the event was built with and
  `String.toString()` returns `this`, so the click arrives holding the key itself. An equal-valued
  copy resolves too, for as long as the original is alive, which is every case where the text is
  still on screen to be clicked.
- **The test was made to fail before it was trusted.** `TextEventsTest` attaches a handler inside a
  helper, drops the style on return, and asserts the handler is released. Putting the old strong
  `HashMap` back makes `aHandlerIsReleasedOnceItsTextIsGone` fail and leaves the other two green,
  so it pins the leak and not the plumbing. Suite is 103 tests, up from 100.
- **What the sweep does and does not settle here.** `scripts/dev-client.sh --screens` passes in
  170s with all three checks and four factory screens, which is the smoke test that the client
  boots and the mixins still bind. It does not open a chat screen or a book preview: the sweep
  picks its rows from the item's type and `NBTE_SCREENS_NBT` only patches components, so reaching
  `BookScreen` would need a new harness knob. The call-site changes there are `MixinLink.` to
  `TextEvents.` with nothing else moved, which the compiler settles and the diff shows hunk by
  hunk.
