# Upstream sync: cherry-pick candidates from `us/dev` and `fork-thorioum/dev`

Status: triaged 2026-09-18. All 64 items carry a verdict, so nothing here is awaiting
evaluation. The two calls that were parked rather than settled are in
[`issues/`](issues/). The five roles in `docs/agents/triage-labels.md` describe an issue's
triage state and none of them says "done", which is why this line is prose.
Created: 2026-09-17
Owner: unassigned

Two sources, classified the same way. `us` is mega12345mega/NBT-Editor, the upstream we forked.
`fork-thorioum` is Thorioum/NBT-Editor26.1.2, a sibling fork of the same upstream.

## Why cherry-pick rather than rebase

Shared base with `us/dev` is `226a31c` (2025-09-13). We are 112 commits ahead and 50 behind.
Shared base with `fork-thorioum/dev` is `c9d47cc` (2025-02-02), older still, 173 ahead and 21
behind.

`us/dev` still targets `minecraft_version=1.21.5` and carries 122 files under `multiversion/`,
and its recent work widens support back to 1.17. We target 26.2 with 66 multiversion files and
falling, having deleted the version-switch machinery, `MVMisc`, `TextInst`, `IdentifierInst` and
`MainUtil`. Of the 478 files we touched and the 227 they touched, 167 overlap; in those we
rewrote +7373/-11900 lines against their +3148/-3720. 29 files we deleted are files they
modified, so any rebase produces delete/modify conflicts with no mechanical resolution.

`fork-thorioum/dev` diverges further. It targets 26.1.1, keeps the whole multiversion layer, and
four of its 21 commits are bulk hand-ports that duplicate our 4.1 through 4.5 phases.

Rebase is not feasible against either remote, and not indicated. This list is the alternative.

## How to evaluate an item

Nothing here is approved. For each unchecked item, in order:

1. Read `git show <sha>` in full. The subject often bundles several unrelated fixes.
2. Decide whether the defect exists at 26.2. A subject saying "in 1.17-1.21.4" usually means the
   bug is in a branch we deleted, but not always; some are version-agnostic fixes that upstream
   merely happened to notice on an old version.
3. Check whether we already fixed it independently during the migration. `a5abf3a` below is an
   example of one we had already corrected.
4. Only then attempt the pick.

Record the verdict by ticking the box and appending a one-line result. Leave a rejected item
unticked with a `rejected:` note so the next sync does not re-evaluate it.

## Mechanics

- Pick onto `local`, oldest first within a group, so later picks see earlier context.
- `git cherry-pick -x -Xignore-space-change -Xfind-renames <sha>`. The `ignore-space-change` is
  load-bearing. Upstream's `0f64b7e` reindented 45 files, so without it almost every later pick
  conflicts on whitespace alone.
- One commit per pick. Do not batch; a conflicted pick needs its own resolution recorded.
- Fork commits bundle unrelated changes under a version-bump subject, so pick hunks, not commits.
  Drop every `gradle.properties` and `fabric.mod.json` version hunk on the way through.
- `./gradlew build` after each group. `scripts/dev-client.sh --screens` after any group that
  touches a screen, a container io or the registry path.

## Upstream group A: take, applies at 26.2

- [x] `35e0ada` Fixed EquipmentContainerIO referring to boots rather than feet
      `containers/EquipmentContainerIO.java` (+1/-1). **Confirmed live bug.** 26.2's
      `EquipmentSlot` serialises `head, chest, legs, feet, saddle, body, mainhand, offhand`;
      our `KEYS` array still says `boots`, so the boots slot addresses a key vanilla never
      writes. Our file was restructured by `b681410`, so apply the one-word change by hand.
      Do this one first regardless of what happens to the rest of the list.
      Result: done in 5842bd6. Confirmed against 26.2 EquipmentSlot; one-word change.
- [x] `0f8ff44` Fixed entity icons rotating sporadically in 1.21+
      `localnbt/LocalEntity.java` (+7/-0). Version range includes 26.2.
      Result: done in 335353d. Entity.load leaves yHeadRotO/yBodyRotO at zero and the preview is never ticked.
