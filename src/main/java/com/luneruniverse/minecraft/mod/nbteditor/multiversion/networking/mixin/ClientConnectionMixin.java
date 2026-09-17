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

import net.minecraft.client.multiplayer.ClientPacketListener;
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
	public abstract boolean isConnected();
	
	@Inject(method = "disconnect(Lnet/minecraft/network/chat/Component;)V", at = @At("HEAD"))
	private void disconnect(Component reason, CallbackInfo info) {
		if (isConnected()) {
			if (!NBTEditorServer.IS_DEDICATED && packetListener instanceof ClientPacketListener)
				MVClientNetworking.onPlayStop();
			if (packetListener instanceof ServerGamePacketListenerImpl handler)
				MVServerNetworking.onPlayStop(handler.player);
		}
	}
	
	@Inject(method = "genericsFtw", at = @At("HEAD"), cancellable = true)
	private static void genericsFtw(Packet<?> packet, PacketListener listener, CallbackInfo info) {
		if (!NBTEditorServer.IS_DEDICATED && listener instanceof ClientPacketListener && packet instanceof ClientboundCustomPayloadPacket customPacket) {
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
