# NBT-Editor roadmap: client-side hardening and the 26.2 migration

Branch `fix/client-defects`, based on `origin/dev` at `226a31c`.

## Context

Minecraft moved to year-based versioning in 2026. `1.21.x` was followed by **26.1 "Tiny
Takeover"** (24 Mar 2026) and then **26.2 "Chaos Cubed"** (16 Jun 2026). There is no 1.22. This
mod's multi-version layer assumes a leading `1.`, so it cannot load on 26.x at all.

Separately, a three-model adversarial review of the 64 commits on `dev` surfaced a set of
client-side defects that are independent of game version. Those are cheap, provable, and land
first. The version migration is a much larger piece of work, re-scoped in Phase 4 after
establishing that 26.2 ships deobfuscated in Mojang names, so it is planned there rather than
started.

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
- **Phase 4 is not gated externally.** Fabric shipped 26.2 and is on 26.3-rc. The blocker is
  internal: this codebase is written in Yarn names and Yarn stops at 1.21.11.

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

## Phase 4: the 26.2 migration (re-scoped 2026-09-13, not started)

The earlier version of this section said Phase 4 was gated on Fabric shipping 26.2 support, and
that the work was predominantly a deletion. Both were wrong. Fabric shipped 26.2 and is already
on 26.3-rc. The dominant cost is a **mappings rename**.

### What changed in Minecraft

26.2 ships **deobfuscated**. Its client jar carries 10,372 readable `net/minecraft/...` classes,
and Mojang publishes no mappings for it: the 26.2 version manifest lists only `client` and
`server` downloads, where 1.21.5 and 1.21.11 also list `client_mappings` and `server_mappings`.
Fabric API's `26.2` branch declares no `mappings` line at all, where its `1.21.11` branch still
needs `mappings loom.officialMojangMappings()`. Absence of mappings means they are unnecessary,
not missing.

The names shipped are **Mojang names**, not Yarn. `net.minecraft.item.ItemStack` becomes
`net.minecraft.world.item.ItemStack`, and `net.minecraft.nbt.NbtCompound` becomes
`net.minecraft.nbt.CompoundTag`. Yarn never crossed the boundary: the newest Yarn on Fabric's
maven is `1.21.11+build.6` and there is nothing for any 26.x.

Intermediary is retired in practice too. `net.fabricmc:intermediary` resolves to a real `1.21.11`
artifact but to a placeholder `0.0.0` for 26.2, because intermediary is the identity mapping once
the game is already deobfuscated. That kills the mod's intermediary layer: the 8
`loom:injected_interfaces` entries keyed to `class_4068`, `class_437` and similar, plus the ~75
hardcoded `method_NNNNN`/`field_NNNNN` lookups in and around `Reflection.java`. None have a
target on 26.2.

### Toolchain coordinates

| | 1.21.5 (now) | 1.21.11 | 26.2 |
| --- | --- | --- | --- |
| yarn | 1.21.5+build.1 | 1.21.11+build.6 | none |
| mojmap | published | published | not needed |
| intermediary | 1.21.5 | 1.21.11 | 0.0.0 placeholder |
| fabric-api | 0.128.1+1.21.5 | 0.141.6+1.21.11 | 0.160.0+26.2 |
| loom | 1.10-SNAPSHOT | 1.13.3 | 1.16.2 |
| java | 21 | 21 | 25 |

### Why 1.21.11 is the pivot

1.21.11 is the only version carrying **both** mapping sets, Yarn `1.21.11+build.6` and Mojang
`client_mappings`. It is therefore the one place the rename can be verified in isolation, on a
fixed game version, with no API drift mixed in. Going straight from 1.21.5/Yarn to 26.2/Mojang
blends rename errors, API-change errors, and a six-minor-version loom upgrade into one failure
surface with no way to attribute any single break.

### What the first build taught us (2026-09-13)

Four corrections, recorded before the plan below is read again.

**Loom 1.16 split into two plugins.** `net.fabricmc.fabric-loom-remap` builds against a mapped
game; `net.fabricmc.fabric-loom` builds against a deobfuscated one. At 26.2 there is no remapping
stage at all: no `mappings` line, `implementation` instead of `modImplementation`, and no
`remapJar`. That removes the ground under `mergeRefmapJson`, `mergeLibs` and `mergeDevLibs` in
`build.gradle`, and `loom:injected_interfaces` needs re-checking against the new plugin.

**The target toolchain is already in place.** Gradle 9.5.1 and Loom 1.16.2 landed during 4.1
rather than 4.5, because ModMenu 17.0.0 is itself built with Loom 1.14.6 and refuses an older
one. Only `minecraft_version`, `yarn_mappings` and `fabric_version` are left for 4.5.