- [x] `e9f26ca` Fixed edited signs not updating their text until reloading
      `server/NBTEditorServer.java` (+2/-0). No version qualifier.
      Result: done in 594dfdf. blockEntityChanged + blockChanged now run regardless of triggerBlockUpdates.
- [x] `fd4b9c6` Fixed SignboardScreen not updating sign when entering less than 4 lines in 1.20.0+
      `screens/factories/SignboardScreen.java` (+10/-2). Range includes 26.2.
      Result: done in 63e1b02. SignText.LINES_CODEC is Util.fixedSize(4); lines are padded to four.
- [x] `3277f27` Fixed fancy text [show_item] not handling negative slot indices and air properly
      `fancytext/FancyTextStyleOptionNode.java` (+7/-1). We rewrote throws in this file
      (`7cea6c1`), so expect a small conflict.
      Result: done in 56d257c. Empty item now yields the unchanged style; at 26.2 ItemStackTemplate throws on air, so this was a crash rather than a bad tooltip. Negative index half already covered by our IndexOutOfBoundsException catch.
- [x] `d0755cb` Fixed converting text with a custom color into fancytext missing ';'
      `fancytext/FancyText.java` (+1/-1). Pairs with our `4b6dd29` round-trip fix.
      Result: skipped, already present. FancyText.java:155 already appends the ";".
- [x] `be450e4` Fixed closing quote getting removed from escaped keys passed to NBT Autocomplete
      `integrations/NBTAutocompleteIntegration.java` (+6/-1).
      Result: done in 1d63762. Closing quote kept when a value follows.
- [x] `c131c19` Made jukebox and brewing stand block states get updated by /open
      `containers/ContainerIOs.java` (+4/-2).
      Result: done in 2c25766. 26.2 still has has_record and has_bottle_0..2; jukebox BE still reads RecordItem.
- [x] `4d10512` body_armor_item in /open, SlotKeyNbtListContainerIO returning null in empty slots
      `containers/ContainerIOs.java`, `EquipmentContainerIO.java`, `SlotKeyNbtListContainerIO.java`
      (+18/-5). The null-in-empty-slots half is version-agnostic and is the reason to take this.
      Result: partly done in 06ec48d (SlotKeyNbtListContainerIO now fills EMPTY; ContainerItemReference.getItem could return null). body_armor_item half skipped, 1.20.5-1.21.4 only. KEYS/texture reorder (hands before saddle/body) skipped as cosmetic; filed as `issues/01-equipment-slot-order.md`.
- [x] `57d2dbe` Switched normal and inverted for page keybinds config
      12 files, +12/-19. Tracks FabricMC/fabric-api#4416, which our 26.2 build is downstream of.
      Result: done in f2d6f98. All three screens, ConfigScreen negation shim, en_us desc, other lang descs dropped.
- [x] `2a136a4` Removed extra @SuppressWarnings("serial")
      `clientchest/ClientChest.java`, `screens/NBTEditorScreen.java` (+1/-3). Trivial, but we
      compile on JDK 25 where the warning behaviour is the same.
      Result: done in b7a6706. Proven with -Xlint:serial and a sentinel on MixinLink.HiddenException.
- [x] `36e98c7` papa_louie09 updated pl_pl
      `lang/pl_pl.json` (+49/-52).
      Result: done in f26298d. Brought back a Polish page_keybinds.desc describing the old meaning; dropped again in eb5e08f. Carries two orphan keys (config.no_mouse_jump_time*) for an upstream option we do not have; harmless, left in place to keep future picks clean.
