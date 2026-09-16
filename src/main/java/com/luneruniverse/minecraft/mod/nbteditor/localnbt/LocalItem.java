package com.luneruniverse.minecraft.mod.nbteditor.localnbt;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVComponentType;
import com.luneruniverse.minecraft.mod.nbteditor.util.MainUtil;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public abstract class LocalItem implements LocalNBT {
	
	/**
	 * The name the item has before anyone renames it: its item_name component if it carries one,
	 * otherwise the name of its type. {@link ItemStack#getHoverName} cannot answer this, because
	 * it returns the custom name when there is one.
	 */
	public static Component defaultNameOf(ItemStack item) {
		Component name = item.get(MVComponentType.ITEM_NAME);
		if (name != null)
			return name;
		return item.getItem().getName(item);
	}
	
	/**
	 * Copies a stack, air included. {@link ItemStack#copy} yields EMPTY for air and drops the
	 * count with it, and the editor has to be able to hold an empty slot that still knows how
	 * many of nothing are in it.
	 */
	public static ItemStack copyAirable(ItemStack item) {
		ItemStack output = item.transmuteCopy(item.getItem(), item.getCount());
		output.setPopTime(item.getPopTime());
		return output;
	}
	
	public abstract LocalItemStack toStack();
	public abstract LocalItemParts toParts();
	
	/**
	 * Will throw if called on a {@link LocalItemParts}
	 * @see #getReadableItem()
	 * @see #toItem()
	 */
	public abstract ItemStack getEditableItem();
	/**
	 * Will not throw if called on a {@link LocalItemParts}
	 * @see #getEditableItem()
	 * @see #toItem()
	 */
	public abstract ItemStack getReadableItem();
	
	public abstract Item getItemType();
	
	public abstract int getCount();
	public abstract void setCount(int count);
	
	public boolean receive() {
		ItemStack item = getReadableItem();
		if (item.isEmpty())
			return false;
		MainUtil.getWithMessage(item);
		return true;
	}
	
	@Override
	public boolean equals(Object nbt) {
		if (nbt instanceof LocalItem item)
			return ItemStack.matches(this.getReadableItem(), item.getReadableItem());
		return false;
	}
}
