package com.luneruniverse.minecraft.mod.nbteditor.multiversion;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public class MVRegistryKeys {
	
	private static final Class<?> REGISTRY_CLASS = Reflection.getClass("net.minecraft.class_2378");
	private static final Class<?> REGISTRY_KEYS_CLASS = Reflection.getClass("net.minecraft.class_7924");
	private static <T> ResourceKey<T> getRegistryKey(String oldName, String newName) {
		return Reflection.getField(REGISTRY_KEYS_CLASS,
				newName,
				"Lnet/minecraft/class_5321;").get(null);
	}
	
	public static final ResourceKey<Registry<Level>> WORLD = getRegistryKey("field_25298", "field_41223");
	
}