- [x] `2168510` DZultra added de_de (German) (#174)
      8 lang files (+16/-8). New locale, no code.
      Result: done in 10b184a. de_de carries orphan keys for enchant_glint_fix and no_mouse_jump_time, same reasoning.
- [x] `09b5ad5` Tweaked /get help textformat
      `lang/en_us.json` (+10/-7).
      Result: done in af3b3b8. text_format_1..15 contiguous; GetHelpCommand reads them through getLongTranslatableText.
## Upstream group B: evaluate, plausibly applies but will not pick cleanly

- [x] `5e65608` Cleaned up default registry stuff, fixing memory issues and splash screen issues
      44 kept files, 9 deleted, +57/-353. **Read this first in the group.** It rewrites
      `multiversion/DynamicRegistryManagerHolder.java`, `MVClientNetworking.java`,
      `ComponentItemNBTManager.java` and `screens/factories/EnchantmentsScreen.java`, which is
      exactly the path that produced the dead factory menus fixed in `25dd620`. Whatever upstream
      concluded about the default registry set is directly relevant to us. Will not cherry-pick;
      port by hand after reading.
      Result: skipped, not present. RegistryEntryReferenceMixin and Registry1Mixin already do the holder conversion, both registered in NBTEditorMixinPlugin, all targets resolve at 26.2.
- [x] `377e0c2` SNBT formatting for 1.21.5 features, special number parsing, NBTEditorScreen caching
      13 kept files, 7 deleted, +230/-473. The 1.21.5 SNBT features are still present at 26.2.
      Bundles at least five separable fixes; split it rather than taking it whole.
      Result: done in `e7792e4`. Proved both halves from the old file: INT_PATTERN `[-+]?(?:0|[1-9][0-9]*)` admits no 0x/0b/_ so those fell through to STRING_COLOR, and parseCompound's readComma -> expect('}') threw on the `(` of a builtin call. Rewrote NbtFormatter over 26.2's own SnbtGrammar instead of taking upstream's 20-file SNBTFormatter/Version-switch abstraction, which only earns its keep across versions. New NbtFormatterTest pins the eight forms. Left out: the NBTEditorScreen caching half, which is a performance change, not a defect.
- [x] `23898d2` Mouse jumping to center of screen, plus a config option to prevent it
      8 kept files (+83/-7). Real UX defect, applies at 26.2. Touches `misc/MixinLink.java`,
      which `eb858bb` and `8bf9a4c` gutted, so the mixin half needs rehoming.
      Result: done in `08ccc45`. Proved by javap: MouseHandler.releaseMouse writes getScreenWidth()/2 and getScreenHeight()/2 into xpos/ypos before grabOrReleaseMouse. New MouseHandlerMixin puts the remembered position back, driven by CursorManager.closeRootToNewScreen(), used on the two closes that open another screen. Left out: upstream's noMouseJumpTime config option and its 9 lang files, and the unrelated ConfigValueSlider.firstDrag change.
- [x] `e9a3453` Heterogeneous list entry defaults, config descriptions, several fixes
      9 kept files (+112/-63). Mixed bag. At least the list-default change and the
      `FormattedTextFieldWidget` validation fixes look version-agnostic.
      Result: partly done in `31e924c`. Five of eleven sub-fixes were still wrong at 26.2: MVTextEvents `new URI(...)` -> Util.parseAndValidateUntrustedUri (any scheme was accepted); NBTAutocompleteIntegration nextTagAllowed by bracket depth rather than cursor position, plus a new quote-aware nestLevel helper; the same filter now rejects `]`; ListNBTFolder getDefaultValue falls back to the last entry's type; HideFlags...TagReference HashMap -> LinkedHashMap. Two proved absent: the SplashOverlay isDone() guard is unnecessary because CompletableFutureCache.get() returns an already-completed future once LOADED, and the EventEditorWidget validation listeners are already there.
- [x] `2582d3d` Fixed server crash, fixed no slot restrictions not working on 1.21.5 servers
      5 kept files (+54/-20). "No slot restrictions" is a feature we ship and
      `server/ServerMixinLink.java` is a file we just edited. Check whether the server crash
      reproduces at 26.2.
      Result: partly done in `af5c089`. The serialization half reproduces: BootstrapMethods #7 of ServerboundContainerClickPacket is a method reference to buttonNum(), which our @Inject masked the flag out of, so it never reached the wire and ServerPlayNetworkHandlerMixin never saw one. Took upstream's shape: a noSlotRestrictions field, a @ModifyArg wrapping that getter in <clinit>, and a CTOR_HEAD @ModifyVariable that reads the flag back on a server thread. The crash half is N/A: AccessWidenedApi.setCursorStackSilently reaches remoteCarried through the access widener, not the reflection upstream dropped.
