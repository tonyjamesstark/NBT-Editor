package com.luneruniverse.minecraft.mod.nbteditor.multiversion.nbt.manager.components;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.Attempt;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.DynamicRegistryManagerHolder;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.nbt.NbtViews;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.nbt.manager.NBTManager;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;

public class ComponentBlockEntityNBTManager implements NBTManager<BlockEntity> {
	
	@Override
	public Attempt<NbtCompound> trySerialize(BlockEntity subject) {
		return new Attempt<>(subject.createNbtWithIdentifyingData(DynamicRegistryManagerHolder.get()));
	}
	
	@Override
	public boolean hasNbt(BlockEntity subject) {
		return true;
	}
	@Override
	public NbtCompound getNbt(BlockEntity subject) {
		return subject.createNbt(DynamicRegistryManagerHolder.get());
	}
	@Override
	public NbtCompound getOrCreateNbt(BlockEntity subject) {
		return getNbt(subject);
	}
	@Override
	public void setNbt(BlockEntity subject, NbtCompound nbt) {
		NbtViews.read(nbt, subject::read);
	}
	
}
