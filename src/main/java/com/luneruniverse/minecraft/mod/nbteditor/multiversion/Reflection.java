package com.luneruniverse.minecraft.mod.nbteditor.multiversion;

import java.lang.reflect.Field;
import java.util.concurrent.ExecutionException;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.UncheckedExecutionException;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.MappingResolver;

/**
 * Intermediary-name lookups for the handful of members that have no Mojang name
 * (synthetics) or that a dedicated server cannot link against.
 */
public class Reflection {
	
	public static final MappingResolver mappings = FabricLoader.getInstance().getMappingResolver();
	
	
	private static final Cache<String, Class<?>> classCache = CacheBuilder.newBuilder().build();
	public static Class<?> getClass(String name) {
		try {
			return classCache.get(name, () -> Class.forName(mappings.mapClassName("intermediary", name)));
		} catch (ExecutionException | UncheckedExecutionException e) {
			throw new RuntimeException("Error getting class", e);
		}
	}
	
	
	public static class FieldReference {
		private final Field field;
		public FieldReference(Field field) {
			this.field = field;
		}
		public void set(Object obj, Object value) {
			try {
				field.set(obj, value);
			} catch (Exception e) {
				throw new RuntimeException("Error setting field", e);
			}
		}
		@SuppressWarnings("unchecked")
		public <T> T get(Object obj) {
			try {
				return (T) field.get(obj);
			} catch (Exception e) {
				throw new RuntimeException("Error getting field value", e);
			}
		}
	}
	
	private static String getFieldName(Class<?> clazz, String field, String descriptor) {
		return mappings.mapFieldName("intermediary", mappings.unmapClassName("intermediary", clazz.getName()), field, descriptor);
	}
	public static FieldReference getField(Class<?> clazz, String field, String descriptor) {
		try {
			Field fieldObj = clazz.getDeclaredField(getFieldName(clazz, field, descriptor));
			fieldObj.setAccessible(true);
			return new FieldReference(fieldObj);
		} catch (Exception e) {
			throw new RuntimeException("Error getting field", e);
		}
	}
	
}