- [x] `2df9bb9` setBlockState SKIP_BLOCK_ENTITY_REPLACED_CALLBACK and SKIP_BLOCK_ADDED_CALLBACK ignored
      `mixin/toggled/NBTEditorMixinPlugin.java`, `server/NBTEditorServer.java`,
      `server/ServerMixinLink.java` (+32/-1). Stated range stops at 1.21.4; verify against 26.2's
      `setBlock` flag handling before deciding.
      Result: skipped, N/A. Upstream's own subject scopes it to 1.17-1.21.4, and our only block writes (NBTEditorServer:198 and :202) call setBlockAndUpdate, which passes no SKIP flag for a WorldChunk mixin to honour.
- [x] `2b38c22` Fixed ComponentsAccess-based crash in 1.20.5-1.21.4
      7 kept files (+20/-11). Components are still how 26.2 stores everything, so the crash may
      survive the version range.
      Result: skipped, N/A. Upstream's own subject scopes it to 1.20.5-1.21.4, and its substance is a new MVComponentsAccess wrapper plus two mixins standing in for a ComponentsAccess.get that threw there. At 26.2 DataComponentGetter.get returns null natively, and CLAUDE.md forbids new MV* wrappers. The null guard that does matter is 2e854bc's, taken in `00342a0`.
- [x] `2e854bc` Container component removed crash, duplicated screenshot options, hide flag ordering
      `containers/ContainerComponentContainerIO.java`, `mixin/ScreenshotRecorderMixin.java`
      (+5/-9), plus 2 deleted files. The "container component removed" crash is the part that
      matters; the hide flag half targets `1.20.5-1.21.4` structures we replaced.
      Result: partly done in `00342a0`. The container crash reproduces: ContainerComponentContainerIO.isSupported admits a null CONTAINER component and read then called allItemsCopyStream on it; BundleContentsComponentContainerIO already returned an empty array. The screenshot half is N/A -- our ScreenshotRecorderMixin declares one @ModifyVariable on Screenshot.grab, not the three intermediary-named ones upstream deduplicated. The hide flag half is N/A -- ComponentsHideFlag and HideFlagsComponentsTagReference are the 1.20.5-1.21.4 per-component show_in_tooltip model, replaced at 26.2 by tooltip_display, and the ordering fix already landed as the LinkedHashMap in `31e924c`.
- [x] `4403d8d` ':name' component name format, NBT Autocomplete suggestion filtering
      `addons/NBTEditorAPI.java`, `integrations/NBTAutocompleteIntegration.java`,
      `localnbt/LocalItemParts.java` (+37/-15).
      Result: done in `a42f4f6`. Proved on the test classpath that Identifier.tryParse(":custom_name") returns minecraft:custom_name at 26.2, so all three spellings are one key. TextUtil.addNamespace returned the bare-colon form untouched; LocalItemParts.setName knew two spellings and would write a second name; the autocomplete filter compared raw text and only for suggestions ending in '=', and dereferenced otherTags unguarded. Compared qualified on both sides with the removal '!' stripped, rather than upstream's six literal contains calls, which miss a non-minecraft namespace. Left out: the NBTEditorAPI javadoc wording.
- [x] `e9297fa` Fixed scissors getting broken by MVTooltip
      `multiversion/MVTooltip.java`, `nbteditor.accesswidener` (+12/-5). We still have MVTooltip,
      but `f4a192b` moved the GUI to render-state extraction, where scissor state is managed
      differently. Likely moot; confirm rather than assume.
      Result: done in `b8bd859`, by deletion rather than by port. javap proves setTooltipForNextFrameInternal only stores a Runnable in deferredTooltip, run after extractRenderState and a nextStratum() -- so nothing drew between MVTooltip's glDisable and glEnable, and the caller's enableScissor is popped before the tooltip draws. GUI scissoring is GuiGraphics' own ScissorStack at 26.2, not the GL state GlStateManager.SCISSOR reports. Upstream's bypassScissor, MVDrawableHelper and accesswidener entries all guard a clip the deferred tooltip is never subject to.
- [x] `290a846` Renamed NBTManager to SubjectIO
      23 kept files, 13 deleted, +64/-64. Pure rename. Taking it costs a `CONTEXT.md` glossary
      update and buys lower friction on every future pick. This is a naming decision, not a fix.
      Result: skipped, not a defect. A pure NBTManager -> SubjectIO rename over 28 files / 69 occurrences. It would reduce friction on later cherry-picks, but nothing misbehaves at 26.2, so it is a naming decision to take on its own terms rather than part of this defect pass. Filed as `issues/02-nbtmanager-to-subjectio-rename.md`.
