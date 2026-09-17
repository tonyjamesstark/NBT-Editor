package com.luneruniverse.minecraft.mod.nbteditor.multiversion;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;

public class RegistryCache {
	
	private static final Map<RegistryAccess, RegistryCache> caches = Collections.synchronizedMap(new WeakHashMap<>());
	public static RegistryCache get(RegistryAccess registryManager) {
		return caches.computeIfAbsent(registryManager, key -> new RegistryCache(registryManager, false));
	}
	
	/**
	 * @return May be null
	 */
	public static <T> Holder.Reference<T> convertManagerWithCache(Holder.Reference<T> ref) {
		RegistryCache cache = get(DynamicRegistryManagerHolder.getManager());
		
		@SuppressWarnings("unchecked")
		Registry<T> registry = (Registry<T>) cache.getRegistry(ref.key().registry()).orElse(null);
		if (registry == null)
			return null;
		
		return registry.get(ref.key().identifier())
				.orElse(null);
	}
	
	private static final LoadingCache<Registry<?>, Boolean> staticRegistries = CacheBuilder.newBuilder().build(
			CacheLoader.from(registry -> {
				return (BuiltInRegistries.REGISTRY.get(registry.key().identifier()) != null);
			}));
	public static boolean isRegistryStatic(Registry<?> registry) {
		return staticRegistries.getUnchecked(registry);
	}
	
	private final WeakReference<RegistryAccess> registryManagerRef;
	@SuppressWarnings("unused") // Holds a strong reference
	private final RegistryAccess registryManager;
	private final Map<Identifier, Optional<? extends Registry<?>>> cache;
	
	public RegistryCache(RegistryAccess registryManager, boolean stronglyRef) {
		this.registryManagerRef = new WeakReference<>(registryManager);
		this.registryManager = (stronglyRef ? registryManager : null);
		this.cache = new ConcurrentHashMap<>();
	}
	public RegistryCache(RegistryAccess registryManager) {
		this(registryManager, true);
	}
	
	public Optional<? extends Registry<?>> getRegistry(Identifier registryKey) {
		return cache.computeIfAbsent(registryKey, id -> {
			RegistryAccess registryManager = registryManagerRef.get();
			if (registryManager == null)
				return Optional.empty();
			return registryManager.lookup(ResourceKey.createRegistryKey(id));
		});
	}
	
	@SuppressWarnings("unchecked")
	public <T> Optional<? extends Registry<T>> getRegistry(ResourceKey<Registry<T>> registryKey) {
		return (Optional<? extends Registry<T>>) getRegistry(registryKey.identifier());
	}
	
}
