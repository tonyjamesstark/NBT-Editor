package com.luneruniverse.minecraft.mod.nbteditor.util;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.luneruniverse.minecraft.mod.nbteditor.NBTEditorClient;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.Version;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.serialization.Dynamic;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;

public class MainUtil {
	
	// Same as ClientPlayerInteractionManager#clickCreativeSlot, but without a feature flag check
	// Also includes survival bypass
	/**
	 * @param item
	 * @param slot Format: container
	 */
	public static void clickCreativeStack(ItemStack item, int slot) {
		if (NBTEditorClient.SERVER_CONN.isEditingAllowed())
			Minecraft.getInstance().getConnection().send(new ServerboundSetCreativeModeSlotPacket(slot, item.copy()));
	}
	public static void dropCreativeStack(ItemStack item) {
		if (NBTEditorClient.SERVER_CONN.isEditingAllowed() && !item.isEmpty())
			Minecraft.getInstance().getConnection().send(new ServerboundSetCreativeModeSlotPacket(-1, item.copy()));
	}
	
	public static void saveItem(InteractionHand hand, ItemStack item) {
		Minecraft.getInstance().player.setItemInHand(hand, item.copy());
		clickCreativeStack(item, hand == InteractionHand.OFF_HAND ? SlotUtil.createOffHandInContainer() :
			SlotUtil.createHotbarInContainer(Minecraft.getInstance().player.getInventory().selected));
	}
	public static void saveItem(EquipmentSlot slot, ItemStack item) {
		if (slot == EquipmentSlot.MAINHAND)
			saveItem(InteractionHand.MAIN_HAND, item);
		else if (slot == EquipmentSlot.OFFHAND)
			saveItem(InteractionHand.OFF_HAND, item);
		else {
			Minecraft.getInstance().player.setItemSlot(slot, item.copy());
			clickCreativeStack(item, SlotUtil.createArmorInContainer(slot));
		}
	}
	
	/**
	 * @param slot Format: inv
	 * @param item
	 */
	public static void saveItem(int slot, ItemStack item) {
		Minecraft.getInstance().player.getInventory().setItem(slot, item.copy());
		clickCreativeStack(item, SlotUtil.invToContainer(slot));
	}
	
	public static void get(ItemStack item, boolean dropIfNoSpace) {
		Inventory inv = Minecraft.getInstance().player.getInventory();
		item = item.copy();
		
		int slot = inv.getSlotWithRemainingSpace(item);
		if (slot == -1)
			slot = inv.getFreeSlot();
		if (slot == -1) {
			if (dropIfNoSpace) {
				if (item.getCount() > item.getMaxStackSize())
					item.setCount(item.getMaxStackSize());
				dropCreativeStack(item);
			}
		} else {
			item.setCount(item.getCount() + inv.getItem(slot).getCount());
			int overflow = 0;
			if (item.getCount() > item.getMaxStackSize()) {
				overflow = item.getCount() - item.getMaxStackSize();
				item.setCount(item.getMaxStackSize());
			}
			saveItem(slot, item);
			if (overflow != 0) {
				item = item.copy();
				item.setCount(overflow);
				get(item, false);
			}
		}
	}
	public static void getWithMessage(ItemStack item) {
		get(item, true);
		Minecraft.getInstance().player.sendSystemMessage(Component.translatableEscape("nbteditor.get.item").append(item.getDisplayName()));
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	public static Predicate<String> intPredicate(Supplier<Integer> min, Supplier<Integer> max, boolean allowEmpty) {
		return str -> {
			if (str.isEmpty())
				return allowEmpty;
			if (str.equals("+"))
				return allowEmpty && (max == null || max.get() >= 0);
			if (str.equals("-"))
				return allowEmpty && (min == null || min.get() <= 0);
			try {
				int value = Integer.parseInt(str);
				return (min == null || min.get() <= value) && (max == null || value <= max.get());
			} catch (NumberFormatException e) {
				return false;
			}
		};
	}
	public static Predicate<String> intPredicate(Integer min, Integer max, boolean allowEmpty) {
		return intPredicate(() -> min, () -> max, allowEmpty);
	}
	public static Predicate<String> intPredicate() {
		return intPredicate((Supplier<Integer>) null, null, true);
	}
	
	public static Integer parseOptionalInt(String str) {
		try {
			return Integer.parseInt(str);
		} catch (NumberFormatException e) {
			return null;
		}
	}
	public static int parseDefaultInt(String str, int defaultValue) {
		Integer output = parseOptionalInt(str);
		if (output == null)
			return defaultValue;
		return output;
	}
	
	
	// Based on DataFixTypes
	@SuppressWarnings("unchecked")
	public static <T extends Tag> T update(TypeReference typeRef, T nbt, int oldVersion) {
		return (T) Minecraft.getInstance().getFixerUpper().update(typeRef, new Dynamic<>(NbtOps.INSTANCE, nbt), oldVersion, Version.getDataVersion()).getValue();
	}
	/**
	 * If dataVersionTag is not null and a number, this updates from that - otherwise, this updates from defaultOldVersion
	 */
	public static <T extends Tag> T updateDynamic(TypeReference typeRef, T nbt, Tag dataVersionTag, int defaultOldVersion) {
		int dataVersion = defaultOldVersion;
		if (dataVersionTag != null && dataVersionTag instanceof NumericTag num)
			dataVersion = num.nbte$intValue();
		else if (dataVersion == -1)
			return nbt;
		return update(typeRef, nbt, dataVersion);
	}
	/**
	 * If a DataVersion tag exists, this updates from that - otherwise, this updates from defaultOldVersion
	 */
	public static CompoundTag updateDynamic(TypeReference typeRef, CompoundTag nbt, int defaultOldVersion) {
		return updateDynamic(typeRef, nbt, nbt.get("DataVersion"), defaultOldVersion);
	}
	/**
	 * If a DataVersion tag exists, this updates from that - otherwise, nbt is returned
	 */
	public static CompoundTag updateDynamic(TypeReference typeRef, CompoundTag nbt) {
		return updateDynamic(typeRef, nbt, -1);
	}
	
	
	
	public static <T> CompletableFuture<T> mergeFutures(List<CompletableFuture<T>> futures) {
		CompletableFuture<T> output = new CompletableFuture<>();
		output.thenAccept(value -> futures.forEach(future -> future.complete(value)));
		output.exceptionally(e -> {
			futures.forEach(future -> future.completeExceptionally(e));
			return null;
		});
		return output;
	}
	
	
	
}
