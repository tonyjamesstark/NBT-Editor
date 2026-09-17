package com.luneruniverse.minecraft.mod.nbteditor.screens.containers;

import java.util.List;

import com.luneruniverse.minecraft.mod.nbteditor.util.SlotUtil;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerInventoryPacket;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.client.Minecraft;
import com.luneruniverse.minecraft.mod.nbteditor.util.PlayerItems;

public class ServerInventoryManager {
	
	private final Container serverInv;
	
	public ServerInventoryManager() {
		Container playerInv = Minecraft.getInstance().player.getInventory();
		serverInv = new SimpleContainer(playerInv.getContainerSize());
		for (int i = 0; i < serverInv.getContainerSize(); i++)
			serverInv.setItem(i, playerInv.getItem(i).copy());
	}
	
	private AbstractContainerMenu getScreenHandler(int syncId) {
		if (syncId == 0)
			return Minecraft.getInstance().player.inventoryMenu;
		if (syncId == Minecraft.getInstance().player.containerMenu.containerId)
			return Minecraft.getInstance().player.containerMenu;
		return null;
	}
	
	public void onSetPlayerInventoryPacket(ClientboundSetPlayerInventoryPacket packet) {
		serverInv.setItem(packet.slot(), packet.contents().copy());
	}
	
	public void onInventoryPacket(ClientboundContainerSetContentPacket packet) {
		AbstractContainerMenu handler = getScreenHandler(packet.containerId());
		if (handler == null)
			return;
		
		List<ItemStack> contents = packet.items();
		for (int i = 0; i < contents.size(); i++) {
			Slot slot = handler.getSlot(i);
			if (slot.container == Minecraft.getInstance().player.getInventory())
				serverInv.setItem(slot.getContainerSlot(), contents.get(i).copy());
		}
	}
	
	public void onScreenHandlerSlotUpdatePacket(ClientboundContainerSetSlotPacket packet) {
		if (packet.getContainerId() == -1)
			return;
		
		if (packet.getContainerId() == -2) {
			serverInv.setItem(packet.getSlot(), packet.getItem().copy());
			return;
		}
		
		AbstractContainerMenu handler = getScreenHandler(packet.getContainerId());
		if (handler == null)
			return;
		Slot slot = handler.getSlot(packet.getSlot());
		if (slot.container == Minecraft.getInstance().player.getInventory())
			serverInv.setItem(slot.getContainerSlot(), packet.getItem().copy());
	}
	
	public void updateServer() {
		Container playerInv = Minecraft.getInstance().player.getInventory();
		for (int i = 0; i < serverInv.getContainerSize(); i++) {
			ItemStack item = playerInv.getItem(i);
			if (!ItemStack.matches(item, serverInv.getItem(i))) {
				PlayerItems.clickCreativeStack(item, SlotUtil.invToContainer(i));
				serverInv.setItem(i, item.copy());
			}
		}
	}
	
}
