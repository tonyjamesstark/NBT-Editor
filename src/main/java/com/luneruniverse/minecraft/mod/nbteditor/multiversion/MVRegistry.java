package com.luneruniverse.minecraft.mod.nbteditor.multiversion;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.world.level.block.Block;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;

/**
 * A registry handle with a stable shape across game versions. Every lookup is a
 * direct call now; the reflective indirection this used to carry only existed to
 * span versions below 1.21.11.
 */
public class MVRegistry<T> implements Iterable<T> {

	public static final MVRegistry<Item> ITEM = new MVRegistry<>(BuiltInRegistries.ITEM);
	public static final MVRegistry<Block> BLOCK = new MVRegistry<>(BuiltInRegistries.BLOCK);
	public static final MVRegistry<EntityType<?>> ENTITY_TYPE = new MVRegistry<>(BuiltInRegistries.ENTITY_TYPE);
	public static final MVRegistry<Attribute> ATTRIBUTE = new MVRegistry<>(BuiltInRegistries.ATTRIBUTE);
	public static final MVRegistry<Potion> POTION = new MVRegistry<>(BuiltInRegistries.POTION);
	public static final MVRegistry<MobEffect> STATUS_EFFECT = new MVRegistry<>(BuiltInRegistries.MOB_EFFECT);

	/** Enchantments are data pack driven, so the registry changes with the world. */
	private static MVRegistry<Enchantment> ENCHANTMENT;
	public static MVRegistry<Enchantment> getEnchantmentRegistry() {
		Registry<Enchantment> registry = DynamicRegistryManagerHolder.getManager().lookupOrThrow(Registries.ENCHANTMENT);
		if (ENCHANTMENT == null || ENCHANTMENT.value != registry)
			ENCHANTMENT = new MVRegistry<>(registry);
		return ENCHANTMENT;
	}

	private static MVRegistry<DataComponentType<?>> COMPONENTS;
	public static MVRegistry<DataComponentType<?>> getComponentsRegistry() {
		if (COMPONENTS == null)
			COMPONENTS = new MVRegistry<>(BuiltInRegistries.DATA_COMPONENT_TYPE);
		return COMPONENTS;
	}


	private final Registry<T> value;

	private MVRegistry(Registry<T> value) {
		this.value = value;
	}

	public Registry<T> getInternalValue() {
		return value;
	}

	@Override
	public Iterator<T> iterator() {
		return value.iterator();
	}

	public Optional<T> getOrEmpty(Identifier id) {
		return value.getOptional(id);
	}

	public Identifier getId(T entry) {
		return value.getKey(entry);
	}

	public T get(Identifier id) {
		return value.getValue(id);
	}

	public Set<Identifier> getIds() {
		return value.keySet();
	}

	public Set<Map.Entry<Identifier, T>> getEntrySet() {
		return value.entrySet().stream()
				.map(entry -> Map.entry(entry.getKey().identifier(), entry.getValue()))
				.collect(Collectors.toUnmodifiableSet());
	}

	public boolean containsId(Identifier id) {
		return value.containsKey(id);
	}

}
