package com.luneruniverse.minecraft.mod.nbteditor.multiversion.networking.mixin.toggled;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.networking.MVClientNetworking;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.networking.MVServerNetworking;
import com.luneruniverse.minecraft.mod.nbteditor.server.NBTEditorServer;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.PacketListener;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

@Mixin(Connection.class)
public class ClientConnectionMixin_1_20_5 {
	@Shadow
	private PacketFlow receiving;
	@Shadow
	private PacketListener packetListener;
	
	private PacketListener prevListener;
	
	@Inject(method = "validateListener", at = @At("HEAD"))
	private void setPacketListener_head(ProtocolInfo<?> state, PacketListener listener, CallbackInfo info) {
		prevListener = packetListener;
	}
	
	@Inject(method = "setupInboundProtocol", at = @At("RETURN"))
	private void transitionInbound_return(ProtocolInfo<?> state, PacketListener listener, CallbackInfo info) {
		if (receiving == PacketFlow.CLIENTBOUND && !NBTEditorServer.IS_DEDICATED) {
			if (listener instanceof ClientPacketListener clientListener)
				MVClientNetworking.onPlayStart(clientListener);
			else if (prevListener instanceof ClientPacketListener)
				MVClientNetworking.onPlayStop();
		}
		if (receiving == PacketFlow.SERVERBOUND) {
			if (listener instanceof ServerGamePacketListenerImpl handler)
				MVServerNetworking.onPlayStart(handler.player);
			else if (prevListener instanceof ServerGamePacketListenerImpl handler)
				MVServerNetworking.onPlayStop(handler.player);
		}
		prevListener = null;
	}
}
