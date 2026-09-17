package com.luneruniverse.minecraft.mod.nbteditor.multiversion.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

@Mixin(AbstractContainerMenu.class)
public class ScreenHandlerMixin {
	// <= 1.17.1: patches item getting thrown & deleted when creative inventory is closed
	@Inject(method = "removed", at = @At("HEAD"), cancellable = true)
	private void close(Player player, CallbackInfo info) {
	}
}
