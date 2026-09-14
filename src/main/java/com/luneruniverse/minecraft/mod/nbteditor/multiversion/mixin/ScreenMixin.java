package com.luneruniverse.minecraft.mod.nbteditor.multiversion.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVScreen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;

@Mixin(Screen.class)
public class ScreenMixin {
	@Redirect(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;extractBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V"), require = 0)
	private void render_renderBackground(Screen screen, GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		if (!((Object) this instanceof MVScreen))
			screen.extractBackground(context, mouseX, mouseY, delta);
		// An MVScreen draws its own background, so vanilla's must not double up.
	}
}
