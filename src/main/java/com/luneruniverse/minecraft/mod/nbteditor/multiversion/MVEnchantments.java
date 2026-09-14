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
	
	public static final boolean DATA_PACK_ENCHANTMENTS = true;
	
	@SuppressWarnings("unchecked")
	private static Enchantment getEnchantment(String field) {
		Object output = Reflection.getField(Enchantments.class, field,
				DATA_PACK_ENCHANTMENTS ? "Lnet/minecraft/class_5321;" : "Lnet/minecraft/class_1887;").get(null);
		if (DATA_PACK_ENCHANTMENTS)
			return MVRegistry.getEnchantmentRegistry().get(((ResourceKey<Enchantment>) output).identifier());
		return (Enchantment) output;
	}
	
	public static final Enchantment LOYALTY = getEnchantment("field_9120");
	public static final Enchantment FIRE_ASPECT = getEnchantment("field_9124");
	
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
