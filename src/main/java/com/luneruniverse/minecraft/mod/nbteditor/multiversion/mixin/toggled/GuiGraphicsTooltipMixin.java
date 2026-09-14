package com.luneruniverse.minecraft.mod.nbteditor.multiversion.mixin.toggled;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVTooltip;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.util.FormattedCharSequence;

/**
 * Collects the tooltip a widget wants drawn. 1.21.9 moved this off {@code Screen}
 * and onto {@link GuiGraphicsExtractor}, which is why the mixin no longer targets a screen.
 */
@Mixin(GuiGraphicsExtractor.class)
public class GuiGraphicsTooltipMixin {
	@Inject(method = "setTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Ljava/util/List;Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;IIZ)V",
			at = @At("HEAD"), cancellable = true)
	private void setTooltip(Font font, List<FormattedCharSequence> tooltip, ClientTooltipPositioner positioner,
			int x, int y, boolean focused, CallbackInfo info) {
		if (MVTooltip.setExternalOneTooltip(tooltip))
			info.cancel();
	}
}
