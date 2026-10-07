# Entity and block entity data crash: thorough test

Status: `ready-for-human`
Created: 2026-10-06

Hovering a server-given spawn egg in creative crashed the client: `ENTITY_DATA` and
`BLOCK_ENTITY_DATA` were read as `CustomData`, but 26.2 stores `TypedEntityData`. Fixed on
`fix/typed-entity-data`. This ticket is the test plan the fix has to pass in a dev client.

## Checklist

- [x] `MVComponentType` takes `Supplier<DataComponentType<T>>`, so javac checks every declaration
      (proved: the old `CustomData` declaration no longer compiles)
- [x] Tag reference semantics: empty, copy-on-read, id on read, id missing / unknown / changed on
      write, null removes
- [x] Every item in the registry, bare and carrying game-built entity + block entity data:
      tooltip, `ContainerIOs.isSupported`, read, write-back, read-back, codec + stream codec
      round trip
- [x] Every spawn egg: tooltip and the spawn-egg container io round trip keep the entity type
- [x] `LocalEntity.toItem` for entity types with their own item (frames, painting, minecarts,
      boats, armor stand) and a spawn egg: component type matches, encodes
- [x] The NBT editor's raw item NBT view (`nbte$getNbt`/`nbte$setNbt`) round trips both components
- [x] Signboard screen on a sign and a hanging sign writes typed `sign`/`hanging_sign` data
- [x] `/nbteditor export cmdblock` gives a command block with typed `command_block` data
- [x] End to end: dev server `/give`s a spawn egg and a lectern with data, hovered in the real
      creative inventory under Xvfb, screenshot taken, no crash
- [x] The new checks fail against the pre-fix code
- [x] Full `--screens` sweep and `./gradlew build` green

## Comments

### 2026-10-06, agent

All checks are in `src/dev/.../DevTypedDataCheck.java` and run first in `scripts/dev-client.sh --screens`.

- Fixed code: 0 `SWEEP fail`. The sweep covered 1536 items, 155 of them with a container io. 88 items (every spawn egg)
  carry `entity_data` by default, so before the fix *any* spawn egg crashed on hover in the
  creative inventory tab, server-given or not. No item carries default `block_entity_data`.
- Real hover: `glfwSetCursorPos` alone does nothing on Xvfb, because GLFW/X11 does not call the cursor
  callback for its own warp. The check therefore also calls `MouseHandler.onMove`, the callback's
  target. `hoveredSlot` confirms the hover. Screenshots: `run/screenshots/typed-hover-*.png`.
  They show the "Ctrl + Space to open container" line, which comes from the `isSupported` call
  that used to throw.
- Pre-fix `src/main`: every check group fails. The hover crashes the client with the reported
  trace (`ComponentTagReference.get:54` ← `ContainerIOs:96` ← `ItemTooltips:74` ←
  `CreativeModeInventoryScreen`). With the hover moved last, the signboard and export checks also fail. The
  client then gets disconnected: `set_creative_mode_slot` fails to encode. The harness does not treat a
  disconnect as fatal, so it waited out its deadline.
- `./gradlew build` green.
