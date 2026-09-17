package com.luneruniverse.minecraft.mod.nbteditor.screens;

import com.luneruniverse.minecraft.mod.nbteditor.screens.widgets.InitializableOverlay;
import com.luneruniverse.minecraft.mod.nbteditor.util.MainUtil;

import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public class OverlaySupportingScreen extends TickableSupportingScreen {
	
	public static <T extends Renderable & GuiEventListener> T setOverlayStatic(T overlay, double z) {
		return ((OverlaySupportingScreen) MainUtil.client.gui.screen()).setOverlay(overlay, z);
	}
	public static <T extends Renderable & GuiEventListener> T setOverlayStatic(T overlay) {
		return setOverlayStatic(overlay, 0);
	}
	public static <T extends Screen> T setOverlayScreenStatic(T overlay, double z) {
		return ((OverlaySupportingScreen) MainUtil.client.gui.screen()).setOverlayScreen(overlay, z);
	}
	public static <T extends Screen> T setOverlayScreenStatic(T overlay) {
		return setOverlayScreenStatic(overlay, 0);
	}
	
	private GuiEventListener overlay; // extends Renderable & GuiEventListener
	private Screen overlayScreen;
	private double overlayZ;
	
	protected OverlaySupportingScreen(Component title) {
		super(title);
	}
	
	public <T extends Renderable & GuiEventListener> T setOverlay(T overlay, double z) {
		this.overlay = overlay;
		this.overlayScreen = null;
		this.overlayZ = z;
		if (overlay instanceof InitializableOverlay<?> initable)
			initable.initUnchecked(this);
		return overlay;
	}
	public <T extends Renderable & GuiEventListener> T setOverlay(T overlay) {
		return setOverlay(overlay, 0);
	}
	public <T extends Screen> T setOverlayScreen(T overlay, double z) {
		this.overlay = overlay;
		this.overlayScreen = overlay;
		this.overlayZ = z;
		overlay.init(width, height);
		if (overlay instanceof InitializableOverlay<?> initable)
			initable.initUnchecked(this);
		return overlay;
	}
	public <T extends Screen> T setOverlayScreen(T overlay) {
		return setOverlayScreen(overlay, 0);
	}
	
	@SuppressWarnings("unchecked")
	public <T extends Renderable & GuiEventListener> T getOverlay() {
		return (T) overlay;
	}
	public Screen getOverlayScreen() {
		return overlayScreen;
	}
	public double getOverlayZ() {
		return overlayZ;
	}
	
	@Override
	protected void init() {
		if (overlayScreen != null)
			overlayScreen.init(width, height);
		if (overlay instanceof InitializableOverlay<?> initable)
			initable.initUnchecked(this);
	}
	
	@Override
	public final void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		int bgMouseX = (overlay == null ? mouseX : -314);
		int bgMouseY = (overlay == null ? mouseY : -314);
		renderMain(context, bgMouseX, bgMouseY, delta);
		if (overlay != null) {
			if (overlayZ != 0)
				context.nextStratum();
			((Renderable) overlay).extractRenderState(context, mouseX, mouseY, delta);
		}
	}
	protected void renderMain(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		super.extractRenderState(context, mouseX, mouseY, delta);
	}
	
	@Override
	public void tick() {
		if (overlay != null) {
			if (overlay instanceof Tickable tickable)
				tickable.tick();
		} else
			super.tick();
	}
	
	@Override
	public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
		double mouseX = click.x(); double mouseY = click.y(); int button = click.button();
		if (overlay != null)
			return overlay.mouseClicked(click, doubled);
		return super.mouseClicked(click, doubled);
	}
	
	@Override
	public boolean mouseReleased(MouseButtonEvent click) {
		double mouseX = click.x(); double mouseY = click.y(); int button = click.button();
		if (overlay != null)
			return overlay.mouseReleased(click);
		return super.mouseReleased(click);
	}
	
	@Override
	public void mouseMoved(double mouseX, double mouseY) {
		if (overlay != null)
			overlay.mouseMoved(mouseX, mouseY);
		else
			super.mouseMoved(mouseX, mouseY);
	}
	
	@Override
	public boolean mouseDragged(MouseButtonEvent click, double deltaX, double deltaY) {
		double mouseX = click.x(); double mouseY = click.y(); int button = click.button();
		if (overlay != null)
			return overlay.mouseDragged(click, deltaX, deltaY);
		return super.mouseDragged(click, deltaX, deltaY);
	}
	
	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double xAmount, double yAmount) {
		if (overlay != null)
			return overlay.mouseScrolled(mouseX, mouseY, xAmount, yAmount);
		return super.mouseScrolled(mouseX, mouseY, xAmount, yAmount);
	}
	
	@Override
	public boolean keyPressed(KeyEvent input) {
		if (overlay != null)
			return overlay.keyPressed(input);
		return super.keyPressed(input);
	}
	
	@Override
	public boolean keyReleased(KeyEvent input) {
		if (overlay != null)
			return overlay.keyReleased(input);
		return super.keyReleased(input);
	}
	
	@Override
	public boolean charTyped(CharacterEvent input) {
		if (overlay != null)
			return overlay.charTyped(input);
		return super.charTyped(input);
	}
	
}
