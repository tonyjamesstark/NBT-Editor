package com.luneruniverse.minecraft.mod.nbteditor.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.luneruniverse.minecraft.mod.nbteditor.NBTEditor;
import com.luneruniverse.minecraft.mod.nbteditor.NBTEditorClient;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.IgnoreCloseScreenPacket;
import com.luneruniverse.minecraft.mod.nbteditor.screens.containers.ClientHandledScreen;
import com.luneruniverse.minecraft.mod.nbteditor.screens.containers.ClientScreenHandler;

import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.client.Minecraft;

@Mixin(ClientPacketListener.class)
public class ClientPlayNetworkHandlerMixin {
	
	private static boolean updatingClientInventory;
	
	@Inject(method = "handleContainerContent", at = @At("HEAD"), cancellable = true)
	private void handleContainerContent(ClientboundContainerSetContentPacket packet, CallbackInfo info) {
		if (!Minecraft.getInstance().isSameThread() || updatingClientInventory)
			return;
		
		if (packet.containerId() == ClientScreenHandler.SYNC_ID) {
			NBTEditor.LOGGER.warn("Ignoring an inventory packet with a ClientHandledScreen sync id!");
			info.cancel();
			return;
		}
		
		if (NBTEditorClient.CURSOR_MANAGER.isBranched()) {
			info.cancel();
			
			try {
				updatingClientInventory = true;
				Minecraft.getInstance().player.containerMenu = NBTEditorClient.CURSOR_MANAGER.getCurrentRoot().getMenu();
				((ClientPacketListener) (Object) this).handleContainerContent(packet);
			} finally {
				updatingClientInventory = false;
				Minecraft.getInstance().player.containerMenu = NBTEditorClient.CURSOR_MANAGER.getCurrentBranch().getMenu();
			}
		}
	}
	
	@Inject(method = "handleContainerSetSlot", at = @At("HEAD"), cancellable = true)
	private void handleContainerSetSlot(ClientboundContainerSetSlotPacket packet, CallbackInfo info) {
		if (!Minecraft.getInstance().isSameThread() || updatingClientInventory)
			return;
		
		if (packet.getContainerId() == ClientScreenHandler.SYNC_ID) {
			NBTEditor.LOGGER.warn("Ignoring a slot update packet with a ClientHandledScreen sync id!");
			info.cancel();
			return;
		}
		
		if (NBTEditorClient.CURSOR_MANAGER.isBranched()) {
			info.cancel();
			
			if (packet.getContainerId() == -1) {
				if (!(NBTEditorClient.CURSOR_MANAGER.getCurrentRoot() instanceof CreativeModeInventoryScreen))
					Minecraft.getInstance().player.containerMenu.setCarried(packet.getItem());
				return;
			}
			
			try {
				updatingClientInventory = true;
				Minecraft.getInstance().player.containerMenu = NBTEditorClient.CURSOR_MANAGER.getCurrentRoot().getMenu();
				((ClientPacketListener) (Object) this).handleContainerSetSlot(packet);
			} finally {
				updatingClientInventory = false;
				Minecraft.getInstance().player.containerMenu = NBTEditorClient.CURSOR_MANAGER.getCurrentBranch().getMenu();
			}
		}
	}
	
	@Inject(method = "handleContainerContent", at = @At("RETURN"), cancellable = true)
	private void onInventory_return(ClientboundContainerSetContentPacket packet, CallbackInfo info) {
		if (Minecraft.getInstance().gui.screen() instanceof ClientHandledScreen clientHandledScreen)
			clientHandledScreen.getServerInventoryManager().onInventoryPacket(packet);
	}
	
	@Inject(method = "handleContainerSetSlot", at = @At("RETURN"), cancellable = true)
	private void onScreenHandlerSlotUpdate_return(ClientboundContainerSetSlotPacket packet, CallbackInfo info) {
		if (Minecraft.getInstance().gui.screen() instanceof ClientHandledScreen clientHandledScreen)
			clientHandledScreen.getServerInventoryManager().onScreenHandlerSlotUpdatePacket(packet);
	}
	
	@Inject(method = "handleContainerClose", at = @At("HEAD"), cancellable = true)
	private void handleContainerClose(ClientboundContainerClosePacket packet, CallbackInfo info) {
		if (!Minecraft.getInstance().isSameThread())
			return;
		
		if (packet.getContainerId() == ClientScreenHandler.SYNC_ID) {
			NBTEditor.LOGGER.warn("Ignoring a close screen packet with a ClientHandledScreen sync id!");
			info.cancel();
			return;
		}
		
		NBTEditorClient.CURSOR_MANAGER.onCloseScreenPacket();
		
		if (Minecraft.getInstance().gui.screen() instanceof IgnoreCloseScreenPacket)
			info.cancel();
	}
	
}
