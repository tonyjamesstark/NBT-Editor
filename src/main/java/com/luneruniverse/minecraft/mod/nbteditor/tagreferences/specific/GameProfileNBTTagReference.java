package com.luneruniverse.minecraft.mod.nbteditor.tagreferences.specific;

import java.lang.invoke.MethodType;
import java.util.Optional;
import java.util.UUID;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.Reflection;
import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.general.TagReference;
import com.mojang.authlib.GameProfile;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

public class GameProfileNBTTagReference implements TagReference<Optional<GameProfile>, CompoundTag> {
	
	private static final Class<?> NbtHelper = Reflection.getClass("net.minecraft.class_2512");
	
	private static final Reflection.MethodInvoker NbtHelper_toGameProfile =
			Reflection.getMethod(NbtHelper, "method_10683", MethodType.methodType(GameProfile.class, CompoundTag.class));
	@Override
	public Optional<GameProfile> get(CompoundTag object) {
		if (object.nbte$contains("SkullOwner", Tag.TAG_STRING))
			return Optional.of(new GameProfile(new UUID(0L, 0L), object.nbte$getStringOrDefault("SkullOwner")));
		if (object.nbte$contains("SkullOwner", Tag.TAG_COMPOUND))
			return Optional.ofNullable(NbtHelper_toGameProfile.invoke(null, object.nbte$getCompoundOrDefault("SkullOwner")));
		return Optional.empty();
	}
	
	private static final Reflection.MethodInvoker NbtHelper_writeGameProfile =
			Reflection.getMethod(NbtHelper, "method_10684", MethodType.methodType(CompoundTag.class, CompoundTag.class, GameProfile.class));
	@Override
	public void set(CompoundTag object, Optional<GameProfile> value) {
		value.ifPresentOrElse(
				profile -> object.put("SkullOwner", NbtHelper_writeGameProfile.invoke(null, new CompoundTag(), value.get())),
				() -> object.remove("SkullOwner"));
	}
	
}
