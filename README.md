# NBT-Editor
This is a mod built for editing items, blocks, and entities in Minecraft.

Download and more information: [https://modrinth.com/mod/nbt-editor](https://modrinth.com/mod/nbt-editor)

List of every feature: [https://github.com/mega12345mega/NBT-Editor/wiki/List-of-Every-Feature](https://github.com/mega12345mega/NBT-Editor/wiki/List-of-Every-Feature)

Discord: [https://discord.gg/PzeYTbEZjn](https://discord.gg/PzeYTbEZjn)

## About this fork

This is a fork of [mega12345mega/NBT-Editor](https://github.com/mega12345mega/NBT-Editor),
diverged at upstream v2.0.3.

It targets **Minecraft 26.2 only**. Upstream ships one jar spanning 1.17 through 1.21; that
multi-version layer has been removed here rather than extended, so this build will not load on
any version 2.0.x supported. That is why it is versioned 3.0.0.

Changes from upstream and from [Thorioum/NBT-Editor26.1.2](https://github.com/Thorioum/NBT-Editor26.1.2)
are reviewed commit by commit and taken only where the defect still reproduces at 26.2; a fix the
migration already made moot is skipped rather than replayed.

Credit for the mod and for nearly all of the behaviour here belongs to **mega12345mega**, with the
sibling 26.1.2 fork by **Thorioum**. Translations carried in from upstream are the work of
**DZultra** (de_de), **papa_louie09** (pl_pl), **Clexus** and **GTedd** (zh_cn, zh_tw) and
**Axolotl1000** (zh_tw).

Licensed under MPL-2.0, following upstream. The vendored `tsp.headdb` and
`multiversion.commands` trees carry their own licenses.
