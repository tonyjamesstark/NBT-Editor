package com.luneruniverse.minecraft.mod.nbteditor.containers;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.nbt.manager.NBTManagers;

import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;

public class ArmorHandsContainerIO implements ContainerIO<CompoundTag> {
	
	private static final Identifier[] TEXTURES = new Identifier[] {
			HELMET_TEXTURE, CHESTPLATE_TEXTURE, LEGGINGS_TEXTURE, BOOTS_TEXTURE, SWORD_TEXTURE, SHIELD_TEXTURE};
	
	@Override
	public boolean isSupported(CompoundTag container) {
		Tag armorItemsNbtElement = container.get("ArmorItems");
		if (armorItemsNbtElement != null) {
			if (!(armorItemsNbtElement instanceof ListTag armorItemsNbt) ||
					armorItemsNbt.size() > 4 ||
					armorItemsNbt.nbte$getHeldType().filter(
							heldType -> heldType == 0 || heldType == Tag.TAG_COMPOUND).isEmpty()) {
				return false;
			}
		}
		
		Tag handItemsNbtElement = container.get("HandItems");
		if (handItemsNbtElement != null) {
			if (!(handItemsNbtElement instanceof ListTag handItemsNbt) ||
					handItemsNbt.size() > 2 ||
					handItemsNbt.nbte$getHeldType().filter(
							heldType -> heldType == 0 || heldType == Tag.TAG_COMPOUND).isEmpty()) {
				return false;
			}
		}
		
		return true;
	}
	
	@Override
	public int getMaxSlots(CompoundTag container) {
		return 6;
	}
	
	@Override
	public Identifier[] getTextures(CompoundTag container) {
		return TEXTURES;
	}
	
	@Override
	public ItemStack[] read(CompoundTag container) {
		ItemStack[] items = new ItemStack[6];
		
		ListTag armorItemsNbt = container.nbte$getListOrDefault("ArmorItems", Tag.TAG_COMPOUND);
		for (int i = 0; i < armorItemsNbt.nbte$size() && i < 4; i++)
			items[3 - i] = NBTManagers.ITEM.deserializeOrElse((CompoundTag) armorItemsNbt.nbte$get(i), ItemStack.EMPTY);
		
		ListTag handItemsNbt = container.nbte$getListOrDefault("HandItems", Tag.TAG_COMPOUND);
		for (int i = 0; i < handItemsNbt.nbte$size() && i < 2; i++)
			items[4 + i] = NBTManagers.ITEM.deserializeOrElse((CompoundTag) handItemsNbt.nbte$get(i), ItemStack.EMPTY);
		
		return items;
	}
	
	@Override
	public int write(CompoundTag container, ItemStack[] contents) {
		ItemStack[] actualContents = new ItemStack[6];
		for (int i = 0; i < 6; i++) {
			ItemStack item = null;
			if (i < contents.length)
				item = contents[i];
			if (item == null)
				item = ItemStack.EMPTY;
			actualContents[i] = item;
		}
		
		ListTag armorItemsNbt = new ListTag();
		for (int i = 0; i < 4; i++)
			armorItemsNbt.add(actualContents[3 - i].nbte$serialize(true));
		container.put("ArmorItems", armorItemsNbt);
		
		ListTag handItemsNbt = new ListTag();
		for (int i = 0; i < 2; i++)
			handItemsNbt.add(actualContents[4 + i].nbte$serialize(true));
		container.put("HandItems", handItemsNbt);
		
		return 6;
	}
	
	@Override
	public int getNumWritten(CompoundTag container, ItemStack[] contents) {
		return 6;
	}
	
	@Override
	public int getWrittenSlotIndex(CompoundTag container, ItemStack[] contents, int slot) {
		return slot;
	}
	
}
