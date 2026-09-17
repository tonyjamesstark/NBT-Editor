package com.luneruniverse.minecraft.mod.nbteditor.multiversion;

import com.luneruniverse.minecraft.mod.nbteditor.screens.Tickable;

import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.Minecraft;

public class MVTextFieldWidget extends EditBox implements Tickable, MVElement {
	
	protected MVTooltip tooltip;
	
	public MVTextFieldWidget(int x, int y, int width, int height, EditBox copyFrom) {
		super(Minecraft.getInstance().font, x, y, width, height, copyFrom, Component.nullToEmpty(""));
	}
	public MVTextFieldWidget(int x, int y, int width, int height) {
		super(Minecraft.getInstance().font, x, y, width, height, Component.nullToEmpty(""));
	}
	
	public MVTextFieldWidget tooltip(MVTooltip tooltip) {
		this.tooltip = tooltip;
		setTooltip(tooltip == null ? null : tooltip.toNewTooltip());
		return this;
	}
	
	
	@Override
	@Deprecated
	public void setFocused(boolean focused) {
		setMultiFocused(focused);
	}
	@Override
	@Deprecated
	public boolean isFocused() {
		return isMultiFocused();
	}
	
}