- [x] `531ca58` Changed format of /get credits, changed homepage link to Modrinth
      8 lang files plus `fabric.mod.json` (+137/-113). The credits reformat is worth taking. The
      homepage change points at upstream's Modrinth page, which is wrong for a fork that is not
      published there. Take the lang hunks, decide the `fabric.mod.json` contact block separately.
      Result: done in f504f5e, lang hunks only; the Modrinth homepage hunk was reverted before amending. Applied before the three group A lang items because 2168510 and 36e98c7 build on the new credits numbering.
- [x] `c81d4d4` Changed dev builds to be identified with '-dev' instead of '.999'
      5 files (+22/-9). Our `mod_version` is still `2.0.4.999`. Cosmetic, and `build.gradle` and
      `gradle.properties` are files we rewrote. Low value, listed for completeness.

## Upstream group C: port by hand, the file upstream fixed no longer exists here

Each of these fixes a defect in code we deleted or moved. The pick will fail. The question is
whether our replacement has the same defect.
      Result: skipped, not a defect. Its build half is N/A -- we have no mergeDevLibs or mergeLibs tasks, and jar.archiveClassifier is already the git hash. The rest is a versioning convention (.999 -> -dev) plus the UpdateCheckerThread comparison that follows from it; our 2.0.4.999 sorts above every upstream release, so the outdated toast stays quiet, which for a fork checking upstream's version list is arguably the wanted behaviour. A decision, not a bug.
- [x] `136de6f` Editing show_item hover event with an enchanted item causing crash in 1.20.5-1.21.4
      Same family as the bug fixed in `25dd620`: encoding an item whose enchantment holder came
      from elsewhere. Check `util/StyleUtil` and the hover-event path at 26.2.
      Result: present, fixed in `8a553bd`. `MVTextEvents.HoverAction` encoded and parsed
      HoverEvent.CODEC through plain NbtOps. RegistryFixedCodec.encode errors for any ops that is
      not a RegistryOps, and Enchantment.CODEC is one. Probed in a dev client on an enchanted
      diamond sword: `PROBE hover threw java.util.NoSuchElementException: No value present`
      (the `.result().orElseThrow()` in getStringifiedValue), and after the fix it prints
      `{components:{"minecraft:enchantments":{"minecraft:sharpness":5}},id:"minecraft:diamond_sword"}`.
- [x] `e8d2651` More hover event crashes in 1.17, show_item hover with an enchanted item in 1.20.5+
      The second half is the one that matters, and its range is open-ended.
      Result: second half present, fixed in `8a553bd` with `136de6f` -- same root cause, one
      commit. `TextUtil.fromNbt`/`toNbt` took plain NbtOps while its JSON twin already took a
      serialization context; probed as `PROBE text threw NbtFormatException: Can't access registry
      ResourceKey[minecraft:root / minecraft:enchantment]`. The 1.17 half is reflection through a
      `Version` switch we do not have; `MVHoverActions.getAction`'s unknown-action guard is already
      ours as `HoverAction.getAction`, over an exhaustive switch on a sealed type.
- [x] `b7612ec` StringNBTFolder modifying cached empty NbtCompound and NbtList instances from SnbtParsing
      Shared mutable cached instances. Check whether `util/NbtIO` and
      `screens/nbtfolder/StringNBTFolder.java` alias the same way at 26.2.
      Result: skipped, not present. 26.2 allocates: `NbtOps.emptyMap()` is `new CompoundTag()` and
      `emptyList()` is `new ListTag()` in the bytecode, and SnbtGrammar holds no cached tag. Probed
      `NbtIO.parseSnbt` twice over `{}`, `[]` and `{a:{},b:[]}`: every pair, nested included, came
      back a distinct instance. Upstream's `.copy()` would only cost one.
- [x] `6f2d019` SNBT formatter incorrectly labeling out of bounds numbers as numbers
      Check `util/NbtFormatter.java`.
      Result: skipped, cannot exist. Both it and `f37c766` patch regexes in NormalSNBTFormatter1, deleted by `e7792e4`. Probed at 26.2: {a:999999999999b} and {a:99999999999999999999} fail the parse rather than colouring as numbers or strings, so the editor marks them unsafe. Pinned as tests in `8818fe8`.
