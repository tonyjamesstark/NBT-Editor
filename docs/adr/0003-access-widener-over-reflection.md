# Reach private vanilla members through the access widener, not reflection

## Status

accepted

## Context

The editor needs a handful of Minecraft members that vanilla keeps to itself: a creative-screen
slot wrapper's target, an `EditBox`'s backing string, `Style`'s nullable flag fields, and a few
constructors whose public equivalents copy when the editor wants to rewrite a whole set at once.

Two mechanisms were available. Reflection, which the multi-version layer already used because it
could name members that did not exist on every supported release (see ADR-0001). Or Fabric's
access widener, which edits the member's visibility at load time so the call compiles like any
other.

## Decision

Widen the member in `nbteditor.accesswidener` and call it normally. Every such call lives in
`util/AccessWidenedApi`, one method per widener line, each naming its line in javadoc.

One kind of access cannot move there. A class that subclasses a vanilla type and overrides one of
its methods has to read and write the state that method owns, and a static helper cannot stand in
for `this.keepSuggestions` inside an override. `screens/widgets/SuggestingTextFieldWidget` is the
only such place: it drives a `CommandSuggestions` and reimplements `updateCommandInfo`, which is
what six of the widener's lines exist for. A mixin is the same case, since it is compiled into the
class it targets.

## Consequences

A Minecraft update that renames or removes a widened member breaks **the build**, at one file,
instead of throwing at runtime inside whatever screen happened to touch it. That is the whole
point of the choice, and the reason the calls are collected rather than scattered: the seam is
visible from both sides, and deleting a widener line makes exactly one file stop compiling.

`AccessWidenedApi` holds nothing else. A method that compiles against the public API is ordinary
mod code and belongs in the package that owns the concept.

`tools/check-widener-consumers.py` is what makes the rule true rather than aspirational, and it
runs in `check`. It reads the constant pools of the mod's own compiled classes and fails on a
widener line no mod class uses, on an access outside `AccessWidenedApi` that vanilla's own
modifiers would have refused, and on an exemption that no longer matches anything, so the list of
exceptions above cannot quietly grow or rot. Loom's `validateAccessWidener` is the other half: it
proves each line still names a member that exists.

The rule is about reach, not about inheritance. `Screen.addRenderableWidget` is protected, so a
mod screen calling its own inherited copy is not using the widener at all and does not have to
route anywhere; `AbstractWidget.x` was private, so a mod widget touching it was. The checker
needs the un-widened game jar to tell those apart, because Loom puts the already-widened one on
the compile classpath, and it refuses to run without it rather than passing vacuously.

Reflection survives at one call site, `server/NBTEditorServer.java:150`, which looks a synthetic
lectern field up by intermediary name. It is not covered by this decision and is not a precedent
for new ones.
