# Item 11 — Partition `MainUtil`

Audit finding T4: `util/MainUtil.java`, 497 lines, 44 public statics, god node #1.
Same pathology as T3 (`MVMisc`), so the same fix as checklist item 7: inline the
pass-throughs, fold the single-caller helpers into their caller, rehome the deep ones
to the module that owns the concept, and name what is left.

## The measurement that shapes the plan

`MainUtil` has 384 references across 124 files. **269 of them (70%) are `MainUtil.client`**
— a `static final` cache of `Minecraft.getInstance()`. The god node is mostly one field.
The 43 methods are thin on the ground: 12 have a single call site, 25 have three or fewer.

So this is not one big module to split. It is one pass-through field plus a long tail.

## Destinations

| member | sites | destination | why |
| --- | --- | --- | --- |
| `client` | 269 | inline `Minecraft.getInstance()` | pass-through; 70% of the god node |
| `getCustomItemNameSafely` | 2 | inline `item.getHoverName()` | pass-through |
| `setType` ×2 | 4 | inline `item.transmuteCopy(..)` | pass-through |
| `equals` ×2 | 1 | fold into `ConfigValueSlider` | one caller |
| `newEvent` | 1 | fold into `ItemLostCallback` | one caller |
| `getFormattedCurrentTime` | 1 | fold into `NBTExportCommand` | one caller |
| `scaleImage` | 1 | fold into `ImageToLoreWidget` | one caller |
| `mapMatrices` | 1 | fold into `ItemTooltips` | one caller |
| `readNBT` | 3 | `util/NbtIO` | T4 names the duplication with `NbtIO.read` |
| `getDyeColor` | 1 | `util/StyleUtil` | beside `isColor` / `getColor` / `getByName` |
| `colorize`, `stripColor` | 5 | `tsp/headdb/ported/Utils` | every caller is in the Bukkit port; these are Bukkit-isms |
| `setTextFieldValueSilently` | 4 | `util/AccessWidenedApi` | writes `EditBox.value`, a widener line |
| `setCursorStackSilently` | 5 | `util/AccessWidenedApi` | merges with `setPreviousCursorStack`, its only caller |
| `getNbtNameSafely` | 4 | `util/TextUtil` | reads a `Component` out of NBT |
| `addNamespace` | 2 | `util/TextUtil` | beside the other plain-string helpers |
| `fillId` | 4 | `containers/ContainerIO` | 3 of 4 callers; beside `ContainerIO.isEmpty` |
| `copyAirable`, `getBaseItemNameSafely` | 6 | `localnbt/LocalItemStack` | 4 of 6 callers, and both are about the stack behind a `LocalItem` |
| `mergeFutures` | 2 | **new** `util/Futures` | `LoadQueue` and `SaveQueue` are unrelated classes with no shared base |
| `renderLogo`, `drawWrappingString`, `getMousePos` | 15 | `util/Drawing` (`MVDrawableHelper`, moved) | one drawing module, not two |
| `clickCreativeStack`, `dropCreativeStack`, `saveItem` ×3, `get`, `getWithMessage` | 20 | **new** `util/PlayerItems` | one concept: write an item to the player and sync it with a creative packet |
| `intPredicate` ×3, `parseOptionalInt`, `parseDefaultInt` | 16 | **new** `util/IntFields` | pure; the first testable thing in this file |
| `update`, `updateDynamic` ×3 | 11 | **new** `util/DataFixes` | NBT data-version migration |

Three new files, one moved file, everything else lands somewhere that already exists.

### Why `MVDrawableHelper` moves

`CLAUDE.md`: *"a deep one whose package is the only thing version-flavoured about it belongs
in `util/` instead."* `MVDrawableHelper` is that — its own javadoc says it covers "the calls
the mod makes in more than one place", which is exactly what `renderLogo` and
`drawWrappingString` are. Putting them anywhere else creates the second overlapping drawing
grab-bag that T4 complains about. Its twelve one-line pass-throughs are A1's job, not this
item's, and the move does not touch them.

### What gets pinned

`drawWrappingString` is the only non-trivial algorithm in the file and it has never been
tested. Its wrapping half needs nothing but a width function, so it comes out as
`TextWrapping.wrap(text, maxWidth, width)` and gets tests; `Drawing.drawWrappingString`
becomes wrap-then-draw. `IntFields` is pure and gets tests too.

## Checklist

- [x] 11a. Inline the pass-throughs: `client`, `getCustomItemNameSafely`, `setType`
- [x] 11b. Fold the five single-caller helpers into their callers
- [x] 11c. Rehome to existing modules: `NbtIO`, `StyleUtil`, headdb `Utils`,
      `AccessWidenedApi`, `TextUtil`, `ContainerIO`, `LocalItemStack`
- [x] 11d. Move `multiversion/MVDrawableHelper` to `util/Drawing`
- [x] 11e. Extract `util/TextWrapping` + tests; move the three drawing helpers into `Drawing`
- [x] 11f. Extract `util/PlayerItems`
- [x] 11g. Extract `util/IntFields` + tests
- [x] 11h. Extract `util/DataFixes` and `util/Futures`
- [x] 11i. Delete `MainUtil`; build and full test suite green

Each step is its own commit and each commit builds on its own.

## Outcome

Done across 13 commits, each verified to build on its own (13/13). 143 files,
+1392/-1093. `MainUtil` is deleted; `util/` gained `PlayerItems`, `IntFields`,
`DataFixes`, `Futures`, `TextWrapping` and `Drawing` (the last moved, not new).
Tests 60 -> 80.

Two things came out of the extraction that were not in the plan:

- `TextWrapping`'s mid-word split stepped back one character from the first that
  did not fit, which lands on zero when the first character is already wider than
  the line -- an infinite loop inside a render call. Fixed on its own commit, with
  a timeout-guarded test, after the extraction had landed unchanged.
- `IntFields.intPredicate`'s fixed-bound overload took `Integer` and forwarded it
  into a supplier, so a null bound passed the "no bound" check and then
  dereferenced. The parameters are `int` now. No caller was passing null.

Not done: `Drawing`'s twelve one-line forwards to `GuiGraphicsExtractor`. They are
A1's inlining pass, which is not on this checklist; the move only changed where
they live.
