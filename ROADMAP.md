# NBT-Editor roadmap: client-side hardening and the 26.2 migration

Branch `fix/client-defects`, based on `origin/dev` at `226a31c`.

## Context

Minecraft moved to year-based versioning in 2026. `1.21.x` was followed by **26.1 "Tiny
Takeover"** (24 Mar 2026) and then **26.2 "Chaos Cubed"** (16 Jun 2026). There is no 1.22. This
mod's multi-version layer assumes a leading `1.`, so it cannot load on 26.x at all.

Separately, a three-model adversarial review of the 64 commits on `dev` surfaced a set of
client-side defects that are independent of game version. Those are cheap, provable, and land
first. The version migration is a much larger piece of work that is gated on Fabric shipping
26.2 support, so it is scoped here rather than started.

## Scope

**In scope.** The client-side mod. Defects reachable from the GUI, the chat formatting layer,
and the client chest. Game versions 1.21.11 and newer.

**Explicitly out of scope.** All server-side content. Every game version before 1.21.11. The
`nbteditor_1.17` module and the pre-1.21.11 arms of the multi-version layer are dead weight
under that floor, but nothing is deleted in this pass. Deletion is scoped in Phase 4 and
deferred.

**Not attempted.** Removing the dependency on `net.minecraft.*`. Investigated separately and
rejected. 324 of 382 source files import it, 998 of 1357 such imports are outside
`multiversion/`, and `loom:injected_interfaces` grafts mod interfaces onto 8 vanilla classes.
The `multiversion/` package is a version-drift adapter, not an isolation seam, and no evidence
in git history, the README, or code comments suggests decoupling was ever a goal. Fabric mods
call `net.minecraft.*` directly because it is the only API. There is no Bukkit-style stable
wrapper to migrate to, and Paper and Purpur are server platforms that cannot host a client GUI
mod.

## Constraints

- **No test source in the repo.** `.gitignore` excludes `src/test/`. Verification is
  compilation plus in-game exercise on the affected screen, not unit tests.
- **JDK comes from SDKMAN** (`25.0.4-tem`). Not on the default `PATH`.
- **Builds run in tmux** via `claude-term`, never in an unsupervised shell.
- **Fabric gates Phase 4.** Loader, Yarn mappings, and Fabric API must ship 26.2 support before
  any migration work can start. That is someone else's timeline.

---

## Phase 1: version-independent client defects

Each item is one commit. Verify the build after each before starting the next.

- [x] **1.1 `EventEditorWidget` OK button throws on invalid click-event input.**
  `screens/widgets/FormattedTextFieldWidget.java`. `updateOk()` validates both the click and
  hover sides, but only the hover dropdown and field are wired to call it, and it is never
  called from the constructor. Pick `Open URL`, type invalid text, leave the hover row alone,
  press OK, and line 142 calls `.get()` on an empty `Optional`. Fix mirrors the existing hover
  wiring.

- [x] **1.2 `StyleOption.valueOf` crashes the chat formatter.**
  `fancytext/FancyTextNode.java:49`. Any unrecognised `[foo]` token throws
  `IllegalArgumentException` out of a method declaring only `CommandSyntaxException`. The
  following `if (action != null)` guard shows the intent was a null-returning lookup, so the
  guard is currently dead code. Fix is a lookup that returns null.

- [x] **1.3 Custom RGB colours do not survive a round trip.**
  `fancytext/FancyText.java`. `stringify` emits `&#RRGGBB` with no terminator, but the
  tokenizer requires `&#RRGGBB;` and rejects the token without it. The adjacent shadow-colour
  path emits the `;` correctly, which is what makes this a one-character omission rather than a
  design question. On re-parse the colour is lost and the literal text `#RRGGBB` appears in the
  output.

- [x] **1.4 Unchecked throws in `FancyTextStyleOptionNode.modifyStyle`.**
  `[show_item]{99}` indexes the player inventory out of range and throws past the
  `NumberFormatException` catch. `[font]` with no value passes null to `IdentifierInst.of`.
  Both are reachable by typing in the formatted-text field.

