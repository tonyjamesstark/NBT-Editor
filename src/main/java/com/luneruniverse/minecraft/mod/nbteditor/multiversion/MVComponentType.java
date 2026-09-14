package com.luneruniverse.minecraft.mod.nbteditor.multiversion;

import java.util.function.Supplier;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.nbt.manager.NBTManagers;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.JukeboxPlayable;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.component.SuspiciousStewEffects;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.item.AdventureModePredicate;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Unit;

public class MVComponentType<T> {
	
	public static final MVComponentType<ItemAttributeModifiers> ATTRIBUTE_MODIFIERS =
			new MVComponentType<>(() -> DataComponents.ATTRIBUTE_MODIFIERS);
	public static final MVComponentType<CustomData> BLOCK_ENTITY_DATA =
			new MVComponentType<>(() -> DataComponents.BLOCK_ENTITY_DATA);
	public static final MVComponentType<BlockItemStateProperties> BLOCK_STATE =
			new MVComponentType<>(() -> DataComponents.BLOCK_STATE);
	public static final MVComponentType<AdventureModePredicate> CAN_BREAK =
			new MVComponentType<>(() -> DataComponents.CAN_BREAK);
	public static final MVComponentType<AdventureModePredicate> CAN_PLACE_ON =
			new MVComponentType<>(() -> DataComponents.CAN_PLACE_ON);
	public static final MVComponentType<CustomData> CUSTOM_DATA =
			new MVComponentType<>(() -> DataComponents.CUSTOM_DATA);
	public static final MVComponentType<Component> CUSTOM_NAME =
			new MVComponentType<>(() -> DataComponents.CUSTOM_NAME);
	public static final MVComponentType<DyedItemColor> DYED_COLOR =
			new MVComponentType<>(() -> DataComponents.DYED_COLOR);
	public static final MVComponentType<ItemEnchantments> ENCHANTMENTS =
			new MVComponentType<>(() -> DataComponents.ENCHANTMENTS);
	public static final MVComponentType<CustomData> ENTITY_DATA =
			new MVComponentType<>(() -> DataComponents.ENTITY_DATA);
	public static final MVComponentType<Component> ITEM_NAME =
			new MVComponentType<>(() -> DataComponents.ITEM_NAME);
	public static final MVComponentType<ItemLore> LORE =
			new MVComponentType<>(() -> DataComponents.LORE);
	public static final MVComponentType<Integer> MAX_DAMAGE =
			new MVComponentType<>(() -> DataComponents.MAX_DAMAGE);
	public static final MVComponentType<Integer> MAX_STACK_SIZE =
			new MVComponentType<>(() -> DataComponents.MAX_STACK_SIZE);
	public static final MVComponentType<PotionContents> POTION_CONTENTS =
			new MVComponentType<>(() -> DataComponents.POTION_CONTENTS);
	public static final MVComponentType<ResolvableProfile> PROFILE =
			new MVComponentType<>(() -> DataComponents.PROFILE);
	public static final MVComponentType<ItemEnchantments> STORED_ENCHANTMENTS =
			new MVComponentType<>(() -> DataComponents.STORED_ENCHANTMENTS);
	public static final MVComponentType<SuspiciousStewEffects> SUSPICIOUS_STEW_EFFECTS =
			new MVComponentType<>(() -> DataComponents.SUSPICIOUS_STEW_EFFECTS);
	public static final MVComponentType<ArmorTrim> TRIM =
			new MVComponentType<>(() -> DataComponents.TRIM);
	public static final MVComponentType<Unit> UNBREAKABLE =
			new MVComponentType<>(() -> DataComponents.UNBREAKABLE);
	public static final MVComponentType<WritableBookContent> WRITABLE_BOOK_CONTENT =
			new MVComponentType<>(() -> DataComponents.WRITABLE_BOOK_CONTENT);
	public static final MVComponentType<WrittenBookContent> WRITTEN_BOOK_CONTENT =
			new MVComponentType<>(() -> DataComponents.WRITTEN_BOOK_CONTENT);
	public static final MVComponentType<JukeboxPlayable> JUKEBOX_PLAYABLE =
			new MVComponentType<>(() -> DataComponents.JUKEBOX_PLAYABLE);
	
	private final Object component;
	
	public MVComponentType(Supplier<Object> component) {
		this.component = (NBTManagers.COMPONENTS_EXIST ? component.get() : null);
	}
	
	public Object getInternalValue() {
		if (component == null)
			throw new IllegalStateException("Components aren't in this version!");
		return component;
	}
	
}
