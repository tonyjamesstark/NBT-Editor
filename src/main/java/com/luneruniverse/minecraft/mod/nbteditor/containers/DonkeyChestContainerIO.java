package com.luneruniverse.minecraft.mod.nbteditor.containers;

import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;

/**
 * The chest a donkey or llama carries, which is the entity's {@code Items} list addressed by
 * {@code Slot} key. Writing it also maintains the two entity fields the chest implies:
 * {@code ChestedHorse}, and a llama's {@code Strength}, which caps how many columns are shown.
 */
public class DonkeyChestContainerIO implements ContainerIO<CompoundTag> {
	
	private static final int SLOTS = 15;
	/** A llama shows {@code Strength} columns of three. */
	private static final int SLOTS_PER_COLUMN = 3;
	
	private final boolean llama;
	private final ContainerIO<CompoundTag> delegate;
	private final Identifier[] textures;
	
	public DonkeyChestContainerIO(boolean llama) {
		this.llama = llama;
		this.delegate = new SlotKeyNbtListContainerIO(SLOTS).forNbtCompoundItems();
		this.textures = new Identifier[SLOTS];
	}
	
	@Override
	public boolean isSupported(CompoundTag container) {
		return delegate.isSupported(container);
	}
	
	@Override
	public int getMaxSlots(CompoundTag container) {
		return SLOTS;
	}
	
	@Override
	public Identifier[] getTextures(CompoundTag container) {
		return textures;
	}
	
	@Override
	public ItemStack[] read(CompoundTag container) {
		return delegate.read(container);
	}
	
	@Override
	public int write(CompoundTag container, ItemStack[] contents) {
		delegate.write(container, contents);
		
		for (ItemStack item : contents) {
			if (item != null && !item.isEmpty()) {
				container.putBoolean("ChestedHorse", true);
				break;
			}
		}
		
		if (llama) {
			int columns = 1;
			for (int i = SLOTS_PER_COLUMN; i < contents.length; i++) {
				if (contents[i] != null && !contents[i].isEmpty())
					columns = (i / SLOTS_PER_COLUMN) + 1;
			}
			if (columns != 1 && container.nbte$getIntOrDefault("Strength") < columns)
				container.putInt("Strength", columns);
		}
		
		return SLOTS;
	}
	
	@Override
	public int getNumWritten(CompoundTag container, ItemStack[] contents) {
		return SLOTS;
	}
	
	@Override
	public int getWrittenSlotIndex(CompoundTag container, ItemStack[] contents, int slot) {
		return slot;
	}
	
}
