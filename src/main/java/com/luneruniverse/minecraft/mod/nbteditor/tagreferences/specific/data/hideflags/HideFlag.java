package com.luneruniverse.minecraft.mod.nbteditor.tagreferences.specific.data.hideflags;


import net.minecraft.component.DataComponentTypes;
import net.minecraft.text.Text;

public abstract class HideFlag {
	
	public static final HideFlag TOOLTIP = TooltipHideFlag.INSTANCE;
	public static final HideFlag CONTAINER = TooltipDisplayComponentHideFlag.FLAGS.get(DataComponentTypes.CONTAINER);
	
	public abstract Text getName();
	
}
