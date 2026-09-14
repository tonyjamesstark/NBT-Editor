package com.luneruniverse.minecraft.mod.nbteditor.screens.util;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.IgnoreCloseScreenPacket;
import com.luneruniverse.minecraft.mod.nbteditor.util.MainUtil;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class FancyConfirmScreen extends ConfirmScreen implements IgnoreCloseScreenPacket {
	
	private Screen parent;
	
	public FancyConfirmScreen(BooleanConsumer callback, Component title, Component message, Component yesTranslated, Component noTranslated) {
		super(callback, title, message, yesTranslated, noTranslated);
		parent = MainUtil.client.gui.screen();
	}
	public FancyConfirmScreen(BooleanConsumer callback, Component title, Component message) {
		super(callback, title, message);
		parent = MainUtil.client.gui.screen();
	}
	
	public FancyConfirmScreen setParent(Screen parent) {
		this.parent = parent;
		return this;
	}
	
	@Override
	protected void init() {
		if (parent != null)
			parent.init(width, height);
		super.init();
	}
	
	@Override
	public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		if (parent != null)
			parent.extractRenderState(context, -314, -314, delta);
		
		context.nextStratum();
		super.extractRenderState(context, mouseX, mouseY, delta);
		MainUtil.renderLogo(context);
	}
	@Override
	public void renderBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		if (MainUtil.client.level == null)
			super.renderBackground(context, mouseX, mouseY, delta);
		else
			renderTransparentBackground(context);
	}
	
}
