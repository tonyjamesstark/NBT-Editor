package com.luneruniverse.minecraft.mod.nbteditor.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.luneruniverse.minecraft.mod.nbteditor.commands.get.GetLostItemCommand;
import com.luneruniverse.minecraft.mod.nbteditor.misc.MixinLink;

import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ClickType;

@Mixin(CreativeModeInventoryScreen.class)
public class CreativeInventoryScreenMixin {
	@Inject(method = "onMouseClick", at = @At(value = "HEAD"), cancellable = true)
	private void onMouseClick(Slot slot, int slotId, int button, ClickType actionType, CallbackInfo info) {
		MixinLink.onMouseClick((CreativeModeInventoryScreen) (Object) this, slot, slotId, button, actionType, info);
	}
	@Inject(method = "onMouseClick", at = @At(value = "RETURN"))
	private void onMouseClickReturn(Slot slot, int slotId, int button, ClickType actionType, CallbackInfo info) {
		ItemStack cursor = ((CreativeModeInventoryScreen) (Object) this).getMenu().getCarried();
		if (!cursor.isEmpty())
			GetLostItemCommand.addToHistory(cursor);
	}
	
	@Inject(method = "keyPressed", at = @At(value = "HEAD"), cancellable = true)
	private void keyPressed(KeyEvent input, CallbackInfoReturnable<Boolean> info) {
		MixinLink.keyPressed((CreativeModeInventoryScreen) (Object) this, input, info);
	}
}
