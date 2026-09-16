package com.luneruniverse.minecraft.mod.nbteditor.util;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.Version;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.serialization.Dynamic;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.NbtOps;

public class MainUtil {
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	public static Predicate<String> intPredicate(Supplier<Integer> min, Supplier<Integer> max, boolean allowEmpty) {
		return str -> {
			if (str.isEmpty())
				return allowEmpty;
			if (str.equals("+"))
				return allowEmpty && (max == null || max.get() >= 0);
			if (str.equals("-"))
				return allowEmpty && (min == null || min.get() <= 0);
			try {
				int value = Integer.parseInt(str);
				return (min == null || min.get() <= value) && (max == null || value <= max.get());
			} catch (NumberFormatException e) {
				return false;
			}
		};
	}
	public static Predicate<String> intPredicate(Integer min, Integer max, boolean allowEmpty) {
		return intPredicate(() -> min, () -> max, allowEmpty);
	}
	public static Predicate<String> intPredicate() {
		return intPredicate((Supplier<Integer>) null, null, true);
	}
	
	public static Integer parseOptionalInt(String str) {
		try {
			return Integer.parseInt(str);
		} catch (NumberFormatException e) {
			return null;
		}
	}
	public static int parseDefaultInt(String str, int defaultValue) {
		Integer output = parseOptionalInt(str);
		if (output == null)
			return defaultValue;
		return output;
	}
	
	
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
	
	
	
	public static <T> CompletableFuture<T> mergeFutures(List<CompletableFuture<T>> futures) {
		CompletableFuture<T> output = new CompletableFuture<>();
		output.thenAccept(value -> futures.forEach(future -> future.complete(value)));
		output.exceptionally(e -> {
			futures.forEach(future -> future.completeExceptionally(e));
			return null;
		});
		return output;
	}
	
	
	
}
