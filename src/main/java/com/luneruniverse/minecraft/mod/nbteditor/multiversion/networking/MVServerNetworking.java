package com.luneruniverse.minecraft.mod.nbteditor.multiversion.networking;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import com.luneruniverse.minecraft.mod.nbteditor.server.ServerMVMisc;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class MVServerNetworking {
	
	public static class PlayNetworkStateEvents {
		public static interface Start {
			public static final Event<Start> EVENT = EventFactory.createArrayBacked(Start.class, listeners -> player -> {
				for (Start listener : listeners)
					listener.onPlayStart(player);
			});
			public void onPlayStart(ServerPlayerEntity player);
		}
		public static interface Stop {
			public static final Event<Stop> EVENT = EventFactory.createArrayBacked(Stop.class, listeners -> player -> {
				for (Stop listener : listeners)
					listener.onPlayStop(player);
			});
			public void onPlayStop(ServerPlayerEntity player);
		}
	}
	
	public static void onPlayStart(ServerPlayerEntity player) {
		PlayNetworkStateEvents.Start.EVENT.invoker().onPlayStart(player);
	}
	public static void onPlayStop(ServerPlayerEntity player) {
		PlayNetworkStateEvents.Stop.EVENT.invoker().onPlayStop(player);
	}
	
	private static final Map<Identifier, List<BiConsumer<MVPacket, ServerPlayerEntity>>> listeners = new HashMap<>();
	
	@SuppressWarnings("deprecation")
	public static void send(ServerPlayerEntity player, MVPacket packet) {
		ServerMVMisc.sendS2CPacket(player, MVPacketCustomPayload.wrapS2C(packet));
	}
	
	@SuppressWarnings("unchecked")
	public static <T extends MVPacket> void registerListener(Identifier id, BiConsumer<T, ServerPlayerEntity> listener) {
		listeners.computeIfAbsent(id, key -> new ArrayList<>()).add((packet, player) -> listener.accept((T) packet, player));
	}
	
	public static void callListeners(MVPacket packet, ServerPlayerEntity player) {
		if (!player.getEntityWorld().getServer().isOnThread()) {
			player.getEntityWorld().getServer().execute(() -> callListeners(packet, player));
			return;
		}
		List<BiConsumer<MVPacket, ServerPlayerEntity>> specificListeners = listeners.get(packet.getPacketId());
		if (specificListeners == null)
			return;
		specificListeners.forEach(listener -> listener.accept(packet, player));
	}
	
}
