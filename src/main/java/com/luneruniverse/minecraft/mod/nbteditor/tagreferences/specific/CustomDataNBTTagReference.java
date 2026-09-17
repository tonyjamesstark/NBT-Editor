package com.luneruniverse.minecraft.mod.nbteditor.tagreferences.specific;

import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.general.TagReference;

import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;

public class CustomDataNBTTagReference implements TagReference<CompoundTag, ItemStack> {
	
	@Override
	public CompoundTag get(ItemStack object) {
		CompoundTag nbt = object.nbte$getNbt();
		if (nbt == null)
			return new CompoundTag();
		return nbt;
	}
	
	@Override
	public void set(ItemStack object, CompoundTag value) {
		object.nbte$setNbt(value);
	}
	
}
