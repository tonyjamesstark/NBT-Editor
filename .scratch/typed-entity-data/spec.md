# Typed entity data

The 26.2 `entity_data` and `block_entity_data` item components are `TypedEntityData`, with the
id outside the tag. The mod declared and read them as `CustomData`. Reading one the game had
filled threw `ClassCastException`: a server-given spawn egg crashed the client on hover in
creative (report 2026-10-06, nbteditor-3.0.1-f7846d5). Writes stored a `CustomData` under the
component.

Fix: `ItemTagReferences.getComponentTagRefOfTypedNBT` reads the tag with the type's id put
back, and writes with the type from the tag's `id`, else the item's current type.

Test plan: [`issues/01-thorough-test.md`](issues/01-thorough-test.md).
