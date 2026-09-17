package com.luneruniverse.minecraft.mod.nbteditor.mixin.toggled;

import java.util.List;

import org.joml.Matrix3x2fStack;
import org.joml.Vector2ic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.luneruniverse.minecraft.mod.nbteditor.screens.ItemTooltips;
import com.luneruniverse.minecraft.mod.nbteditor.screens.ConfigScreen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.resources.Identifier;
import net.minecraft.client.Minecraft;

@Mixin(GuiGraphicsExtractor.class)
public abstract class DrawContextMixin {
	
	@Shadow
	public abstract Matrix3x2fStack pose();
	
	// 1.21.9 split tooltip drawing out of drawTooltip into renderTooltip,
	// and the matrix it pushes is the 2D GUI stack.
	@Inject(method = "tooltip", at = @At(value = "INVOKE",
			target = "Lorg/joml/Matrix3x2fStack;pushMatrix()Lorg/joml/Matrix3x2fStack;", shift = At.Shift.AFTER))
	private void renderTooltip(Font textRenderer, List<ClientTooltipComponent> tooltip, int x, int y,
			ClientTooltipPositioner positioner, Identifier texture, CallbackInfo info) {
		if (!ConfigScreen.isTooltipOverflowFix())
			return;
		
		int[] size = ItemTooltips.getTooltipSize(tooltip);
		int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
		int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
		Vector2ic pos = positioner.positionTooltip(screenWidth, screenHeight, x, y, size[0], size[1]);
		
		ItemTooltips.renderTooltipFromComponents((GuiGraphicsExtractor) (Object) this,
				pos.x(), pos.y(), size[0], size[1], screenWidth, screenHeight);
	}
	
	@ModifyVariable(method = "containsPointInScissor", at = @At("HEAD"), ordinal = 0, require = 0)
	private int scissorContainsX(int x) {
		return x + (int) pose().m20();
	}
	@ModifyVariable(method = "containsPointInScissor", at = @At("HEAD"), ordinal = 1, require = 0)
	private int scissorContainsY(int y) {
		return y + (int) pose().m21();
	}
	
}
