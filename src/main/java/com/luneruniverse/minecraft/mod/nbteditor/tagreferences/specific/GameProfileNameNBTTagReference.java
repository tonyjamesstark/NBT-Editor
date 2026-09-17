package com.luneruniverse.minecraft.mod.nbteditor.tagreferences.specific;

import java.util.Optional;

import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.general.TagReference;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

public class GameProfileNameNBTTagReference implements TagReference<Optional<String>, CompoundTag> {
	
	@Override
	public Optional<String> get(CompoundTag object) {
		if (object.nbte$contains("SkullOwner", Tag.TAG_STRING))
			return Optional.of(object.nbte$getStringOrDefault("SkullOwner"));
		if (object.nbte$contains("SkullOwner", Tag.TAG_COMPOUND)) {
			CompoundTag skullOwner = object.nbte$getCompoundOrDefault("SkullOwner");
			if (skullOwner.nbte$contains("Name", Tag.TAG_STRING))
				return Optional.of(skullOwner.nbte$getStringOrDefault("Name"));
			return Optional.empty();
		}
		return Optional.empty();
	}
	
	@Override
	public void set(CompoundTag object, Optional<String> value) {
		value.ifPresentOrElse(name -> object.putString("SkullOwner", name), () -> object.remove("SkullOwner"));
	}
	
}