- [x] `f37c766` SNBT formatter incorrectly labeling 1e1 as a number
      Check `util/NbtFormatter.java`. Pairs with the above.

## Upstream group D: skip

Recorded so the next sync does not re-evaluate them.
      Result: skipped, cannot exist. Probed at 26.2: {a:1e1} colours gold and parses, because the grammar reads it as a double -- upstream's 1.21.4 reader was right to refuse it and ours is right to accept it, and neither is a judgement the formatter makes any more. Pinned as a test in `8818fe8`.
- [x] `0f64b7e` Indentation and trailing-newline normalisation across 45 files (+1609/-1643).
      rejected: pure whitespace, and we have since rewritten most of those files. Accept the
      divergence and pick later items with `-Xignore-space-change`.
- [x] `41b8ab9` Updated GitHub workflow. rejected: `5d571f2` replaced ours for JDK 25.
- [x] `a5abf3a` Fixed incorrect DataVersion for 1.21.5. rejected: already applied, `local` has
      `"1.21.5": 4325`.
- [x] `8d30164` Successfully joined world in 1.17. rejected: 1.17 only.
- [x] `dc5e5a8` Various crashes opening the NBTEditorScreen in 1.17. rejected: 1.17 only.
- [x] `e3364f3` Invalid CustomName causing crash in 1.17-1.20.5. rejected: range excludes 26.2,
      all three files deleted here.
- [x] `18e76e8` Fixed build warnings. rejected: warnings were in files we deleted.
- [x] `b88061b` Refactored MVAbstractNbt*Parent into multiversion.nbt.elementio, fixing 1.17 crashes.
      rejected: restructures the multiversion layer we are dismantling.
- [x] `cc8f33b` Refactored MVTextEvents into multiversion.textevents, fixing 1.17 crashes.
      rejected: same reason. Scan the diff once for a version-agnostic fix hidden in the move.
- [x] `397d464` /open slot textures not appearing in 1.17-1.21.3. rejected: range excludes 26.2
      and it touches every ContainerIO we restructured. Verify slot textures render at 26.2 with
      `scripts/dev-client.sh` instead of picking this.
- [x] `3f2d44a` DonkeyChestContainerIO crash in 1.17-1.20.4. rejected: `d69fd92` deleted the
      shifted-slots branch the crash lived in.
- [x] `cdb36a5` Equipment using the wrong container io in 1.20.5-1.21.4, horse chestplate.
      rejected: superseded by `b681410`. Cross-check against `35e0ada` when doing that one.
- [x] `271c7ca` Crashes from custom color picker in 1.17-1.21.4. rejected: all 8 files are the
      shader-based picker we removed. Confirm the 26.2 colour picker works instead.
- [x] `a4ff87b` Bundle crash in 1.20.5-1.21.1. rejected: range excludes 26.2.
- [x] `6d9aee3` Tooltip overflow fix not working in 1.19 exactly. rejected: 1.19 only.
- [x] `be44835` Armor trim hide flag crash in 1.21-1.21.1. rejected: range excludes 26.2, file
      deleted here.

## The Thorioum fork, shape of it

`fork-thorioum/dev` (Thorioum/NBT-Editor26.1.2) branched from upstream at `c9d47cc` (2025-02-02)
and carries 21 commits we lack. It targets `minecraft_version=26.1.1` and keeps the full
multiversion layer, so it is behind us on version and behind upstream on fixes.

Its shape is different from upstream's. Four of the 21 are bulk version-port commits, up to
+8442/-3255, doing by hand what our 4.1 through 4.5 phases did. Those are worth reading and
worth nothing as picks. Seven more are bare `gradle.properties` version bumps. The pickable
residue is six commits.

Licensing is simpler here than for upstream. The fork predates upstream's 2026-06-15 relicence,
so its code is MIT, the same terms our tree is under today.

## Fork group A: take, applies at 26.2

