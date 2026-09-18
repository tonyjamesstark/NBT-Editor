# Target one Minecraft version; dismantle the multi-version layer

## Status

accepted

## Context

The upstream mod shipped one jar that ran on 1.17 through 1.21. It paid for that with a
`multiversion/` package of signature shims: `MV*` wrappers around APIs whose shape changed
between releases, a `Reflection` helper for members that could not be named at compile time, and
a second Gradle module (`nbteditor_1.17`) compiled against the older mappings.

This fork targets **one** `minecraft_version` at a time, currently 26.2. The span the machinery
existed to bridge is gone, but the machinery outlived it: most `MV*` members are now one-line
pass-throughs to a single current API, and they cost every reader an extra hop with nothing on
the other side.

## Decision

The jar targets a single game version. New code calls the Minecraft API directly. `MV*` members
that are one-line delegations get inlined when touched; the genuinely deep ones stay until
something replaces them, and a deep one whose only version flavour is its package moves to
`util/` rather than being preserved in `multiversion/`.

**Do not add new `MV*` wrappers.** 20 remain; the number should only go down.

## Consequences

Supporting an older release again means a separate branch or a separate jar, not a shim layer.
That is the trade accepted here: the fork is maintained against current Minecraft, and the cost
of a hypothetical backport is paid by whoever needs one rather than by every reader of every
file.

`Version.java` is **not** part of what gets dismantled. It no longer gates behaviour, but it
still reads `version.json` / `data_versions.json` and exposes the release target and data
version, which client-chest persistence depends on (see ADR-0002).
