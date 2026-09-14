package com.luneruniverse.minecraft.mod.nbteditor.screens.widgets;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVElement;

import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.narration.NarratableEntry.NarrationPriority;

public class List2D extends Panel<List2D.List2DValue> {
	
	public static abstract class List2DValue implements Renderable, MVElement {
		
		protected static final Minecraft client = Minecraft.getInstance();
		protected static final Font textRenderer = client.font;
		
		private boolean insideList;
		
		private void setInsideList(boolean insideList) {
			this.insideList = insideList;
		}
		protected boolean isInsideList() {
			return this.insideList;
		}
		
	}
	
	
	
	private int itemWidth;
	private int itemHeight;
	private int itemPadding;
	
	private final List<PositionedPanelElement<List2DValue>> elements;
	private GuiEventListener finalEventHandler;
	
	public List2D(int x, int y, int width, int height, int outerPadding, int itemWidth, int itemHeight, int itemPadding) {
		super(x, y, width, height, outerPadding, true);
		
		this.itemWidth = itemWidth;
		this.itemHeight = itemHeight;
		this.itemPadding = itemPadding;
		
		this.elements = new ArrayList<>();
	}
	public List2D setFinalEventHandler(GuiEventListener finalEventHandler) {
		this.finalEventHandler = finalEventHandler;
		return this;
	}
	public List2D addElement(List2DValue element) {
		this.elements.add(genPositioned(element, elements.size()));
		return this;
	}
	public List2D removeElement(List2DValue element) {
		if (this.elements.removeIf(pos -> pos.element() == element)) {
			this.elements.replaceAll(new UnaryOperator<>() {
				private int i = 0;
				@Override
				public PositionedPanelElement<List2DValue> apply(PositionedPanelElement<List2DValue> pos) {
					return genPositioned(pos.element(), i++);
				}
			});
		}
		return this;
	}
	public List2D clearElements() {
		this.elements.clear();
		return this;
	}
	public List2D addElements(List<List2DValue> elements) {
		elements.forEach(this::addElement);
		return this;
	}
	public List<List2DValue> getElements() {
		return this.elements.stream().map(PositionedPanelElement::element).toList();
	}
	private PositionedPanelElement<List2DValue> genPositioned(List2DValue element, int i) {
		int elementsPerRow = (width + itemPadding) / (itemWidth + itemPadding);
		int x = i % elementsPerRow * (itemWidth + itemPadding);
		int y = i / elementsPerRow * (itemHeight + itemPadding);
		
		return new PositionedPanelElement<>(element, x, y);
	}
	@Override
	protected Iterable<PositionedPanelElement<List2DValue>> getPanelElements() {
		return this.elements;
	}
	
	@Override
	public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
		double mouseX = click.x(); double mouseY = click.y(); int button = click.button();
		return super.mouseClicked(click, doubled) || finalEventHandler.mouseClicked(new MouseButtonEvent(mouseX - x, mouseY - y, click.buttonInfo()), doubled);
	}
	@Override
	public boolean mouseReleased(MouseButtonEvent click) {
		double mouseX = click.x(); double mouseY = click.y(); int button = click.button();
		return super.mouseReleased(click) || finalEventHandler.mouseReleased(new MouseButtonEvent(mouseX - x, mouseY - y, click.buttonInfo()));
	}
	
	@Override
	public void mouseMoved(double mouseX, double mouseY) {
		super.mouseMoved(mouseX, mouseY);
		finalEventHandler.mouseMoved(mouseX, mouseY);
	}
	@Override
	public boolean mouseDragged(MouseButtonEvent click, double deltaX, double deltaY) {
		double mouseX = click.x(); double mouseY = click.y(); int button = click.button();
		return super.mouseDragged(click, deltaX, deltaY) || finalEventHandler.mouseDragged(new MouseButtonEvent(mouseX - x, mouseY - y, click.buttonInfo()), deltaX, deltaY);
	}
	@Override
	protected void updateMousePos(double mouseX, double mouseY) {
		boolean hovering = isMouseOver(mouseX, mouseY);
		for (PositionedPanelElement<List2DValue> pos : this.elements)
			pos.element().setInsideList(hovering);
	}
	
	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double xAmount, double yAmount) {
		return super.mouseScrolled(mouseX, mouseY, xAmount, yAmount) || finalEventHandler.mouseScrolled(mouseX, mouseY, xAmount, yAmount);
	}
	@Override
	protected int getPanelElementHeight(List2DValue element) {
		return itemHeight;
	}
	public void setScroll(int scroll) {
		this.scroll = scroll;
	}
	public int getScroll() {
		return scroll;
	}
	
	@Override
	public boolean keyPressed(KeyEvent input) {
		int keyCode = input.key(); int scanCode = input.scancode(); int modifiers = input.modifiers();
		return super.keyPressed(input) || finalEventHandler.keyPressed(input);
	}
	@Override
	public boolean keyReleased(KeyEvent input) {
		int keyCode = input.key(); int scanCode = input.scancode(); int modifiers = input.modifiers();
		return super.keyReleased(input) || finalEventHandler.keyReleased(input);
	}
	@Override
	public boolean charTyped(CharacterEvent input) {
		char chr = (char) input.codepoint(); int modifiers = input.modifiers();
		return super.charTyped(input) || finalEventHandler.charTyped(input);
	}
	
	
	@Override
	public void updateNarration(NarrationElementOutput builder) {
		
	}
	
	@Override
	public NarrationPriority narrationPriority() {
		return NarrationPriority.FOCUSED;
	}
	
}
