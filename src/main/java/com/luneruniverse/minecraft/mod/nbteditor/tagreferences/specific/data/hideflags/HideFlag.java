package com.luneruniverse.minecraft.mod.nbteditor.tagreferences.specific.data.hideflags;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.Version;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.text.Text;

public abstract class HideFlag {
	
	public static final HideFlag TOOLTIP = Version.<HideFlag>newSwitch()
			.range("1.20.5", null, () -> TooltipHideFlag.INSTANCE)
			.get();
	public static final HideFlag CONTAINER = Version.<HideFlag>newSwitch()
			.range("1.21.5", null, () -> TooltipDisplayComponentHideFlag.FLAGS.get(DataComponentTypes.CONTAINER))
			.get();
	
	public abstract Text getName();
	
}
