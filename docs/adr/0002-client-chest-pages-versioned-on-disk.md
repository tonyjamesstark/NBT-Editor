# Client-chest pages are one file each, stamped with the data version that wrote them

## Status

accepted

## Context

The client chest is storage the player owns locally. It survives worlds, servers and game
updates, which makes it the only place in this mod holding data that outlives the release it was
written on. Minecraft's item NBT changes shape between versions, so a page written on an older
release cannot simply be handed to the current one.

## Decision

Each page is its own file, `page<N>.nbt` in the client-chest folder, and carries the data version
of the release that wrote it. Nothing is migrated on startup. A page is fixed up through
`DataFixes` when it is loaded, and `DataVersionStatus` (`UNKNOWN`, `OUTDATED`, `CURRENT`,
`TOO_UPDATED`) decides whether that is even possible.

A page whose data version is not the current one refuses to hand out its items:
`ClientChestPage.getItemsOrThrow` throws rather than returning stacks that were parsed under the
wrong rules.

## Consequences

The on-disk shape is a compatibility surface. Changing the file name pattern, the per-page
stamp, or the meaning of a `DataVersionStatus` value breaks chests that already exist on players'
disks, and there is no server to migrate. Treat it the way you would a database schema.

Reading is per page rather than whole-chest, which is what makes `PageLoadLevel` meaningful: a
page can be listed without being loaded, and loaded without resolving dynamic item registries.
That granularity exists to keep the editor responsive, and it is the reason page IO is
concurrent and lock-partitioned rather than one lock over the whole chest.

`TOO_UPDATED` is not a corruption case. A player who downgrades the game keeps pages written by a
newer release, and those pages stay on disk, unreadable but intact, until the newer client comes
back.
