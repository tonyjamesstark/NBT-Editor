package com.luneruniverse.minecraft.mod.nbteditor.screens.widgets;

import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.GuiGraphics;

public class TranslatedGroupWidget extends GroupWidget {
	
	public static <T extends Renderable & GuiEventListener> TranslatedGroupWidget forWidget(T widget, double x, double y, double z) {
		TranslatedGroupWidget output = new TranslatedGroupWidget(x, y, z) {
			@Override
			protected void renderPre(GuiGraphics context, int mouseX, int mouseY, float delta) {
				setFocused(isMultiFocused() ? widget : null);
			}
		};
		output.addWidget(widget);
		return output;
	}
	
	private double x;
	private double y;
	private double z;
	
	public TranslatedGroupWidget(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	public TranslatedGroupWidget() {
		this(0, 0, 0);
	}
	
	public TranslatedGroupWidget setTranslation(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
		return this;
	}
	
	public TranslatedGroupWidget addTranslation(double x, double y, double z) {
		this.x += x;
		this.y += y;
		this.z += z;
		return this;
	}
	
	@Override
	public final void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
		context.pose().pushMatrix();
		context.pose().translate((float) x, (float) y);
		mouseX -= (int) x;
		mouseY -= (int) y;
		renderPre(context, mouseX, mouseY, delta);
		super.render(context, mouseX, mouseY, delta);
		renderPost(context, mouseX, mouseY, delta);
		context.pose().popMatrix();
	}
	protected void renderPre(GuiGraphics context, int mouseX, int mouseY, float delta) {}
	protected void renderPost(GuiGraphics context, int mouseX, int mouseY, float delta) {}
	
	@Override
	public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
		double mouseX = click.x(); double mouseY = click.y(); int button = click.button();
		mouseX -= x;
		mouseY -= y;
		return mouseClickedPre(mouseX, mouseY, button) ||
				super.mouseClicked(click, doubled) ||
				mouseClickedPost(mouseX, mouseY, button);
	}
	protected boolean mouseClickedPre(double mouseX, double mouseY, int button) {
		return false;
	}
	protected boolean mouseClickedPost(double mouseX, double mouseY, int button) {
		return false;
	}
	
	@Override
	public boolean mouseReleased(MouseButtonEvent click) {
		double mouseX = click.x(); double mouseY = click.y(); int button = click.button();
		mouseX -= x;
		mouseY -= y;
		return mouseReleasedPre(mouseX, mouseY, button) ||
				super.mouseReleased(click) ||
				mouseReleasedPost(mouseX, mouseY, button);
	}
	protected boolean mouseReleasedPre(double mouseX, double mouseY, int button) {
		return false;
	}
	protected boolean mouseReleasedPost(double mouseX, double mouseY, int button) {
		return false;
	}
	
	@Override
	public void mouseMoved(double mouseX, double mouseY) {
		mouseX -= x;
		mouseY -= y;
		mouseMovedPre(mouseX, mouseY);
		super.mouseMoved(mouseX, mouseY);
		mouseMovedPost(mouseX, mouseY);
	}
	protected void mouseMovedPre(double mouseX, double mouseY) {}
	protected void mouseMovedPost(double mouseX, double mouseY) {}
	
	@Override
	public boolean mouseDragged(MouseButtonEvent click, double deltaX, double deltaY) {
		double mouseX = click.x(); double mouseY = click.y(); int button = click.button();
		mouseX -= x;
		mouseY -= y;
		return mouseDraggedPre(mouseX, mouseY, button, deltaX, deltaY) ||
				super.mouseDragged(click, deltaX, deltaY) ||
				mouseDraggedPost(mouseX, mouseY, button, deltaX, deltaY);
	}
	protected boolean mouseDraggedPre(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		return false;
	}
	protected boolean mouseDraggedPost(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		return false;
	}
	
	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double xAmount, double yAmount) {
		mouseX -= x;
		mouseY -= y;
		return mouseScrolledPre(mouseX, mouseY, xAmount, yAmount) ||
				super.mouseScrolled(mouseX, mouseY, xAmount, yAmount) ||
				mouseScrolledPost(mouseX, mouseY, xAmount, yAmount);
	}
	protected boolean mouseScrolledPre(double mouseX, double mouseY, double xAmount, double yAmount) {
		return false;
	}
	protected boolean mouseScrolledPost(double mouseX, double mouseY, double xAmount, double yAmount) {
		return false;
	}
	
	@Override
	public boolean isMouseOver(double mouseX, double mouseY) {
		mouseX -= x;
		mouseY -= y;
		return isMouseOverPre(mouseX, mouseY) ||
				super.isMouseOver(mouseX, mouseY);
	}
	protected boolean isMouseOverPre(double mouseX, double mouseY) {
		return false;
	}
	
}
