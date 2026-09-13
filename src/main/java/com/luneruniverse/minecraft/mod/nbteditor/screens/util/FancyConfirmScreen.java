package com.luneruniverse.minecraft.mod.nbteditor.screens.util;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.IgnoreCloseScreenPacket;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVDrawableHelper;
import com.luneruniverse.minecraft.mod.nbteditor.util.MainUtil;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class FancyConfirmScreen extends ConfirmScreen implements IgnoreCloseScreenPacket {
	
	private Screen parent;
	
	public FancyConfirmScreen(BooleanConsumer callback, Text title, Text message, Text yesTranslated, Text noTranslated) {
		super(callback, title, message, yesTranslated, noTranslated);
		parent = MainUtil.client.currentScreen;
	}
	public FancyConfirmScreen(BooleanConsumer callback, Text title, Text message) {
		super(callback, title, message);
		parent = MainUtil.client.currentScreen;
	}
	
	public FancyConfirmScreen setParent(Screen parent) {
		this.parent = parent;
		return this;
	}
	
	@Override
	protected void init() {
		if (parent != null)
			parent.init(client, width, height);
		super.init();
	}
	
	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		if (parent != null)
			parent.render(context, -314, -314, delta);
		
		context.createNewRootLayer();
		super.render(context, mouseX, mouseY, delta);
		MainUtil.renderLogo(context);
	}
	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
		if (MainUtil.client.world == null)
			super.renderBackground(context, mouseX, mouseY, delta);
		else
			renderInGameBackground(context);
	}
	
}