- [x] `fd88a05` 2.0.67.03 - fixed head database cmds
      `containers/ContainerIOs.java`, `tsp/headdb/ported/Head.java`, `HeadDatabase.java` (+18/-7).
      Two separable fixes, and both call sites are still here verbatim.
      `Head.java:43` does `profile.properties().put("textures", ...)`, which they had to rebuild
      through an `ImmutableMultimap` because newer authlib hands back an immutable `PropertyMap`.
      Confirm whether 26.2's authlib throws there before taking it.
      `ContainerIOs.java:286` allocates `new ItemStack[maxSlots]` and never fills it, so the
      array holds nulls if the loop below leaves a gap. They added `Arrays.fill(sections, EMPTY)`.
      Ignore the `gradle.properties` hunk and their wildcard-import reformatting.
      Result: done in 09885d7 (Head.java, immutable PropertyMap.EMPTY) and 9397b95 (ContainerIOs.java, null section tail).
- [x] `b7841e2` 2.0.67.676768 - bug fixes
      `NBTEditorClient.java`, `screens/NBTEditorScreen.java`, `screens/NBTValue.java` (+13/-15).
      Two one-line changes worth a look. `getKey(null, ...)` becomes `getKey("", ...)`, which is
      a null-versus-empty defect in the key prompt. The other adds a missing `.start()` on a
      thread that was constructed and never run. Our `93d303f` restructured that area, so check
      whether either still applies rather than picking.
      Result: getKey half done in b96f912 (EditBox.setValue(null) NPEs at 26.2). Thread.start() half skipped, not present: tabs register synchronously in onRegistriesLoad.
## Fork group B: evaluate

- [x] `3af95c2` v2.0.5 - client chest updating, enchants of 0, items load on join
      6 kept files, 4 deleted (+34/-34). Two things in here.
      They deleted the eager `ClientChestHelper.loadDefaultPages(PageLoadLevel.NORMAL_ITEMS)`
      call at startup, on the reasoning that "minecraft's default registry doesnt have all the
      valid data needed to load all items". **Our `NBTEditorClient.java:78` still makes that
      call.** Same root-cause family as `25dd620`. Evaluate this together with upstream's
      `5e65608`, which attacks the same area from the other side.
      They also added `clientchest/ExtraDataFixes.java` (88 lines) to coerce enchantment level 0
      to 1 during a data-fixer upgrade. Judge that on its own merits.
      Skip their ender-chest-to-chest icon change, which is a personal preference.
      Result: skipped, not present. readPageSync forces the default manager via withDefaultManager and the dynamic flag defers server-registry items to DYNAMIC_ITEMS on join; dropping the eager load would regress that.
- [x] `b7a9c58` 2.0.67.02 - nbt formatter for new snbt features, button hover textures, startup crash
      9 kept files (+201/-138), including `util/NbtFormatter.java`. Overlaps upstream `377e0c2`,
      `f37c766` and `6f2d019`. Read both formatter fixes side by side and take whichever is
      better reasoned; do not take both.
      Result: skipped, superseded. Its formatter half is the same defect as `377e0c2`, already fixed in `e7792e4` against 26.2's grammar.
- [x] `696ff62` v2.0.4 - fixed dropdowns sometimes not working
      `screens/configurable/ConfigValueDropdown.java` (+19/-2). The defect is plausibly real, but
      the fix sets the value, closes the dropdown and plays a sound from inside `isMouseOver`,
      which is supposed to be a predicate. Reproduce it at 26.2 with `scripts/dev-client.sh`
      first. If it reproduces, fix it in `mouseClicked` rather than taking this.
      Result: skipped, not reproduced. Our ConfigValueDropdown routes clicks through mouseClicked only, with no side effects in isMouseOver, so the defect their fix works around is not present.
- [x] `8c15e82` v2.0.6 - bugfix (sign lines)
      `screens/factories/SignboardScreen.java`, `tagreferences/SignSideTagReferences.java`
      (+29/-8). Re-routes sign lines through `TextCodecs` at the NBT boundary. Their version
      wraps both directions in `catch (Throwable t) { t.printStackTrace(); }`, which swallows the
      failure and writes to stdout. Take upstream's `fd4b9c6` and `e9f26ca` first and only come
      back here if signs are still wrong.

## Fork group C: reference only, do not pick

None of these can be picked. They are listed because they are the only other person's answer to
problems we also had to solve, and they are worth opening when something at 26.x looks wrong.
      Result: skipped, already fixed. Its two real bugs are the ones `63e1b02` and `594dfdf` fixed.
