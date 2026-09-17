# Do not stand up a local server to test this mod

## Status

deferred — considered 2026-09-16, not adopted, revisit only under the trigger below

## Context

Four consecutive builds crashed the client on launch, each on a different mixin, so the
question came up of whether a local server would catch these before a Prism install cycle.
Purpur was the candidate.

It would not catch them, for three independent reasons.

Purpur is a Paper fork, so it speaks the Bukkit plugin API. It cannot load a Fabric mod at
all. Nothing about the jar is legible to it.

`fabric.mod.json` declares `"environment": "client"`. 7538861 narrowed it from upstream's
`"*"`, so even a Fabric dedicated server skips the jar. That also disables the upstream
feature 2b2ceef added, the expanded capabilities when the mod is installed server-side.
`ROADMAP.md` puts all server-side content explicitly out of scope, so the narrowing stands.

Every failure in the series is a mixin apply failure during client class loading, raised by
`MixinProcessor` before `Minecraft.<init>` returns and long before a connection exists. A
server is not on that code path. `ItemModelManagerMixin` died during `Minecraft.<init>`,
and `ChatInputSuggestorMixin` would have died on the first chat screen. Neither involves a
network.

The cost, had it been worth paying, on this host:

| cost | figure |
| --- | --- |
| jar | Purpur 26.2 build 2633, needs Java 25, already present as `25.0.4-tem` |
| RAM | 2 GB heap minimum, ~2.7 GB resident; the host has 2.4 GB available and 1.6 GB of swap already in use, with qemu holding 2.8 GB |
| CPU | 4 cores, single-threaded tick loop, competing with Gradle |
| disk | negligible against 369 GB free |
| ongoing | a second surface to keep pinned at 26.2, and a second build-and-install loop |

RAM is the binding constraint. `gradle.properties` already caps Gradle at 1 GB heap and one
worker, which is the same edge found from the other direction.

## Decision

No local server. The launch-crash class is covered at build time instead, by
`tools/check-mixin-targets.py`, which 956e540 extended to compare an `@Inject` handler's
declared parameters against its target's descriptor. That extension found three defects on
the first run, where the crash report had named one.

Behaviour that the checker cannot reach is verified by `./gradlew runClient` on the machine
with a display, per `docs/adr/0004-jvm-only-tests.md`. The Linux build host is headless,
with no `DISPLAY` and no Xvfb, so it builds and checks but never launches.

## Consequences

The client-to-server protocol in `server/NBTEditorServer.java` and the `packets/` classes
stays unexercised. It is already unreachable from a client-only jar, so this costs nothing
today.

Reopen this only if the fork takes server-side capabilities back into scope. The order would
then be to revert `environment` to `"*"`, stand up a **Fabric** dedicated server rather than
Purpur, and host it somewhere other than this box, which has no RAM to spare.
