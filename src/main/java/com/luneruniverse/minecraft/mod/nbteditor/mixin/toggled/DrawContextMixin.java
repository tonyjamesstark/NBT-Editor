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

import com.luneruniverse.minecraft.mod.nbteditor.misc.MixinLink;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVMatrix4f;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVMisc;
import com.luneruniverse.minecraft.mod.nbteditor.screens.ConfigScreen;
import com.luneruniverse.minecraft.mod.nbteditor.util.MainUtil;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.gui.tooltip.TooltipPositioner;
import net.minecraft.util.Identifier;

@Mixin(DrawContext.class)
public abstract class DrawContextMixin {
	
	@Shadow
	public abstract Matrix3x2fStack getMatrices();
	
	// 1.21.9 split tooltip drawing out of drawTooltip into drawTooltipImmediately,
	// and the matrix it pushes is the 2D GUI stack.
	@Inject(method = "drawTooltipImmediately", at = @At(value = "INVOKE",
			target = "Lorg/joml/Matrix3x2fStack;pushMatrix()Lorg/joml/Matrix3x2fStack;", shift = At.Shift.AFTER))
	private void drawTooltipImmediately(TextRenderer textRenderer, List<TooltipComponent> tooltip, int x, int y,
			TooltipPositioner positioner, Identifier texture, CallbackInfo info) {
		if (!ConfigScreen.isTooltipOverflowFix())
			return;
		
		int[] size = MixinLink.getTooltipSize(tooltip);
		Vector2ic pos = MVMisc.getPosition(positioner, MainUtil.client.currentScreen, x, y, size[0], size[1]);
		int screenWidth = MainUtil.client.getWindow().getScaledWidth();
		int screenHeight = MainUtil.client.getWindow().getScaledHeight();
		
		MixinLink.renderTooltipFromComponents((DrawContext) (Object) this,
				pos.x(), pos.y(), size[0], size[1], screenWidth, screenHeight);
	}
	
	@ModifyVariable(method = "scissorContains", at = @At("HEAD"), ordinal = 0, require = 0)
	private int scissorContainsX(int x) {
		return x + (int) MVMatrix4f.getTranslation(getMatrices())[0];
	}
	@ModifyVariable(method = "scissorContains", at = @At("HEAD"), ordinal = 1, require = 0)
	private int scissorContainsY(int y) {
		return y + (int) MVMatrix4f.getTranslation(getMatrices())[1];
	}
	
}
