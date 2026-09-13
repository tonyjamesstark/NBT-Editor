package com.luneruniverse.minecraft.mod.nbteditor.screens.widgets;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVDrawableHelper;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVElement;

import net.minecraft.client.input.KeyInput;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.DrawContext;

public abstract class Panel<T extends Drawable & Element> implements Drawable, MVElement, Selectable {
	
	public static record PositionedPanelElement<T extends Drawable & Element>(T element, int x, int y) {
	}
	
	protected int x;
	protected int y;
	protected int width;
	protected int height;
	protected int renderPadding; // An area around the panel which elements can draw in, but events aren't passed - useful for borders
	protected boolean scrollable;
	protected int scroll;
	protected final ScrollBarWidget scrollBar;
	
	protected Panel(int x, int y, int width, int height, int renderPadding, boolean scrollable) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		this.renderPadding = renderPadding;
		
		this.scrollable = scrollable;
		this.scroll = 0;
		this.scrollBar = new ScrollBarWidget(x + width + renderPadding - 8, y, height,
				() -> scroll, scroll -> this.scroll = scroll, this::getMaxScroll);
	}
	private int getPaddedX() {
		return x - renderPadding;
	}
	private int getPaddedY() {
		return y - renderPadding;
	}
	private int getPaddedWidth() {
		return width + renderPadding * 2;
	}
	private int getPaddedHeight() {
		return height + renderPadding * 2;
	}
	
	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		updateMousePos(mouseX, mouseY);
		
		checkOverScroll();
		
		MVDrawableHelper.enableScissor(context, getPaddedX(), getPaddedY(), getPaddedWidth(), getPaddedHeight());
		
		for (PositionedPanelElement<T> pos : getPanelElementsSafe()) {
			T element = pos.element();
			
			context.getMatrices().pushMatrix();
			context.getMatrices().translate((float) (pos.x() + x), (float) (pos.y() + y + scroll));
			element.render(context, mouseX - pos.x() - x, mouseY - pos.y() - y - scroll, delta);
			context.getMatrices().popMatrix();
		}
		
		MVDrawableHelper.disableScissor(context);
		
		scrollBar.render(context, mouseX, mouseY, delta);
	}
	
	private void checkOverScroll() {
		int maxScroll = getMaxScroll();
		if (scroll < maxScroll)
			scroll = maxScroll;
	}
	
	@Override
	public boolean isMouseOver(double mouseX, double mouseY) {
		return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
	}
	
	
	protected abstract Iterable<PositionedPanelElement<T>> getPanelElements();
	protected final List<PositionedPanelElement<T>> getPanelElementsSafe() {
		List<PositionedPanelElement<T>> output = new ArrayList<>();
		getPanelElements().forEach(output::add);
		return output;
	}
	protected boolean continueEvents() {
		return true;
	}
	protected void updateMousePos(double mouseX, double mouseY) {}
	
	
	@Override
	public boolean mouseClicked(Click click, boolean doubled) {
		double mouseX = click.x(); double mouseY = click.y(); int button = click.button();
		updateMousePos(mouseX, mouseY);
		
		if (scrollBar.mouseClicked(click, doubled))
			return true;
		
		boolean success = false;
		for (PositionedPanelElement<T> pos : getPanelElementsSafe()) {
			if (pos.element().mouseClicked(new Click(mouseX - pos.x() - x, mouseY - pos.y() - y - scroll, click.buttonInfo()), doubled)) {
				success = true;
				if (!continueEvents())
					break;
			}
		}
		return success;
	}
	
	@Override
	public boolean mouseReleased(Click click) {
		double mouseX = click.x(); double mouseY = click.y(); int button = click.button();
		updateMousePos(mouseX, mouseY);
		
		boolean success = false;
		for (PositionedPanelElement<T> pos : getPanelElementsSafe()) {
			if (pos.element().mouseReleased(new Click(mouseX - pos.x() - x, mouseY - pos.y() - y - scroll, click.buttonInfo()))) {
				success = true;
				if (!continueEvents())
					break;
			}
		}
		return success;
	}
	
	@Override
	public void mouseMoved(double mouseX, double mouseY) {
		updateMousePos(mouseX, mouseY);
		
		for (PositionedPanelElement<T> pos : getPanelElementsSafe())
			pos.element().mouseMoved(mouseX - pos.x() - x, mouseY - pos.y() - y - scroll);
	}
	
	@Override
	public boolean mouseDragged(Click click, double deltaX, double deltaY) {
		double mouseX = click.x(); double mouseY = click.y(); int button = click.button();
		updateMousePos(mouseX, mouseY);
		
		if (scrollBar.mouseDragged(click, deltaX, deltaY))
			return true;
		
		boolean success = false;
		for (PositionedPanelElement<T> pos : getPanelElementsSafe()) {
			if (pos.element().mouseDragged(new Click(mouseX - pos.x() - x, mouseY - pos.y() - y - scroll, click.buttonInfo()), deltaX, deltaY)) {
				success = true;
				if (!continueEvents())
					break;
			}
		}
		return success;
	}
	
	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double xAmount, double yAmount) {
		updateMousePos(mouseX, mouseY);
		
		boolean success = false;
		for (PositionedPanelElement<T> pos : getPanelElementsSafe()) {
			if (pos.element().mouseScrolled(mouseX - pos.x() - x, mouseY - pos.y() - y, xAmount, yAmount)) {
				success = true;
				if (!continueEvents())
					break;
			}
		}
		if (!success && scrollable)
			success = scrollBar.mouseScrolled(mouseX, mouseY, xAmount, yAmount);
		return success;
	}
	public int getMaxScroll() {
		return Math.min(0, height - getHighestY());
	}
	protected int getHighestY() {
		return StreamSupport.stream(getPanelElements().spliterator(), false)
				.mapToInt(pos -> pos.y() + getPanelElementHeight(pos.element())).max().orElse(0);
	}
	protected int getPanelElementHeight(T element) {
		return 0;
	}
	
	
	@Override
	public boolean keyPressed(KeyInput input) {
		int keyCode = input.key(); int scanCode = input.scancode(); int modifiers = input.modifiers();
		boolean success = false;
		for (PositionedPanelElement<T> pos : getPanelElementsSafe()) {
			if (pos.element().keyPressed(input)) {
				success = true;
				if (!continueEvents())
					break;
			}
		}
		return success;
	}
	@Override
	public boolean keyReleased(KeyInput input) {
		int keyCode = input.key(); int scanCode = input.scancode(); int modifiers = input.modifiers();
		boolean success = false;
		for (PositionedPanelElement<T> pos : getPanelElementsSafe()) {
			if (pos.element().keyReleased(input)) {
				success = true;
				if (!continueEvents())
					break;
			}
		}
		return success;
	}
	@Override
	public boolean charTyped(CharInput input) {
		char chr = (char) input.codepoint(); int modifiers = input.modifiers();
		boolean success = false;
		for (PositionedPanelElement<T> pos : getPanelElementsSafe()) {
			if (pos.element().charTyped(input)) {
				success = true;
				if (!continueEvents())
					break;
			}
		}
		return success;
	}
	
}
