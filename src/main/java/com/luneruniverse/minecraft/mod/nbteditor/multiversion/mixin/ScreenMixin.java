package com.luneruniverse.minecraft.mod.nbteditor.multiversion.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVScreen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;

@Mixin(Screen.class)
public class ScreenMixin {
	@Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;renderBackground(Lnet/minecraft/client/gui/DrawContext;IIF)V"), require = 0)
	private void render_renderBackground(Screen screen, DrawContext context, int mouseX, int mouseY, float delta) {
		if (!((Object) this instanceof MVScreen))
			screen.renderBackground(context, mouseX, mouseY, delta);
		// An MVScreen draws its own background, so vanilla's must not double up.
	}
}
