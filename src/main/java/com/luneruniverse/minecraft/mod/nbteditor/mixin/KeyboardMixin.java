package com.luneruniverse.minecraft.mod.nbteditor.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.luneruniverse.minecraft.mod.nbteditor.screens.widgets.FormattedTextFieldWidget;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;

@Mixin(KeyboardHandler.class)
public class KeyboardMixin {
	@Redirect(method = "keyPress", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/GameNarrator;isActive()Z"))
	private boolean isActive(GameNarrator manager) {
		if (Minecraft.getInstance().gui.screen() != null) {
			GuiEventListener focused = Minecraft.getInstance().gui.screen().getFocused();
			while (focused != null) {
				if (focused instanceof FormattedTextFieldWidget)
					return false;
				else if (focused instanceof ContainerEventHandler parent)
					focused = parent.getFocused();
				else
					break;
			}
		}
		return manager.isActive();
	}
}
