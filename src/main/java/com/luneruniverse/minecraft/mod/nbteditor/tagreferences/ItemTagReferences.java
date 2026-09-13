package com.luneruniverse.minecraft.mod.nbteditor.tagreferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVComponentType;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVMisc;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.Reflection;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.Version;
import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.general.ComponentTagReference;
import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.general.NBTTagReference;
import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.general.TagReference;
import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.specific.AttributesNBTTagReference;
import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.specific.CustomDataNBTTagReference;
import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.specific.CustomPotionContentsNBTTagReference;
import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.specific.EnchantsTagReference;
import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.specific.GameProfileNBTTagReference;
import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.specific.GameProfileNameNBTTagReference;
import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.specific.data.AttributeData;
import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.specific.data.CustomPotionContents;
import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.specific.data.Enchants;
import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.specific.data.hideflags.HideFlag;
import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.specific.hideflags.HideFlagsComponentsTagReference;
import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.specific.hideflags.HideFlagsNBTTagReference;
import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.specific.hideflags.HideFlagsTooltipDisplayComponentTagReference;
import com.mojang.authlib.GameProfile;

import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.BlockStateComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.component.type.WritableBookContentComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtString;
import net.minecraft.text.RawFilteredPair;
import net.minecraft.text.Text;

public class ItemTagReferences {
	
	private static TagReference<NbtCompound, ItemStack> getComponentTagRefOfNBT(MVComponentType<NbtComponent> component) {
		return new ComponentTagReference<>(component,
				null,
				componentValue -> componentValue == null ? new NbtCompound() : componentValue.copyNbt(),
				NbtComponent::of);
	}
	
	public static final TagReference<CustomPotionContents, ItemStack> CUSTOM_POTION_CONTENTS = Version.<TagReference<CustomPotionContents, ItemStack>>newSwitch()
			.range("1.20.5", null, () -> new ComponentTagReference<>(MVComponentType.POTION_CONTENTS,
					() -> MVMisc.newPotionContentsComponent(Optional.empty(), Optional.empty(), List.of()),
					contents -> new CustomPotionContents(contents.customColor(), contents.customEffects()),
					contents -> MVMisc.newPotionContentsComponent(Optional.empty(), contents.color(), contents.effects())))
			.get();
	
	public static final TagReference<Optional<String>, ItemStack> PROFILE_NAME = Version.<TagReference<Optional<String>, ItemStack>>newSwitch()
			.range("1.20.5", null, () -> new ComponentTagReference<>(MVComponentType.PROFILE,
					null,
					component -> component == null ? Optional.empty() : component.getName(),
					name -> name.map(ProfileComponent::ofDynamic).orElse(null)))
			.get();
	public static final TagReference<Optional<GameProfile>, ItemStack> PROFILE = Version.<TagReference<Optional<GameProfile>, ItemStack>>newSwitch()
			.range("1.20.5", null, () -> new ComponentTagReference<>(MVComponentType.PROFILE,
					null,
					profile -> Optional.ofNullable(profile).map(ProfileComponent::getGameProfile),
					profile -> profile.map(ProfileComponent::ofStatic).orElse(null)))
			.get();
	
	public static final TagReference<List<AttributeData>, ItemStack> ATTRIBUTES = Version.<TagReference<List<AttributeData>, ItemStack>>newSwitch()
			.range("1.20.5", null, () -> new ComponentTagReference<>(MVComponentType.ATTRIBUTE_MODIFIERS,
					null,
					component -> component == null ? new ArrayList<>() :
						component.modifiers().stream().map(AttributeData::fromComponentEntry).collect(Collectors.toList()),
					(component, list) -> (AttributeModifiersComponent) MVMisc.withAttributes(component,
							list.stream().map(AttributeData::toComponentEntry).toList())))
			.get();
	
	public static final TagReference<List<String>, ItemStack> WRITABLE_BOOK_PAGES = Version.<TagReference<List<String>, ItemStack>>newSwitch()
			.range("1.20.5", null, () -> new ComponentTagReference<>(MVComponentType.WRITABLE_BOOK_CONTENT,
					() -> new WritableBookContentComponent(List.of()),
					content -> content.pages().stream().map(RawFilteredPair::raw).collect(Collectors.toList()),
					pages -> new WritableBookContentComponent(pages.stream().map(RawFilteredPair::of).toList())))
			.get();
	
	public static final TagReference<Boolean, ItemStack> UNBREAKABLE = Version.<TagReference<Boolean, ItemStack>>newSwitch()
			.range("1.21.5", null, () -> ComponentTagReference.forExistance(MVComponentType.UNBREAKABLE_1_21_5))
			.get();
	
	public static final TagReference<NbtCompound, ItemStack> CUSTOM_DATA = Version.<TagReference<NbtCompound, ItemStack>>newSwitch()
			.range("1.20.5", null, () -> getComponentTagRefOfNBT(MVComponentType.CUSTOM_DATA))
			.get();
	
	public static final TagReference<Map<String, String>, ItemStack> BLOCK_STATE = Version.<TagReference<Map<String, String>, ItemStack>>newSwitch()
			.range("1.20.5", null, () -> new ComponentTagReference<>(MVComponentType.BLOCK_STATE,
					null,
					component -> component == null ? new HashMap<>() : new HashMap<>(component.properties()),
					BlockStateComponent::new))
			.get();
	
	public static final TagReference<NbtCompound, ItemStack> BLOCK_ENTITY_DATA = Version.<TagReference<NbtCompound, ItemStack>>newSwitch()
			.range("1.20.5", null, () -> getComponentTagRefOfNBT(MVComponentType.BLOCK_ENTITY_DATA))
			.get();
	
	public static final TagReference<NbtCompound, ItemStack> ENTITY_DATA = Version.<TagReference<NbtCompound, ItemStack>>newSwitch()
			.range("1.20.5", null, () -> getComponentTagRefOfNBT(MVComponentType.ENTITY_DATA))
			.get();
	
	public static final TagReference<Enchants, ItemStack> ENCHANTMENTS = new EnchantsTagReference();
	
	public static final TagReference<List<Text>, ItemStack> LORE = Version.<TagReference<List<Text>, ItemStack>>newSwitch()
			.range("1.20.5", null, () -> new ComponentTagReference<>(MVComponentType.LORE,
					() -> LoreComponent.DEFAULT,
					component -> new ArrayList<>(component.lines()),
					lore -> new LoreComponent(lore.stream().limit(256).toList())))
			.get();
	
	public static final TagReference<Map<HideFlag, Boolean>, ItemStack> HIDE_FLAGS = Version.<TagReference<Map<HideFlag, Boolean>, ItemStack>>newSwitch()
			.range("1.21.5", null, () -> new HideFlagsTooltipDisplayComponentTagReference())
			.get();
	
}
