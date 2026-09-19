# Reach private vanilla members through the access widener, not reflection

## Status

accepted

## Context

The editor needs a handful of Minecraft members that vanilla keeps to itself: a creative-screen
slot wrapper's target, the NBT tag-type table, an `EditBox`'s backing string, `Style`'s nullable
flag fields, and a few constructors whose public equivalents copy when the editor wants to
rewrite a whole set at once.

Two mechanisms were available. Reflection, which the multi-version layer already used because it
could name members that did not exist on every supported release (see ADR-0001). Or Fabric's
access widener, which edits the member's visibility at load time so the call compiles like any
other.

## Decision

Widen the member in `nbteditor.accesswidener` and call it normally. Every such call lives in
`util/AccessWidenedApi`, one method per widener line, each naming its line in javadoc.

## Consequences

A Minecraft update that renames or removes a widened member breaks **the build**, at one file,
instead of throwing at runtime inside whatever screen happened to touch it. That is the whole
point of the choice, and the reason the calls are collected rather than scattered: the seam is
visible from both sides, and deleting a widener line makes exactly one file stop compiling.

`AccessWidenedApi` holds nothing else. A method that compiles against the public API is ordinary
mod code and belongs in the package that owns the concept.

Reflection survives at one call site, `server/NBTEditorServer.java:150`, which looks a synthetic
lectern field up by intermediary name. It is not covered by this decision and is not a precedent
for new ones.
