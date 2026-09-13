package com.luneruniverse.minecraft.mod.nbteditor.multiversion;

import com.luneruniverse.minecraft.mod.nbteditor.screens.Tickable;
import com.luneruniverse.minecraft.mod.nbteditor.util.MainUtil;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;

public class MVTextFieldWidget extends TextFieldWidget implements Tickable, MVElement {
	
	protected MVTooltip tooltip;
	
	public MVTextFieldWidget(int x, int y, int width, int height, TextFieldWidget copyFrom) {
		super(MainUtil.client.textRenderer, x, y, width, height, copyFrom, TextInst.of(""));
	}
	public MVTextFieldWidget(int x, int y, int width, int height) {
		super(MainUtil.client.textRenderer, x, y, width, height, TextInst.of(""));
	}
	
	public MVTextFieldWidget tooltip(MVTooltip tooltip) {
		this.tooltip = tooltip;
		setTooltip(tooltip == null ? null : tooltip.toNewTooltip());
		return this;
	}
	
	
	public void method_25352(DrawContext context, int mouseX, int mouseY) { // renderTooltip
		if (tooltip != null)
			tooltip.render(context, mouseX, mouseY);
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
