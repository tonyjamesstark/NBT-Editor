package com.luneruniverse.minecraft.mod.nbteditor.multiversion;


import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.ItemTagReferences;
import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.specific.data.Enchants;

import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.ChatFormatting;

public class MVEnchantments {
	
	private static Enchantment getEnchantment(ResourceKey<Enchantment> key) {
		return MVRegistry.getEnchantmentRegistry().get(key.identifier());
	}
	
	public static final Enchantment LOYALTY = getEnchantment(Enchantments.LOYALTY);
	public static final Enchantment FIRE_ASPECT = getEnchantment(Enchantments.FIRE_ASPECT);
	
	public static boolean isCursed(Enchantment enchant) {
		return MVRegistry.getEnchantmentRegistry().getInternalValue().wrapAsHolder(enchant).is(EnchantmentTags.CURSE);
	}
	
	public static void addEnchantment(ItemStack item, Enchantment enchant, int level) {
		Enchants enchants = ItemTagReferences.ENCHANTMENTS.get(item);
		enchants.addEnchant(enchant, level);
		ItemTagReferences.ENCHANTMENTS.set(item, enchants);
	}
	
	public static Component getEnchantmentName(Enchantment enchant) {
		ChatFormatting color = (isCursed(enchant) ? ChatFormatting.RED : ChatFormatting.GRAY);
		MutableComponent output = enchant.description().copy();
		ComponentUtils.mergeStyles(output, Style.EMPTY.withColor(color));
		return output;
	}
	
}