- [x] **1.5 `SaveQueue` save thread is not a daemon.**
  `util/SaveQueue.java`. `ClientChest.stop()` calls `lock.write().stop()`, which parks any
  waiting thread in `Thread.sleep(Long.MAX_VALUE)` forever by design. A `SaveQueue` thread
  caught there is non-daemon, so the JVM never exits. This is the same symptom as the tip
  commit `226a31c`, which fixed it for the load path by setting `setDaemon(true)` and left the
  save path alone. Tradeoff: a daemon thread killed mid-write can leave a partial page. That is
  strictly better than a guaranteed hang, and the thread this affects is one that has already
  been deliberately frozen rather than one doing useful work.

**Verification.** `sh gradlew compileJava --rerun-tasks` under SDKMAN's `25.0.4-tem`, which
is not on the default `PATH`. Status: the whole tree compiles clean with all five fixes applied,
confirmed against the bytecode rather than the build log (`javap` shows `StyleOption.get(String)`
in `build/classes`). The build emits 5 pre-existing `Cannot find target method` mixin warnings
for the `mixin/toggled/*_1_21_1` classes, which target methods absent from the 1.21.5 artifact.
None of the six files changed here are mixins, so those warnings are untouched by this work.

Still outstanding: none of the five has been exercised in-game. That needs opening the
formatted-text editor, driving the event editor with invalid click-event input, round-tripping a
custom hex colour, and quitting the game with a client chest page mid-save.

## Phase 2: `SHOW_ENTITY` hover scan

- [ ] **2.1 Measure before changing anything.** `FancyTextStyleOptionNode.modifyStyle`
  streams every entity in the world to resolve a UUID, and `modifyStyle` runs on the parse path
  that fires per keystroke in the formatted-text field. The cost is proportional to loaded
  entities, so it is invisible on a test world and painful on a busy server. Capture a baseline
  before picking a fix.

## Phase 3: `ContainerIO` audit

Three models independently flagged this subsystem and each found a *different* defect. No single
finding is individually decisive, but the convergence is the signal, and the refactor is new
enough on `dev` that nothing is built on it yet.

Shared premise, verified first. `ContainerScreen` extends `ClientHandledScreen` with
`super(3, ...)`, so its inventory is always 27 slots, and `save()` passes all 27 to
`ContainerIOs.write` no matter what `getMaxSlots` reports. Every write implementation therefore
receives a longer array than it owns. `ContainerScreen.removed()` returns items sitting in slots
at or past `numSlots` to the player, which is the author's own acknowledgement that items do reach
those slots.

- [x] **3.1 Contents-space versus slot-space conflation** in `ConcatContainerIO`. Real, and
  unreachable. `getWrittenSlotIndex` adds the accumulated `getNumWritten` (contents space) to a
  result that is a slot index. Those two agree only when each io consumes exactly as many contents
  as it occupies slots. Every io satisfies that except the two compacting ones,
  `OrderNbtListContainerIO` and `BundleContentsComponentContainerIO`, and both are last in every
  concat that uses them. Checked all four in-scope concats: `DONKEY_IO`, `LLAMA_IO`, `VILLAGER_IO`,
  `ALLAY_IO`. Each leads with `EquipmentContainerIO`, a fixed 8 slots for 8 contents. No
  restructuring. Stating the invariant in 3.3 is what makes the conflation safe to leave.

- [x] **3.2 `SlotKeyNbtListContainerIO.write` iterates `contents.length`, not `numSlots`.** Fixed
  by bounding the loop. The unbounded version stamps `Slot` tags at or past `numSlots`, which its
  own `isSupported` then rejects, so the next open silently refuses and the container becomes
  uneditable. `OrderNbtListContainerIO.write` had the same shape, appending past `maxSlots` and
  failing its own `isSupported` size check, so it is bounded too and its `getNumWritten` now
  reports `min(contents.length, maxSlots)`. Both were masked by `ClientScreenHandlerSlot` locking,
  a GUI-level guard protecting a data-level function in another package.

