package com.luneruniverse.minecraft.mod.nbteditor.util;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVRegistry;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import net.minecraft.core.Holder;
import net.minecraft.nbt.StringTagVisitor;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * The calls that only compile because {@code nbteditor.accesswidener} opens a member Minecraft
 * keeps to itself.
 *
 * <p>Collected here so the seam is visible from both sides. Every method below names a widener
 * line in its javadoc; delete that line and this file stops compiling, which is the point. A
 * Minecraft update that renames or removes one of those members breaks the build here rather
 * than somewhere in the middle of a screen.
 *
 * <p>Nothing else belongs in this class. A method that compiles against the public API is
 * ordinary mod code and goes in the package that owns the concept.
 */
public class AccessWidenedApi {
	
	/**
	 * Builds an enchantment component from a mutable map.
	 *
	 * <p>Widens {@code ItemEnchantments.<init>(Object2IntOpenHashMap)}. The public route is
	 * {@code ItemEnchantments.Mutable}, which copies; the editor rewrites the whole set at once.
	 */
	public static ItemEnchantments newEnchantments(Object2IntOpenHashMap<Holder<Enchantment>> enchantments) {
		return new ItemEnchantments(enchantments);
	}
	
	/**
	 * Overwrites the server's idea of what is on the cursor, so the client's next click is not
	 * rejected as desynced.
	 *
	 * <p>Widens {@code AbstractContainerMenu.remoteCarried}.
	 */
	public static void setPreviousCursorStack(AbstractContainerMenu handler, ItemStack item) {
		handler.remoteCarried.force(item);
	}
	
	/**
	 * Finds the boat item that spawns a given boat entity.
	 *
	 * <p>Widens {@code BoatItem.entityType}. There is no registry from entity type back to the
	 * item that places it, so this scans the item registry.
	 */
	public static Item getBoatItem(EntityType<?> entityType) {
		for (Item item : MVRegistry.ITEM) {
			if (item instanceof BoatItem boat && entityType == boat.entityType)
				return item;
		}
		throw new IllegalStateException("Unknown boat entity type: " + EntityType.getKey(entityType));
	}
	
	/**
	 * Whether an NBT key can be written unquoted.
	 *
	 * <p>Widens {@code StringTagVisitor.UNQUOTED_KEY_MATCH}, which is the pattern the vanilla
	 * SNBT writer itself consults. {@code true} and {@code false} match the pattern but would
	 * read back as booleans, so they are excluded.
	 */
	public static boolean isSimpleName(String name) {
		return (!name.equalsIgnoreCase("true") && !name.equalsIgnoreCase("false") &&
				StringTagVisitor.UNQUOTED_KEY_MATCH.matcher(name).matches());
	}
	
}
