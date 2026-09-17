package com.luneruniverse.minecraft.mod.nbteditor.multiversion.nbt.manager.components;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.Attempt;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.DynamicRegistryManagerHolder;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.nbt.NbtViews;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.nbt.manager.NBTManager;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.nbt.CompoundTag;

public class ComponentBlockEntityNBTManager implements NBTManager<BlockEntity> {
	
	@Override
	public Attempt<CompoundTag> trySerialize(BlockEntity subject) {
		return new Attempt<>(subject.saveWithFullMetadata(DynamicRegistryManagerHolder.get()));
	}
	
	@Override
	public boolean hasNbt(BlockEntity subject) {
		return true;
	}
	@Override
	public CompoundTag getNbt(BlockEntity subject) {
		return subject.saveWithoutMetadata(DynamicRegistryManagerHolder.get());
	}
	@Override
	public CompoundTag getOrCreateNbt(BlockEntity subject) {
		return getNbt(subject);
	}
	@Override
	public void setNbt(BlockEntity subject, CompoundTag nbt) {
		NbtViews.read(nbt, subject::loadWithComponents);
	}
	
}
