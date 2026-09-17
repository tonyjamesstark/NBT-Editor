package com.luneruniverse.minecraft.mod.nbteditor.mixin.toggled;

import java.util.function.Function;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.luneruniverse.minecraft.mod.nbteditor.NBTEditorClient;
import com.luneruniverse.minecraft.mod.nbteditor.packets.ClickSlotC2SPacketParent;
import com.luneruniverse.minecraft.mod.nbteditor.screens.ConfigScreen;
import com.luneruniverse.minecraft.mod.nbteditor.server.NBTEditorServer;

import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;

@Mixin(ServerboundContainerClickPacket.class)
public class ClickSlotC2SPacketMixin_1_21_5 implements ClickSlotC2SPacketParent {
	private static final byte NO_SLOT_RESTRICTIONS_FLAG = 0b01000000;
	
	private boolean noSlotRestrictions;
	
	// The flag rides a spare bit of buttonNum, and vanilla reads that field for the button the
	// click actually used, so it has to be off everywhere but the wire. It is added back here,
	// in the getter the stream codec serializes through, rather than left in the field.
	@ModifyArg(method = "<clinit>", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/network/codec/StreamCodec;composite(Lnet/minecraft/network/codec/StreamCodec;Ljava/util/function/Function;Lnet/minecraft/network/codec/StreamCodec;Ljava/util/function/Function;Lnet/minecraft/network/codec/StreamCodec;Ljava/util/function/Function;Lnet/minecraft/network/codec/StreamCodec;Ljava/util/function/Function;Lnet/minecraft/network/codec/StreamCodec;Ljava/util/function/Function;Lnet/minecraft/network/codec/StreamCodec;Ljava/util/function/Function;Lnet/minecraft/network/codec/StreamCodec;Ljava/util/function/Function;Lcom/mojang/datafixers/util/Function7;)Lnet/minecraft/network/codec/StreamCodec;"),
			index = 7)
	private static Function<ServerboundContainerClickPacket, Byte> clinit$StreamCodec_composite(
			Function<ServerboundContainerClickPacket, Byte> getButtonNum) {
		return packet -> {
			byte buttonNum = getButtonNum.apply(packet);
			if (packet.isNoSlotRestrictions())
				return (byte) (buttonNum | NO_SLOT_RESTRICTIONS_FLAG);
			return buttonNum;
		};
	}
	
	@ModifyVariable(method = "<init>", at = @At("CTOR_HEAD"))
	private byte init(byte buttonNum) {
		// Receiving: the flag is whatever the sender set, and comes back out of the button.
		if (NBTEditorServer.isOnServerThread()) {
			if ((buttonNum & NO_SLOT_RESTRICTIONS_FLAG) == 0)
				return buttonNum;
			noSlotRestrictions = true;
			return (byte) (buttonNum & ~NO_SLOT_RESTRICTIONS_FLAG);
		}
		// Sending: the button is left alone and the codec adds the flag on the way out.
		if (ConfigScreen.isNoSlotRestrictions() && NBTEditorClient.SERVER_CONN.isEditingExpanded())
			noSlotRestrictions = true;
		return buttonNum;
	}
	
	@Override
	public boolean isNoSlotRestrictions() {
		return noSlotRestrictions;
	}
}
