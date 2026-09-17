package com.luneruniverse.minecraft.mod.nbteditor.util;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.Version;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.serialization.Dynamic;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;

/**
 * Running NBT through Minecraft's data fixers, up to the version the game is currently on.
 *
 * <p>The editor reads nbt the player saved under an older game version -- a client chest page, a
 * preset file, a pasted snippet -- and has to hand the game something it still understands.
 * Vanilla does this behind {@code DataFixTypes}, which will not take a version that is not one of
 * its own constants; these do the same thing for a version read out of the data itself.
 */
public class DataFixes {
	
	// Based on DataFixTypes
	@SuppressWarnings("unchecked")
	public static <T extends Tag> T update(TypeReference typeRef, T nbt, int oldVersion) {
		return (T) Minecraft.getInstance().getFixerUpper().update(typeRef, new Dynamic<>(NbtOps.INSTANCE, nbt), oldVersion, Version.getDataVersion()).getValue();
	}
	/**
	 * If dataVersionTag is not null and a number, this updates from that - otherwise, this updates from defaultOldVersion
	 */
	public static <T extends Tag> T updateDynamic(TypeReference typeRef, T nbt, Tag dataVersionTag, int defaultOldVersion) {
		int dataVersion = defaultOldVersion;
		if (dataVersionTag != null && dataVersionTag instanceof NumericTag num)
			dataVersion = num.nbte$intValue();
		else if (dataVersion == -1)
			return nbt;
		return update(typeRef, nbt, dataVersion);
	}
	/**
	 * If a DataVersion tag exists, this updates from that - otherwise, this updates from defaultOldVersion
	 */
	public static CompoundTag updateDynamic(TypeReference typeRef, CompoundTag nbt, int defaultOldVersion) {
		return updateDynamic(typeRef, nbt, nbt.get("DataVersion"), defaultOldVersion);
	}
	/**
	 * If a DataVersion tag exists, this updates from that - otherwise, nbt is returned
	 */
	public static CompoundTag updateDynamic(TypeReference typeRef, CompoundTag nbt) {
		return updateDynamic(typeRef, nbt, -1);
	}
	
	private DataFixes() {}
	
}
