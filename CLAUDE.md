# NBT-Editor

Fabric mod for editing items, blocks, and entities in Minecraft, client-side. Fork of
[mega12345mega/NBT-Editor](https://github.com/mega12345mega/NBT-Editor); `origin` is our fork,
`us` is upstream.

## Build

Gradle + fabric-loom, `release = 25` on JDK 25 (`sourceCompatibility`/`targetCompatibility` are
`VERSION_25` in `build.gradle`). One module: root (`src/`), compiled against the
`minecraft_version` in `gradle.properties` — currently **26.2**. `settings.gradle` declares no
subprojects; the old `nbteditor_1.17` module and the `mergeRefmapJson` task are gone.

Three source sets inside that one module. `src/main` is the mod, `src/test` is the JUnit slice,
and `src/dev` holds the screen sweep the harness drives. `src/dev` is on the client run's
classpath and is compiled by `check`, but never enters the jar, so `src/main` reaches it by
`Class.forName` and a production launch finds nothing. Do not import a `dev` class from
`src/main`; it will not compile.

`./gradlew build`. JUnit 5 runs over the pure-JVM slice only: twelve test classes under
`src/test/java/`, 103 tests. Anything that touches registries, `Minecraft.getInstance()` or the run
directory cannot run without a Fabric launch, so CI (`.github/workflows/build.yml`) proves that
slice plus compile-and-remap, and nothing about the mod's behaviour. Verify that by running the
mod in a dev client. See `docs/adr/0004-jvm-only-tests.md`.

`scripts/dev-client.sh` does that here without a display, on Xvfb plus Mesa's llvmpipe. It
exits non-zero on a launch crash and takes about 35 seconds. `scripts/dev-client.sh --join`
goes further and connects to `scripts/dev-server.sh`, which is the only way to reach the
path that binds item components. `--screens` implies the join and runs `dev/DevScreenSweep`,
which opens every factory screen and the config screen and checks the entity ids the minecart
container ios write; it is the only thing in the build that opens an editor screen at all, and it
takes about two minutes. Run one of these, not a Gradle build at the same time;
the host does not have the RAM. See `docs/adr/0005-headless-dev-client-harness.md`.

`check` also runs two mixin validations, because nothing else in the build looks at a mixin's
targets. `validateMixinConfigs` (in `build.gradle`) fails when a `*.mixins.json` names a class
that is not there. `checkMixinTargets` runs `tools/check-mixin-targets.py`, which resolves each
`@Mixin` target, `method` selector and `@At` member against the whole compile classpath and
checks that the selected method's bytecode really contains that call. Both exist because a stale
mixin name compiles and remaps clean and only fails at launch, and one on a screen class does not
fail until that screen opens. Rerun the script after every `minecraft_version` bump.

`check` runs `validateReflectiveNames` too, which resolves every `Class.forName` on one of the
mod's own classes against `src/main/java` and `src/dev/java`. It exists because the screen sweep is
reached by name, so moving or renaming it would compile clean and silently stop it installing.

**The jar ships no refMap.** Runtime classes carry Mojang names, so a mixin annotation string is
used verbatim and an intermediary name (`method_*`, `class_*`, `field_*`) never resolves.
`checkMixinTargets` fails the build on one. Reflection through `multiversion/Reflection.java` is
the opposite case and correctly stays intermediary, because Fabric's `MappingResolver` does
translate at runtime. Do not "fix" those call sites to Mojang names.

Lambda targets such as `SnbtParsingMixin`'s `lambda$createParser$13` are the residual risk: the
index is assigned by javac and shifts when a lambda is added above it. The checker proves the
selected method exists and contains the redirected call, not that it is the right one of several
that do. Reread the bytecode when a lambda target moves.

`jar { archiveClassifier = getGitHash() }` means every build drops a differently-named jar into
`build/libs`; clean it out periodically rather than assuming the newest name.

## Multi-version layer (vestigial — being dismantled)

The jar targets **one** game version. The multi-version machinery predates the 26.2 migration and
mostly no longer earns its keep:

- `Version.java` does **not** gate behaviour. It reads `version.json`/`data_versions.json` and
  exposes the release target and data version. That part is load-bearing: client-chest pages store
  a `DataVersion` and are migrated against it. Keep it.
- The `MV*.java` classes and `Reflection.java` were signature shims across 1.17–1.21. Most are now
  pass-throughs to a single current API. `TextInst` and `IdentifierInst` are gone: the identifier
  and text constructors were inlined to `Identifier` and `Component`, and the text
  SNBT/JSON/string unification moved to `util/TextUtil`, beside the safe wrappers that already
  fronted it.
- `loom:injected_interfaces` in `fabric.mod.json` and the access widener are still real mechanisms
  and are unrelated to version spanning.

**Do not add new `MV*` wrappers.** Call the Minecraft API directly. When touching an existing
`MV*` member that is a one-line delegation, inlining it is welcome; the deep ones stay, and a deep
one whose package is the only thing version-flavoured about it belongs in `util/` instead --
`MVMisc` was emptied that way (NBT file IO to `util/NbtIO`, the widener reach-throughs to
`util/AccessWidenedApi`), `MVDrawableHelper` moved whole to `util/Drawing`, and `MVTextEvents` to
`util/TextEvents`.

There is no `MainUtil`. Its 44 statics were partitioned across `util/PlayerItems`,
`util/IntFields`, `util/DataFixes`, `util/Futures`, `util/TextWrapping`, `util/Drawing` and the
modules that already owned each concept; `MainUtil.client` was the vanilla singleton accessor and
is now `Minecraft.getInstance()` at the call site.
See `docs/AUDIT-2026-09-14.md` for the partition and the ordered plan.

## Agent skills

### Issue tracker

Issues and specs live as markdown under `.scratch/<feature>/`; no `gh` CLI on this host. See `docs/agents/issue-tracker.md`.

### Triage labels

The five canonical roles, unchanged: `needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context. `CONTEXT.md` at the repo root is the glossary; read it before naming anything.
`docs/adr/` holds the decisions a reader would otherwise have to guess at (single release target,
client-chest persistence, the access-widener seam, test scope). See `docs/agents/domain.md`.
