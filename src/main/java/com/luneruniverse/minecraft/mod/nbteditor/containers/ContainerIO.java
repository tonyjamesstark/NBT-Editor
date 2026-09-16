package com.luneruniverse.minecraft.mod.nbteditor.containers;

import com.luneruniverse.minecraft.mod.nbteditor.localnbt.LocalNBT;
import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.ItemTagReferences;
import com.luneruniverse.minecraft.mod.nbteditor.util.MainUtil;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.BuiltInRegistries;

public interface ContainerIO<T> {
	public static final Identifier HELMET_TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "container/slot/helmet");
	public static final Identifier CHESTPLATE_TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "container/slot/chestplate");
	public static final Identifier LEGGINGS_TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "container/slot/leggings");
	public static final Identifier BOOTS_TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "container/slot/boots");
	public static final Identifier SADDLE_TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "container/slot/saddle");
	public static final Identifier HORSE_ARMOR_TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "container/slot/horse_armor");
	public static final Identifier LLAMA_ARMOR_TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "container/slot/llama_armor");
	public static final Identifier SWORD_TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "container/slot/sword");
	public static final Identifier SHIELD_TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "container/slot/shield");
	
	public static final Identifier BREWING_FUEL_TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "container/slot/brewing_fuel");
	public static final Identifier POTION_TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "container/slot/potion");
	
	public static ContainerIO<ItemStack> forItemStack(ContainerIO<CompoundTag> io) {
		return DelegateContainerIO.map(io, item -> {
			CompoundTag nbt = item.nbte$getNbt();
			if (nbt == null)
				return new CompoundTag();
			return nbt;
		}, (item, nbt) -> item.nbte$setNbt(nbt));
	}
	
	public static ContainerIO<ItemStack> forItemStackBlockEntityTag(ContainerIO<CompoundTag> io, String entityId) {
		return DelegateContainerIO.map(io,
				ItemTagReferences.BLOCK_ENTITY_DATA::get,
				(item, blockEntityNbt) -> ItemTagReferences.BLOCK_ENTITY_DATA.set(
						item, MainUtil.fillId(blockEntityNbt, entityId)));
	}
	public static ContainerIO<ItemStack> forItemStackBlockEntityTag(ContainerIO<CompoundTag> io, BlockEntityType<?> entityId) {
		return forItemStackBlockEntityTag(io, BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(entityId).toString());
	}
	
	public static ContainerIO<ItemStack> forItemStackEntityTag(ContainerIO<CompoundTag> io, String entityId) {
		return DelegateContainerIO.map(io,
				ItemTagReferences.ENTITY_DATA::get,
				(item, entityNbt) -> ItemTagReferences.ENTITY_DATA.set(
						item, MainUtil.fillId(entityNbt, entityId)));
	}
	public static ContainerIO<ItemStack> forItemStackEntityTag(ContainerIO<CompoundTag> io, EntityType<?> entityId) {
		return forItemStackEntityTag(io, EntityType.getKey(entityId).toString());
	}
	
	/**
	 * A slot holds nothing when it is null or when it is an empty stack. {@link #read} may produce
	 * either, so every caller that inspects a slot has to accept both.
	 */
	public static boolean isEmpty(ItemStack item) {
		return item == null || item.isEmpty();
	}
	
	/**
	 * {@link #getWrittenSlotIndex} for an io that compacts, meaning it drops empty slots rather
	 * than recording them, so each item shifts down by the number of gaps preceding it.
	 * @param contents
	 * @param slot
	 * @return The slot that the item contained within <code>slot</code> will end up in
	 */
	public static int getCompactedSlotIndex(ItemStack[] contents, int slot) {
		int index = slot;
		for (int i = 0; i < slot; i++) {
			if (isEmpty(contents[i]))
				index--;
		}
		return index;
	}
	
	public static <T extends LocalNBT> ContainerIO<T> forLocalNBT(ContainerIO<CompoundTag> io) {
		return DelegateContainerIO.map(io, item -> {
			CompoundTag nbt = item.getNBT();
			if (nbt == null)
				return new CompoundTag();
			return nbt;
		}, (item, nbt) -> item.setNBT(nbt));
	}
	
	/**
	 * @param container
	 * @return If false, none of the other methods can be called safely
	 */
	public boolean isSupported(T container);
	/**
	 * @param container
	 * @return The maximum number of items that may be contained
	 */
	public int getMaxSlots(T container);
	/**
	 * @param container
	 * @return The texture to render in each slot, or null if no texture should be rendered in that slot
	 */
	public Identifier[] getTextures(T container);
	/**
	 * @param container
	 * @return May contain null for an unset slot, can be modified without affecting container
	 */
	public ItemStack[] read(T container);
	/**
	 * <code>contents</code> may be longer than {@link #getMaxSlots}; the caller sizes it to the
	 * screen, not to the container. Ignore the excess. Writing it produces NBT that
	 * {@link #isSupported} rejects on the next read, which makes the container silently uneditable.
	 * @param container
	 * @param contents Can contain null, can be modified later without affecting container
	 * @return The same value {@link #getNumWritten} returns for these arguments
	 */
	public int write(T container, ItemStack[] contents);
	/**
	 * Equals {@link #getMaxSlots} unless this io compacts, meaning
	 * {@link #getWrittenSlotIndex} is not the identity. {@link ConcatContainerIO} uses this as both
	 * the contents-space stride and the slot-space offset of the next io, so the two only agree when
	 * this equals the number of slots occupied. A compacting io must therefore be last in a concat.
	 * <p>Defaults to {@link #getMaxSlots}, which is the answer for every io that does not compact.
	 * @param container
	 * @param contents
	 * @return The number of items in <code>contents</code> that will be written, including empty items
	 */
	public default int getNumWritten(T container, ItemStack[] contents) {
		return getMaxSlots(container);
	}
	/**
	 * Defaults to the identity, which is the answer for every io that does not compact. An io that
	 * does compact overrides this with {@link #getCompactedSlotIndex} and overrides
	 * {@link #getNumWritten} to match.
	 * @param container
	 * @param contents
	 * @param slot
	 * @return The slot that the item contained within <code>slot</code> will end up in after being written
	 */
	public default int getWrittenSlotIndex(T container, ItemStack[] contents, int slot) {
		return slot;
	}
	
	public default ContainerIO<T> withTextures(Identifier... textures) {
		return new DelegateContainerIO<>(this) {
			@Override
			public Identifier[] getTextures(T container) {
				return textures;
			}
		};
	}
}
