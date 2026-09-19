# Verify startup on a headless dev client, not a Purpur server

## Status

accepted — 2026-09-16, supersedes the deferred "no local server" position of the same date

## Context

Four consecutive builds crashed the client on launch, each on a different fault, and each
was only discovered on the Mac one Prism install cycle at a time. The Linux build host
builds and checks but never launched the game, so the whole startup path was unverified
between commits.

Purpur was the first candidate and is the wrong tool. It is a Paper fork and speaks the
Bukkit plugin API, so it cannot load a Fabric mod at all. `fabric.mod.json` declares
`"environment": "client"` since 7538861, so even a Fabric dedicated server skips the jar.
Every fault in the series was client-side. Three were mixin apply failures during class
loading, and the fourth was a `NullPointerException` in `onRegistriesLoad`. None of them
involves a server speaking to a mod.

What the host was actually missing was a display. `xvfb` plus Mesa's llvmpipe software
rasteriser supplies one. The dev client reaches the title screen in about 35 seconds on
this box with no GPU.

A vanilla Fabric dedicated server is still worth running, for a different reason than the
one Purpur was proposed for. 26.2 binds item components through `DataComponentInitializers`,
reached only from `RegistryDataCollector` when joining a server and from
`ReloadableServerResources` when a world loads. Neither runs during client init. That is
precisely what broke in `onRegistriesLoad`, and only a real join exercises it.

## Decision

Four layers, cheapest first.

`tools/check-mixin-targets.py` catches the mixin class at build time. 956e540 extended it to
compare an `@Inject` handler's declared parameters against its target's descriptor, which
found three defects on a run where the crash report had named one.

`scripts/dev-client.sh` boots the loom dev client under Xvfb and exits non-zero on a crash
or a timeout. It reproduced the `onRegistriesLoad` crash locally with 27 of the 28 installed
mods absent, which is what proved the fault was ours and not an interaction.

`scripts/dev-client.sh --join`, against `scripts/dev-server.sh`, connects to a local
dedicated server and succeeds when the server logs the player in. This is the only layer
that reaches the component-binding path. It takes about 35 seconds once the server is up.

`scripts/dev-client.sh --screens` implies the join and then installs `dev/DevScreenSweep`, which
opens every factory screen in turn against an item carrying the non-ASCII lore a 2026-09-16 report
pointed at. It also checks two values that compile, remap and sweep clean while wrong: the entity
id a minecart's container io writes, which only an edited item's NBT shows, and whether the config
screen builds its widget per setting at all. This layer arrived in 25dd620, after the first three
were accepted, and it is what caught the `class_634` defect there.

The sweep lives in its own `dev` source set. A jar players install should not carry a test robot,
and keeping it out of `src/main` also keeps `src/main` from calling it: `NBTEditorClient` reaches
it by `Class.forName` and a production launch finds nothing. `check` compiles the source set, so
the sweep cannot rot unnoticed while nobody runs it.

Purpur is rejected outright rather than deferred. Nothing about it would ever have loaded
the jar.

## Consequences

Startup is now verifiable on the build host, so a launch crash costs a minute rather than an
install cycle on another machine. The Mac keeps the interface testing that neither Xvfb nor
a log grep can do.

RAM is the binding constraint and it is tight. Each JVM is capped at 1 GB in the loom `runs`
block. Measured on this host, the client peaks near 1.05 GB resident, the server sits at
about 730 MB, and the Gradle daemon holds another 830 MB. With the qemu VM running, all
three together drive available memory to roughly 400 MB against 1.8 GB of swap already in
use. Do not run a build concurrently with a join run.

The `Class.forName` seam is the cost of the source set. Renaming or moving `dev/DevScreenSweep`
would compile clean and simply stop the sweep installing, so `validateReflectiveNames` in
`build.gradle` resolves every `Class.forName` on one of the mod's own classes against both source
trees and fails `check` when one does not exist. The rule is encoded rather than written down.

`server/NBTEditorServer.java` and the `packets/` classes stay unexercised. They are already
unreachable from a client-only jar, so this costs nothing today. Taking server-side
capabilities back into scope would mean reverting `environment` to `"*"`, at which point the
existing `scripts/dev-server.sh` would load the mod with no further work. It would still
never be Purpur.