**Both third-party mods publish for 26.2.** ModMenu 20.0.2 and nbt-autocomplete
1.3.15-fabric-26.2. This was listed as unresolved; it is resolved.

**4.1's premise was wrong, and 4.1 cannot finish without 4.2.** Names do move between 1.21.5 and
1.21.11. Two vanilla rewrites land in that window. The input callbacks on `Element` became
records, which is done. The rendering stack was rebuilt, which is not: `DrawContext.getMatrices`
returns a `Matrix3x2fStack`, `RenderSystem.setShaderColor`, `enableScissor` and `disableScissor`
are gone, `RenderLayer` lost `MultiPhaseParameters` and `RenderPhase` entirely. The ~95 remaining
errors sit overwhelmingly in code that 4.2 deletes, so fixing them before 4.2 is wasted work.

Renames confirmed against the 1.21.11 jar: `EntityRenderDispatcher` to `EntityRenderManager`,
`BlockPredicatesChecker` to `BlockPredicatesComponent`, `Text.Serialization` to `TextCodecs`,
`Style.font` from `Identifier` to `StyleSpriteSource`, and the static `Screen.hasShiftDown`
family replaced by `hasShift`/`hasCtrl`/`hasAlt` on the input record.

### Sequence

Each step ends in a build. Do not start the next until the previous compiles.

- [x] **4.1a Toolchain to 1.21.11.** Minecraft 1.21.11, Yarn 1.21.11+build.6, fabric-api 0.141.6,
  loader 0.17.3, Gradle 9.5.1, Loom 1.16.2 on `fabric-loom-remap`. `data_versions.json` gains
  1.21.6 through 1.21.11, and 1.21.5 is corrected from 4324 to 4325.
- [x] **4.1b Input callbacks to the event records.** `Click`, `KeyInput` and `CharInput` across
  roughly 200 sites, done by codemod. Signatures take the record; a destructuring prologue keeps
  each body's existing names, so no body was rewritten.
- [ ] **4.1c The rendering stack.** Blocked behind 4.2 by choice, not by necessity.
- [ ] **4.2 Raise the floor and subtract.** Delete the pre-1.21.11 arms across all 270
  `newSwitch()` sites and 568 `range()` calls, the `nbteditor_1.17` module, and
  `MVShader1`/`MVShader2`. Do this *before* the rename so 4.3 does not pay migration cost on code
  about to be deleted. Largest diff of the phase, and it shrinks every later step.
- [ ] **4.3 Yarn to Mojang names at 1.21.11.** Swap `mappings` to `loom.officialMojangMappings()`
  and rename whatever survives 4.2. Same game version throughout, so every break is a rename
  break. This is the bulk of the ~1357 `net.minecraft.*` imports, 998 of them outside
  `multiversion/`.
- [ ] **4.4 Retire the intermediary layer.** Replace the 8 `loom:injected_interfaces` keys and
  the ~75 `method_NNNNN`/`field_NNNNN` lookups with real names. Kept separate from 4.3 because it
  is a different kind of edit: 4.3 is mechanical renaming, this is deciding whether each
  reflective lookup still needs to be reflective once the name is stable and readable.
- [ ] **4.5 Bump 1.21.11 to 26.2.** Loom 1.10-SNAPSHOT to 1.16.x, fabric-api 0.160.0+26.2, loader
  0.19.5, and *remove* the `mappings` line rather than repointing it. Relax
  `Version.parseVersion`, which rejects any version whose first component is not `1` and runs
  inside `NBTEditorMixinPlugin.getMixins()` with `"required": true`. Add 26.x entries to
  `data_versions.json`. Raise `fabric.mod.json`'s `"java": ">=16"` to `>=25`.

### The version-guard failure mode, unchanged

Relaxing `Version.parseVersion` alone is not sufficient, and the failure afterwards is *mixed*.
Open-ended `range("1.21.x", null, ...)` branches keep matching. Switches closed on both ends match
nothing and throw `IllegalStateException("Missing version!")` at the point of use, far from
`Version.java`. Some paths fail loudly, others silently run 1.21.5-shaped code against moved
APIs. Step 4.2 removes most of that surface before it can bite.

### Unresolved

- Whether `multiversion/` earns its place at all once the floor is 1.21.11 and the game ships
  deobfuscated. Its reason for existing was spanning obfuscated versions with drifting names.
- Whether ModMenu and nbt-autocomplete publish 26.2 builds. `nbt-autocomplete` is pinned to
  `1.3.12-fabric-1.21.5` and is the harder of the two.
- `depends.minecraft` in `fabric.mod.json` is `">=1.17-"` with no upper bound, so the current jar
  is accepted and then hard-crashes on 26.2 rather than being refused. Worth fixing independently
  of this phase.

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
