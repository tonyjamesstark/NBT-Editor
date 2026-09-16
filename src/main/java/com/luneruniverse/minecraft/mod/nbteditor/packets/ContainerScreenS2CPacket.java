package com.luneruniverse.minecraft.mod.nbteditor.packets;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.networking.MVPacket;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;

public class ContainerScreenS2CPacket implements MVPacket {
	
	public static final Identifier ID = Identifier.fromNamespaceAndPath("nbteditor", "container_screen");
	
	public ContainerScreenS2CPacket() {}
	public ContainerScreenS2CPacket(FriendlyByteBuf payload) {}
	
	@Override
	public void write(FriendlyByteBuf payload) {}
	
	@Override
	public Identifier getPacketId() {
		return ID;
	}
	
}
