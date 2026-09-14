package com.luneruniverse.minecraft.mod.nbteditor.multiversion.networking.mixin.toggled;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.networking.MVClientNetworking;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.networking.MVServerNetworking;
import com.luneruniverse.minecraft.mod.nbteditor.server.NBTEditorServer;
import com.luneruniverse.minecraft.mod.nbteditor.server.ServerMixinLink;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.PacketListener;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

@Mixin(Connection.class)
public class ClientConnectionMixin_1_20_5 {
	@Shadow
	private PacketFlow side;
	@Shadow
	private PacketListener packetListener;
	
	private PacketListener prevListener;
	
	@Inject(method = "setPacketListener", at = @At("HEAD"))
	private void setPacketListener_head(ProtocolInfo<?> state, PacketListener listener, CallbackInfo info) {
		prevListener = packetListener;
	}
	
	@Inject(method = "transitionInbound", at = @At("RETURN"))
	private void transitionInbound_return(ProtocolInfo<?> state, PacketListener listener, CallbackInfo info) {
		if (side == PacketFlow.CLIENTBOUND && !NBTEditorServer.IS_DEDICATED) {
			if (ServerMixinLink.isInstanceOfClientPlayNetworkHandlerSafely(listener))
				MVClientNetworking.onPlayStart((ClientPacketListener) listener);
			else if (ServerMixinLink.isInstanceOfClientPlayNetworkHandlerSafely(prevListener))
				MVClientNetworking.onPlayStop();
		}
		if (side == PacketFlow.SERVERBOUND) {
			if (listener instanceof ServerGamePacketListenerImpl handler)
				MVServerNetworking.onPlayStart(handler.player);
			else if (prevListener instanceof ServerGamePacketListenerImpl handler)
				MVServerNetworking.onPlayStop(handler.player);
		}
		prevListener = null;
	}
}