- [x] **3.3 The tri-method invariant** across `getNumWritten`, `getWrittenSlotIndex`, and `write`
  is now documented on `ContainerIO` rather than encoded. `getNumWritten` equals `getMaxSlots`
  unless the io compacts, and a compacting io must be last in a concat. Encoding it would mean a
  separate slot-offset concept threaded through `ConcatContainerIO` to buy nothing today. The same
  pass corrected `read`'s javadoc, which promised "Will not contain null" while
  `EquipmentContainerIO` and `SlotKeyNbtListContainerIO` both leave nulls in unset slots and
  `ContainerScreen` line 57 defends against them.

A fourth reported defect, a slicing bug in `ConcatContainerIO.getWrittenSlotIndex`, was checked
and dismissed. `slot` is rebased by `- numWritten` and the result by `+ numWritten`, so the
slicing is consistent. That is a separate claim from 3.1 and does not rescue it.

**Verification.** `sh gradlew compileJava` under `25.0.4-tem`, BUILD SUCCESSFUL, the same 5
pre-existing mixin warnings and no new ones. Both bounds confirmed in the bytecode with `javap`
rather than read off the build log. Not exercised in-game.

## Phase 4: the 26.2 migration (scoped, not started)

Gated on Fabric. Do not start until loader, Yarn mappings, and Fabric API support 26.2.

Under the 1.21.11 floor this is predominantly a **deletion**, not a port. Sizing it:

- `multiversion/Version.java:152` rejects any version whose first component is not `1`. This is
  the hard blocker. It runs inside `NBTEditorMixinPlugin.getMixins()` during Mixin config prep
  with `"required": true`, so the mod cannot load at all on 26.2.
- Relaxing that guard is not sufficient, and the failure mode afterwards is *mixed*.
  Open-ended `range("1.21.x", null, ...)` branches keep matching correctly. Switches closed on
  both ends match nothing and throw `IllegalStateException("Missing version!")` at the point of
  use, far from `Version.java`. Some paths therefore fail loudly and others silently run
  1.21.5-shaped code against moved APIs.
- Surface area: 270 `newSwitch()` sites, 568 `range()` calls, 214 reflective lookups, and the
  whole `nbteditor_1.17` module.
- ~75 hardcoded intermediary identifiers (`method_10877`, `field_38096`, and similar) live
  outside `Reflection.java`, plus 8 `loom:injected_interfaces` entries keyed by raw `class_NNNN`
  names. All resolve against the mapping set for the exact built version.

Sequence when it starts: raise the floor and delete the pre-1.21.11 arms *first*, then port what
survives. Porting before subtracting means paying migration cost on code that is about to be
deleted.

## Deletion candidates (scoped, deferred)

Recorded so the next pass does not rediscover them. Nothing here is removed yet.

- The pre-1.21.11 arms of every `Version.newSwitch()` and `range()` site.
- The `nbteditor_1.17` module, and `MVShader1` / `MVShader2`.
- `ClientScreenHandlerSlot.unlockDuring` has no callers. It is the only bypass of slot locking,
  and it mishandles nesting: an inner call's `finally` unlocks the thread while the outer call is
  still running.
- `ContainerIOs.getNumWritten` has no callers. `getWrittenSlotIndex` has exactly one, from
  `ContainerScreen` line 151.
- `util/lock/PartitionedLockImpl` is live, reached through `PartitionedReadWriteLock` from
  `ClientChest`, so it is **not** a deletion candidate. It does carry real defects worth a
  separate audit: `lock(int)` blocks on the partition lock while still holding `globalLock`,
  which serialises every partition and can deadlock against `lockAll()`; `globallyLocked++` is a
  non-atomic read-modify-write on a `volatile int`; and `unlock(int)` throws
  `NullPointerException` when called without a matching `lock`.

## Merging `dev` into `local`

Verified clean, recorded here so it does not get re-litigated. `local` is 7 commits ahead and 64
behind `origin/dev`. Merge base is `1847636`. `local` is byte-identical to that base on
`gradle.properties` and `fabric.mod.json`, so only `dev` changed them and there is no conflict.
`git merge-tree --write-tree` produces tree `47041af` cleanly.

The merge does delete 57 files that exist on `local` but not `dev`. All are traceable to refactor
deletions upstream, not content loss, but it is a real deletion and is therefore deferred.
