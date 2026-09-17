package com.luneruniverse.minecraft.mod.nbteditor.packets;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.IdentifierInst;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.networking.MVPacket;

import net.minecraft.world.item.ItemStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;

public class SetCursorC2SPacket implements MVPacket {
	
	public static final Identifier ID = IdentifierInst.of("nbteditor", "set_cursor");
	
	private final ItemStack item;
	
	public SetCursorC2SPacket(ItemStack item) {
		this.item = item;
	}
	public SetCursorC2SPacket(FriendlyByteBuf payload) {
		this.item = payload.readItemStack();
	}
	
	public ItemStack getItem() {
		return item;
	}
	
	@Override
	public void write(FriendlyByteBuf payload) {
		payload.writeItemStack(item);
	}
	
	@Override
	public Identifier getPacketId() {
		return ID;
	}
	
}
