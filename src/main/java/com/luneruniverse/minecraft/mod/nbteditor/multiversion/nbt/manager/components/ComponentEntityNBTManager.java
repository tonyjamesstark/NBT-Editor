package com.luneruniverse.minecraft.mod.nbteditor.multiversion.nbt.manager.components;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.Attempt;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.nbt.NbtViews;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.nbt.manager.NBTManager;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;

public class ComponentEntityNBTManager implements NBTManager<Entity> {
	
	@Override
	public Attempt<NbtCompound> trySerialize(Entity subject) {
		NbtCompound nbt = getNbt(subject);
		nbt.putString("id", EntityType.getId(subject.getType()).toString());
		return new Attempt<>(nbt);
	}
	
	@Override
	public boolean hasNbt(Entity subject) {
		return true;
	}
	@Override
	public NbtCompound getNbt(Entity subject) {
		return NbtViews.write(subject::writeData);
	}
	@Override
	public NbtCompound getOrCreateNbt(Entity subject) {
		return getNbt(subject);
	}
	@Override
	public void setNbt(Entity subject, NbtCompound nbt) {
		NbtViews.read(nbt, subject::readData);
	}
	
}
