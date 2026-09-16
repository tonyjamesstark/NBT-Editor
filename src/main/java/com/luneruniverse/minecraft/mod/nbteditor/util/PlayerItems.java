package com.luneruniverse.minecraft.mod.nbteditor.util;

import com.luneruniverse.minecraft.mod.nbteditor.NBTEditorClient;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Putting an item into the player's inventory and making it stick.
 *
 * <p>Every write here is two writes: the client's own inventory, and a creative set-slot packet
 * so the server agrees. Writing only the first leaves an item that vanishes on the next sync,
 * which is what an editor must never do.
 *
 * <p>Whether the server will accept the packet at all is
 * {@link NBTEditorClient#SERVER_CONN}'s answer, checked once here rather than at every caller.
 */
public class PlayerItems {
	
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
	
	private PlayerItems() {}
	
}
