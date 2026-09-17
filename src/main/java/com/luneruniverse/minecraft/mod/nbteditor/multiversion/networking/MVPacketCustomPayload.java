package com.luneruniverse.minecraft.mod.nbteditor.multiversion.networking;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;

/**
 * Used internally in multiversion.networking; DO NOT USE
 */
@Deprecated
public class MVPacketCustomPayload implements CustomPacketPayload {
	
	/**
	 * Hides the {@link CustomPacketPayload} in {@link ServerboundCustomPayloadPacket#ServerboundCustomPayloadPacket(CustomPacketPayload)}
	 */
	public static ServerboundCustomPayloadPacket wrapC2S(MVPacket packet) {
		return new ServerboundCustomPayloadPacket(new MVPacketCustomPayload(packet));
	}
	/**
	 * Hides the {@link CustomPacketPayload} in {@link ClientboundCustomPayloadPacket#ClientboundCustomPayloadPacket(CustomPacketPayload)}
	 */
	public static ClientboundCustomPayloadPacket wrapS2C(MVPacket packet) {
		return new ClientboundCustomPayloadPacket(new MVPacketCustomPayload(packet));
	}
	
	/**
	 * Hides the {@link CustomPacketPayload} in {@link ServerboundCustomPayloadPacket#payload()}
	 */
	public static MVPacket unwrapC2S(ServerboundCustomPayloadPacket packet) {
		if (packet.payload() instanceof MVPacketCustomPayload mvPacket)
			return mvPacket.getPacket();
		return null;
	}
	/**
	 * Hides the {@link CustomPacketPayload} in {@link ClientboundCustomPayloadPacket#payload()}
	 */
	public static MVPacket unwrapS2C(ClientboundCustomPayloadPacket packet) {
		if (packet.payload() instanceof MVPacketCustomPayload mvPacket)
			return mvPacket.getPacket();
		return null;
	}
	
	private final MVPacket packet;
	
	public MVPacketCustomPayload(MVPacket packet) {
		this.packet = packet;
	}
	
	public MVPacket getPacket() {
		return packet;
	}
	
	@Override
	public Type<MVPacketCustomPayload> type() {
		return new Type<>(packet.getPacketId());
	}
	
}
