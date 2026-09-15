package com.luneruniverse.minecraft.mod.nbteditor.containers;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.nbt.manager.NBTManagers;

import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;

/**
 * A container whose slots are named keys of one compound, as a lectern's {@code Book} or a
 * jukebox's {@code RecordItem}.
 *
 * <p>Nothing here needs the key to already hold an item compound: {@link #read} yields an empty
 * slot for a key that holds anything else, and {@link #write} overwrites it. So every compound is
 * supported, including one an earlier edit left malformed -- which for an NBT editor is the point.
 */
public class KeysContainerIO implements ContainerIO<CompoundTag> {
	
	private final boolean removeWhenEmpty;
	private final String[] keys;
	private final Identifier[] textures;
	
	public KeysContainerIO(boolean removeWhenEmpty, String... keys) {
		this.removeWhenEmpty = removeWhenEmpty;
		this.keys = keys;
		this.textures = new Identifier[keys.length];
	}
	
	@Override
	public boolean isSupported(CompoundTag container) {
		return true;
	}
	
	@Override
	public int getMaxSlots(CompoundTag container) {
		return keys.length;
	}
	
	@Override
	public Identifier[] getTextures(CompoundTag container) {
		return textures;
	}
	
	@Override
	public ItemStack[] read(CompoundTag container) {
		ItemStack[] contents = new ItemStack[keys.length];
		for (int i = 0; i < keys.length; i++) {
			contents[i] = container.nbte$getCompound(keys[i])
					.map(itemNbt -> NBTManagers.ITEM.deserializeOrElse(itemNbt, ItemStack.EMPTY)).orElse(ItemStack.EMPTY);
		}
		return contents;
	}
	
	@Override
	public int write(CompoundTag container, ItemStack[] contents) {
		for (int i = 0; i < keys.length; i++) {
			ItemStack item = contents[i];
			if (ContainerIO.isEmpty(item)) {
				if (removeWhenEmpty) {
					container.remove(keys[i]);
					continue;
				} else {
					item = ItemStack.EMPTY;
				}
			}
			container.put(keys[i], item.nbte$serialize(true));
		}
		return keys.length;
	}
	
}
