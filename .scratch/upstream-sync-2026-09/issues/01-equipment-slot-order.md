# Equipment slot order differs from upstream

Status: `ready-for-human`
Created: 2026-09-18
Source: [`../spec.md`](../spec.md), upstream group A item `4d10512`

`4d10512` reordered the equipment container's slots to put the hands before the saddle and body
slots. We took the half of that commit that fixes a defect and left the reorder, because nothing
misbehaves either way.

`EquipmentContainerIO.java:26` reads:

```java
private static final String[] KEYS = {
        "head", "chest", "legs", "feet", "saddle", "body", "mainhand", "offhand"};
```

Upstream's order is head, chest, legs, feet, mainhand, offhand, saddle, body, with
`HORSE_ARMOR_TEXTURES` and `LLAMA_ARMOR_TEXTURES` permuted to match.

## Why it is a decision rather than a task

The arrays are index-parallel and nothing else keys off the order, so the change is mechanical.
What it costs is that an entity edited on one build and reopened on the other shows its items in
different boxes. The NBT keys carry the meaning, so nothing is lost or mis-saved, but a player
mid-edit sees items move. Matching upstream is worth that only if we expect to keep picking from
their container code, which the spec's counts say we mostly do not.

Taking it means changing both texture arrays in the same commit. They are positional, and a
partial edit silently draws a sword in the saddle box.

## Comments
