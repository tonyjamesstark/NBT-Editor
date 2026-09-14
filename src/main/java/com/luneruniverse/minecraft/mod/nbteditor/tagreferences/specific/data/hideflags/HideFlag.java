package com.luneruniverse.minecraft.mod.nbteditor.tagreferences.specific.data.hideflags;


import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;

public abstract class HideFlag {
	
	public static final HideFlag TOOLTIP = TooltipHideFlag.INSTANCE;
	public static final HideFlag CONTAINER = TooltipDisplayComponentHideFlag.FLAGS.get(DataComponents.CONTAINER);
	
	public abstract Component getName();
	
}
