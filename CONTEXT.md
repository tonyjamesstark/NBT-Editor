# NBT Editor

A client-side Minecraft mod for editing the NBT of items, blocks and entities, plus a local
"client chest" that stores items outside any world. Everything here is the mod's own vocabulary.
Vanilla Minecraft terms (item stack, component, screen handler) keep their vanilla meaning and
are not redefined below.

## Language

### Editing a thing

**Local NBT**:
An editable in-memory copy of one item, block or entity, held entirely on the client. It is what
the editor mutates; nothing is sent anywhere until it is saved.
_Avoid_: snapshot, draft, working copy.

**NBT reference**:
The handle that says where a piece of local NBT came from and how to write it back. Reading and
saving go through the reference; the local NBT itself has no idea where it lives.
_Avoid_: handle, pointer, target, source.

**Item reference**:
An NBT reference to an item, distinguished by where the item sits: the player's hand, an
inventory slot, an open container, a client-chest page, or the server copy. The kind of item
reference determines what saving means and whether it can happen at all.
_Avoid_: item handle, slot reference.

**Tag reference**:
A named, typed view onto one piece of NBT, with a getter and a setter. Enchantments, attributes
and potion contents are each a tag reference; the editor screens are built out of them rather
than out of raw tag paths.
_Avoid_: accessor, field, binding, property.

### The client chest

**Client chest**:
Local item storage the player owns, independent of any world or server, spread over numbered
pages and persisted on disk.
_Avoid_: ender chest, vault, local inventory.

**Page**:
One 54-slot unit of the client chest, and the unit of storage, loading and locking. Pages are
numbered from 1 in the interface.
_Avoid_: tab, chest, screen, section.

**Page load level**:
How much of a page is currently in memory: nothing, its items, or its items with dynamic items
resolved. A page can be listed and named without being loaded at all.
_Avoid_: load state, page status.

**Dynamic items**:
The parts of a page's items that only mean something once the game's registries are available.
They are the reason a page has a load level rather than a loaded flag.

**Data version**:
Minecraft's own number for the serialization format of a release. Every page records the data
version it was written under.
_Avoid_: game version, release, schema version. The **release target** (`26.2`) is a different
thing and is not interchangeable with it.

**Data version status**:
What a page's recorded data version means to the client that is running now: unknown, outdated,
current, or written by a newer release. It decides whether a page can be read, upgraded, or only
left alone.
_Avoid_: compatibility, validity.

**Uncached processing**:
Work that goes straight at a page's file on disk under the page lock, without putting it through
the in-memory page cache. Bulk sweeps over every page are uncached; ordinary editing is not.
_Avoid_: direct IO, raw access, bypass.

### Containers and screens

**Container**:
Anything the editor can show as a grid of item slots. That is wider than vanilla's idea of a
container: a shulker box held as an item, a block entity's contents, a mob's equipment and a
bundle are all containers here.
_Avoid_: inventory, screen handler, menu.

**Container IO**:
The rule for turning one kind of container into slots and back again. Each container shape has
its own, because a chest item, a bundle component and a horse's armour each hide their contents
somewhere different.
_Avoid_: adapter, codec, serializer.

**NBT folder**:
A level of the tag tree as the editor presents it: the keys of a compound, the entries of a list,
or the characters of a string, each openable and editable. It is why editing NBT feels like
walking a directory tree.
_Avoid_: node, directory, branch.

**Fancy text**:
The mod's own markup for writing Minecraft formatted text, including hover and click behaviour,
in a single editable string.
_Avoid_: rich text, formatted text, styled text.

### The version layer

**Release target**:
The single Minecraft release this jar is built for. One jar, one release. See ADR-0001.
_Avoid_: supported versions, version range.

**Access-widener seam**:
The set of Minecraft members this mod is allowed to touch that vanilla keeps private, named once
in the widener file and reached from exactly one place in the code. See ADR-0003.
_Avoid_: hack, patch, reflection.
