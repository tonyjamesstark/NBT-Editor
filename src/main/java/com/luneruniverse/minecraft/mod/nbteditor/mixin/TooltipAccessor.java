package com.luneruniverse.minecraft.mod.nbteditor.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.util.FormattedCharSequence;

@Mixin(Tooltip.class)
public interface TooltipAccessor {
	@Accessor
	public void setCachedTooltip(List<FormattedCharSequence> cachedTooltip);
}
