package com.luneruniverse.minecraft.mod.nbteditor.screens;

import com.luneruniverse.minecraft.mod.nbteditor.util.MainUtil;

import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public class OverlayScreen extends OverlaySupportingScreen {
	
	public static <T extends Renderable & GuiEventListener & NarratableEntry> T setOverlayOrScreen(T overlay, double z, boolean restoreParent) {
		if (MainUtil.client.gui.screen() instanceof OverlaySupportingScreen screen)
			screen.setOverlay(overlay, z);
		else
			MainUtil.client.setScreenAndShow(new OverlayScreen(Component.nullToEmpty(overlay.getClass().getName()), overlay, z, restoreParent));
		return overlay;
	}
	public static <T extends Renderable & GuiEventListener & NarratableEntry> T setOverlayOrScreen(T overlay, boolean restoreParent) {
		return setOverlayOrScreen(overlay, 0, restoreParent);
	}
	
	private Screen parent;
	
	private <T extends Renderable & GuiEventListener & NarratableEntry> OverlayScreen(Component title, T widget, double z, boolean restoreParent) {
		super(title);
		setOverlay(widget, z);
		if (restoreParent)
			parent = MainUtil.client.gui.screen();
	}
	
	@Override
	public <T extends Renderable & GuiEventListener> T setOverlay(T overlay, double z) {
		if (overlay == null)
			MainUtil.client.setScreenAndShow(parent);
		else
			parent = null;
		return super.setOverlay(overlay, z);
	}
	@Override
	public <T extends Screen> T setOverlayScreen(T overlay, double z) {
		if (overlay == null)
			MainUtil.client.setScreenAndShow(parent);
		else
			parent = null;
		return super.setOverlayScreen(overlay, z);
	}
	
	@Override
	protected void init() {
		if (parent != null)
			parent.init(width, height);
		super.init();
	}
	
	@Override
	protected void renderMain(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		if (parent != null)
			parent.extractRenderState(context, -314, -314, delta);
	}
	
}
