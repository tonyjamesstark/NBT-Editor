package com.luneruniverse.minecraft.mod.nbteditor.multiversion.nbt.manager.components;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.Attempt;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.nbt.NbtViews;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.nbt.manager.NBTManager;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.nbt.CompoundTag;

public class ComponentEntityNBTManager implements NBTManager<Entity> {
	
	@Override
	public Attempt<CompoundTag> trySerialize(Entity subject) {
		CompoundTag nbt = getNbt(subject);
		nbt.putString("id", EntityType.getKey(subject.getType()).toString());
		return new Attempt<>(nbt);
	}
	
	@Override
	public boolean hasNbt(Entity subject) {
		return true;
	}
	@Override
	public CompoundTag getNbt(Entity subject) {
		return NbtViews.write(subject::saveWithoutId);
	}
	@Override
	public CompoundTag getOrCreateNbt(Entity subject) {
		return getNbt(subject);
	}
	@Override
	public void setNbt(Entity subject, CompoundTag nbt) {
		NbtViews.read(nbt, subject::load);
	}
	
}