- [x] `3dd9246` 2.0.67.676769 - 26.1.2 (10 kept, 6 deleted, +57/-36). **Read this one.**
      They hit our bug. Every `DynamicRegistryManagerHolder.get()` call in
      `ComponentItemNBTManager`, `ComponentEntityNBTManager`, `ComponentBlockEntityNBTManager`
      and `NBTComponentTagReference` is replaced with an inline
      `client.getConnection() == null ? VanillaRegistries.createLookup() : client.getConnection().registryAccess()`.
      That is what you write when the holder is always empty and you never find out why. It is
      independent confirmation of the `25dd620` diagnosis and should not be taken. Their new
      `mixin/IdMapMixin.java` and `mixin/RegistryEntryMixin.java` are worth a skim for the same
      reason.
- [x] `46d9342` 2.0.67.676767 - 26.1.2, the first iteration (291 kept, 111 deleted, +4456/-5365).
      Another person's 26.x migration in one commit. Useful when a 26.x API question comes up.
- [x] `918475c` v2.0.67 - 1.21.10 (245 kept, 106 deleted, +8442/-3255). Covers the same GUI
      rewrite our `84618f6` and `f4a192b` handled. Cross-check against ours if a widget
      misbehaves.
- [x] `ea8dc3a` v2.0.6 - 1.21.5 (55 kept, 35 deleted, +317/-363).
- [x] `6d94322` v2.0.3 - 1.21.4 (14 kept, 6 deleted, +106/-52). Touches
      `DynamicRegistryManagerHolder` and `mixin/toggled/RegistryEntryReferenceMixin`, so it is
      the earliest point in their history where the registry problem shows up.

## Fork group D: skip

- [x] `2df496a` Update README.md. rejected: describes their fork.
- [x] `df6bae7` Merge remote-tracking branch 'origin/dev' into dev. rejected: no content.
- [x] `7a98be0` 2.0.67.01 - smaller shader bugfix. rejected: the shader files were deleted here.
- [x] `e1fd248`, `fc4ad8e`, `4d345ad`, `a38a5a4`, `2e23316`, `f47515a`, `7a1ec65`.
      rejected: version bumps and one-line follow-ups to the port commits above.

## Licensing, decide before any picking

- [x] `2847a04` Changed LICENSE to MPL-2.0, and `985586d` LICENSE files for tsp.headdb and
      multiversion.commands. Our `LICENSE` is still the MIT text. Upstream relicensed on
      2026-06-15. This is a decision for the repo owner, not a mechanical pick. Resolve it
      before taking code from commits that postdate the relicence.
      Result: done: owner chose MPL-2.0 on 2026-09-17. Cherry-picked as 1a87ade and 5aa4e67; fabric.mod.json license line now MPL-2.0.
## Adjacent findings

- `EquipmentContainerIO.KEYS` addresses the boots slot as `"boots"`. Verified against
  `net.minecraft.world.entity.EquipmentSlot` in the 26.2 deobf jar, where the name is `"feet"`.
  See upstream group A item one.
- `NBTEditorClient.java:78` still calls `ClientChestHelper.loadDefaultPages(PageLoadLevel.NORMAL_ITEMS)`
  at startup, before any server registries exist. Both the fork and upstream changed this
  independently. See fork group B `3af95c2` and upstream group B `5e65608`.
- `tsp/headdb/ported/Head.java:43` and `containers/ContainerIOs.java:286` are unchanged from the
  code the fork had to patch. See fork group A `fd88a05`.
- Three independent parties hit the registry-set problem that `25dd620` fixed. Upstream attacked
  it in `5e65608`, the fork worked around it in `3dd9246`, and we traced it to the intermediary
  class lookup. Only one of those three is a root-cause fix.

## Counts

- Upstream `us/dev`, 50 commits. 14 take, 13 evaluate, 5 port by hand, 16 rejected (one of which
  we had already fixed independently), 2 held for the licensing decision.
- Fork `fork-thorioum/dev`, 21 commits. 2 take, 4 evaluate, 5 reference only, 10 rejected.
- Real judgment is needed on 17 items in total, and 6 of those are the ones that touch the
  registry and component paths.
