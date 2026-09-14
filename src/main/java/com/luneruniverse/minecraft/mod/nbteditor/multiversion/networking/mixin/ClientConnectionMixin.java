package com.luneruniverse.minecraft.mod.nbteditor.multiversion.networking.mixin;


import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.networking.MVClientNetworking;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.networking.MVPacket;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.networking.MVPacketCustomPayload;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.networking.MVServerNetworking;
import com.luneruniverse.minecraft.mod.nbteditor.server.NBTEditorServer;
import com.luneruniverse.minecraft.mod.nbteditor.server.ServerMixinLink;

import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.network.chat.Component;

@SuppressWarnings("deprecation")
@Mixin(Connection.class)
public abstract class ClientConnectionMixin {
	@Shadow
	private PacketListener packetListener;
	@Shadow
	public abstract boolean isOpen();
	
	@Inject(method = "disconnect", at = @At("HEAD"))
	private void disconnect(Component reason, CallbackInfo info) {
		if (isOpen()) {
			if (!NBTEditorServer.IS_DEDICATED && ServerMixinLink.isInstanceOfClientPlayNetworkHandlerSafely(packetListener))
				MVClientNetworking.onPlayStop();
			if (packetListener instanceof ServerGamePacketListenerImpl handler)
				MVServerNetworking.onPlayStop(handler.player);
		}
	}
	
	@Inject(method = "handlePacket", at = @At("HEAD"), cancellable = true)
	private static void handlePacket(Packet<?> packet, PacketListener listener, CallbackInfo info) {
		if (!NBTEditorServer.IS_DEDICATED && ServerMixinLink.isInstanceOfClientPlayNetworkHandlerSafely(listener) && packet instanceof ClientboundCustomPayloadPacket customPacket) {
			MVPacket mvPacket = MVPacketCustomPayload.unwrapS2C(customPacket);
			if (mvPacket != null) {
				MVClientNetworking.callListeners(mvPacket);
				info.cancel();
			}
		}
		if (listener instanceof ServerGamePacketListenerImpl handler && packet instanceof ServerboundCustomPayloadPacket customPacket) {
			MVPacket mvPacket = MVPacketCustomPayload.unwrapC2S(customPacket);
			if (mvPacket != null) {
				MVServerNetworking.callListeners(mvPacket, handler.player);
				info.cancel();
			}
		}
	}
}
