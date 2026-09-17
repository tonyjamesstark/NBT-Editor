package com.luneruniverse.minecraft.mod.nbteditor.tagreferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVComponentType;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVMisc;
import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.general.ComponentTagReference;
import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.general.TagReference;
import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.specific.EnchantsTagReference;
import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.specific.data.AttributeData;
import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.specific.data.CustomPotionContents;
import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.specific.data.Enchants;
import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.specific.data.hideflags.HideFlag;
import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.specific.hideflags.HideFlagsTooltipDisplayComponentTagReference;
import com.mojang.authlib.GameProfile;

import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.network.Filterable;
import net.minecraft.network.chat.Component;

public class ItemTagReferences {
	
	private static TagReference<CompoundTag, ItemStack> getComponentTagRefOfNBT(MVComponentType<CustomData> component) {
		return new ComponentTagReference<>(component,
				null,
				componentValue -> componentValue == null ? new CompoundTag() : componentValue.copyTag(),
				CustomData::of);
	}
	
	public static final TagReference<CustomPotionContents, ItemStack> CUSTOM_POTION_CONTENTS = (new ComponentTagReference<>(MVComponentType.POTION_CONTENTS,
					() -> MVMisc.newPotionContentsComponent(Optional.empty(), Optional.empty(), List.of()),
					contents -> new CustomPotionContents(contents.customColor(), contents.customEffects()),
					contents -> MVMisc.newPotionContentsComponent(Optional.empty(), contents.color(), contents.effects())));
	
	public static final TagReference<Optional<String>, ItemStack> PROFILE_NAME = (new ComponentTagReference<>(MVComponentType.PROFILE,
					null,
					component -> component == null ? Optional.empty() : component.name(),
					name -> name.map(ResolvableProfile::createUnresolved).orElse(null)));
	public static final TagReference<Optional<GameProfile>, ItemStack> PROFILE = (new ComponentTagReference<>(MVComponentType.PROFILE,
					null,
					profile -> Optional.ofNullable(profile).map(ResolvableProfile::partialProfile),
					profile -> profile.map(ResolvableProfile::createResolved).orElse(null)));
	
	public static final TagReference<List<AttributeData>, ItemStack> ATTRIBUTES = (new ComponentTagReference<>(MVComponentType.ATTRIBUTE_MODIFIERS,
					null,
					component -> component == null ? new ArrayList<>() :
						component.modifiers().stream().map(AttributeData::fromComponentEntry).collect(Collectors.toList()),
					(component, list) -> (ItemAttributeModifiers) MVMisc.withAttributes(component,
							list.stream().map(AttributeData::toComponentEntry).toList())));
	
	public static final TagReference<List<String>, ItemStack> WRITABLE_BOOK_PAGES = (new ComponentTagReference<>(MVComponentType.WRITABLE_BOOK_CONTENT,
					() -> new WritableBookContent(List.of()),
					content -> content.pages().stream().map(Filterable::raw).collect(Collectors.toList()),
					pages -> new WritableBookContent(pages.stream().map(Filterable::passThrough).toList())));
	
	public static final TagReference<Boolean, ItemStack> UNBREAKABLE = ComponentTagReference.forExistance(MVComponentType.UNBREAKABLE);
	
	public static final TagReference<CompoundTag, ItemStack> CUSTOM_DATA = getComponentTagRefOfNBT(MVComponentType.CUSTOM_DATA);
	
	public static final TagReference<Map<String, String>, ItemStack> BLOCK_STATE = (new ComponentTagReference<>(MVComponentType.BLOCK_STATE,
					null,
					component -> component == null ? new HashMap<>() : new HashMap<>(component.properties()),
					BlockItemStateProperties::new));
	
	public static final TagReference<CompoundTag, ItemStack> BLOCK_ENTITY_DATA = getComponentTagRefOfNBT(MVComponentType.BLOCK_ENTITY_DATA);
	
	public static final TagReference<CompoundTag, ItemStack> ENTITY_DATA = getComponentTagRefOfNBT(MVComponentType.ENTITY_DATA);
	
	public static final TagReference<Enchants, ItemStack> ENCHANTMENTS = new EnchantsTagReference();
	
	public static final TagReference<List<Component>, ItemStack> LORE = (new ComponentTagReference<>(MVComponentType.LORE,
					() -> ItemLore.EMPTY,
					component -> new ArrayList<>(component.lines()),
					lore -> new ItemLore(lore.stream().limit(256).toList())));
	
	public static final TagReference<Map<HideFlag, Boolean>, ItemStack> HIDE_FLAGS = new HideFlagsTooltipDisplayComponentTagReference();
	
}
