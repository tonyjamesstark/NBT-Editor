package com.luneruniverse.minecraft.mod.nbteditor.multiversion;

import com.luneruniverse.minecraft.mod.nbteditor.screens.Tickable;
import com.luneruniverse.minecraft.mod.nbteditor.util.MainUtil;

import net.minecraft.client.gui.components.EditBox;

public class MVTextFieldWidget extends EditBox implements Tickable, MVElement {
	
	protected MVTooltip tooltip;
	
	public MVTextFieldWidget(int x, int y, int width, int height, EditBox copyFrom) {
		super(MainUtil.client.font, x, y, width, height, copyFrom, TextInst.of(""));
	}
	public MVTextFieldWidget(int x, int y, int width, int height) {
		super(MainUtil.client.font, x, y, width, height, TextInst.of(""));
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
