package com.luneruniverse.minecraft.mod.nbteditor.screens.util;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.IgnoreCloseScreenPacket;
import com.luneruniverse.minecraft.mod.nbteditor.util.MainUtil;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;

public class FancyConfirmScreen extends ConfirmScreen implements IgnoreCloseScreenPacket {
	
	private Screen parent;
	
	public FancyConfirmScreen(BooleanConsumer callback, Component title, Component message, Component yesTranslated, Component noTranslated) {
		super(callback, title, message, yesTranslated, noTranslated);
		parent = Minecraft.getInstance().gui.screen();
	}
	public FancyConfirmScreen(BooleanConsumer callback, Component title, Component message) {
		super(callback, title, message);
		parent = Minecraft.getInstance().gui.screen();
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
	public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		if (Minecraft.getInstance().level == null)
			super.extractBackground(context, mouseX, mouseY, delta);
		else
			extractTransparentBackground(context);
	}
	
}
