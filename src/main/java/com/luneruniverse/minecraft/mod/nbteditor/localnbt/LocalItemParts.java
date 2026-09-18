package com.luneruniverse.minecraft.mod.nbteditor.localnbt;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.luneruniverse.minecraft.mod.nbteditor.NBTEditor;
import com.luneruniverse.minecraft.mod.nbteditor.util.Drawing;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVRegistry;
import com.luneruniverse.minecraft.mod.nbteditor.util.TextUtil;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.nbt.manager.NBTManagers;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class LocalItemParts extends LocalItem {
	
	private Item item;
	private CompoundTag nbt;
	private int count;
	
	private ItemStack cachedItem;
	private CompoundTag cachedNbt;
	
	public LocalItemParts(ItemStack item) {
		this.item = item.getItem();
		this.nbt = item.nbte$getNbt();
		this.count = item.getCount();
		
		if (this.item == null)
			this.item = Items.AIR;
		
		this.cachedItem = LocalItem.copyAirable(item);
		this.cachedNbt = (this.nbt == null ? null : this.nbt.copy());
	}
	private LocalItemParts(LocalItemParts toCopy) {
		this.item = toCopy.item;
		this.nbt = (toCopy.nbt == null ? null : toCopy.nbt.copy());
		this.count = toCopy.count;
		this.cachedItem = LocalItem.copyAirable(toCopy.cachedItem);
		this.cachedNbt = (toCopy.cachedNbt == null ? null : toCopy.cachedNbt.copy());
	}
	
	@Override
	public LocalItemStack toStack() {
		return new LocalItemStack(getCachedItem());
	}
	@Override
	public LocalItemParts toParts() {
		return this;
	}
	
	private void setCachedItemCount() {
		cachedItem.setCount(Math.min(count, cachedItem.getMaxStackSize()));
	}
	private ItemStack getCachedItem() {
		if (cachedItem.getItem() == item && Objects.equals(cachedNbt, nbt)) {
			setCachedItemCount();
			return cachedItem;
		}
		
		ItemStack oldCachedItem = cachedItem;
		cachedItem = new ItemStack(item, 1);
		cachedNbt = (nbt == null ? null : nbt.copy());
		try {
			cachedItem.nbte$setNbt(cachedNbt);
		} catch (Exception e) {
			NBTEditor.LOGGER.warn("Error while updating item cache", e);
			cachedItem = oldCachedItem;
		}
		setCachedItemCount();
		return cachedItem;
	}
	@Override
	public ItemStack getEditableItem() {
		throw new UnsupportedOperationException("LocalItemParts's items cannot be edited directly!");
	}
	@Override
	public ItemStack getReadableItem() {
		return getCachedItem();
	}
	
	@Override
	public boolean isEmpty() {
		return item == Items.AIR || count <= 0;
	}
	@Override
	public boolean isEmpty(Identifier id) {
		return MVRegistry.ITEM.get(id) == Items.AIR;
	}
	
	private static final String[] CUSTOM_NAME_KEYS = {"minecraft:custom_name", ":custom_name", "custom_name"};
	
	@Override
	public Component getName() {
		return getCachedItem().getHoverName();
	}
	@Override
	public void setName(Component name) {
		// A component key has three spellings the game reads alike, and the one already in the
		// item is the one to keep writing; putting another would leave two names behind.
		if (name == null) {
			if (nbt != null) {
				for (String key : CUSTOM_NAME_KEYS)
					nbt.remove(key);
			}
		} else {
			CompoundTag nbt = getOrCreateNBT();
			Tag nameNbt = TextUtil.toMinecraft(name);
			for (String key : CUSTOM_NAME_KEYS) {
				if (nbt.contains(key)) {
					nbt.put(key, nameNbt);
					return;
				}
			}
			nbt.put(CUSTOM_NAME_KEYS[0], nameNbt);
		}
	}
	@Override
	public String getDefaultName() {
		return LocalItem.defaultNameOf(getCachedItem()).getString();
	}
	
	@Override
	public Item getItemType() {
		return item;
	}
	@Override
	public Identifier getId() {
		return MVRegistry.ITEM.getId(item);
	}
	@Override
	public void setId(Identifier id) {
		item = MVRegistry.ITEM.get(id);
	}
	@Override
	public Set<Identifier> getIdOptions() {
		return MVRegistry.ITEM.getIds();
	}
	
	@Override
	public int getCount() {
		return count;
	}
	@Override
	public void setCount(int count) {
		this.count = count;
	}
	
	@Override
	public CompoundTag getNBT() {
		return nbt;
	}
	@Override
	public void setNBT(CompoundTag nbt) {
		this.nbt = nbt;
	}
	@Override
	public CompoundTag getOrCreateNBT() {
		if (nbt == null)
			nbt = new CompoundTag();
		return nbt;
	}
	
	@Override
	public void renderIcon(GuiGraphicsExtractor context, int x, int y, float tickDelta) {
		Drawing.renderItem(context, getCachedItem(), x, y);
	}
	
	@Override
	public Optional<ItemStack> toItem(boolean cleanup) {
		return Optional.of(getCachedItem().copy());
	}
	@Override
	public CompoundTag serialize() {
		CompoundTag output = new CompoundTag();
		output.putString("id", getId().toString());
		output.put("components", nbt);
		output.putInt("count", count);
		output.putString("type", "item");
		return output;
	}
	@Override
	public Component toHoverableText() {
		return getCachedItem().getDisplayName();
	}
	
	@Override
	public LocalItemParts copy() {
		return new LocalItemParts(this);
	}
	@Override
	public boolean equals(Object nbt) {
		if (nbt instanceof LocalItemParts item)
			return this.item == item.item && Objects.equals(this.nbt, item.nbt) && this.count == item.count;
		return super.equals(nbt);
	}
	
}
