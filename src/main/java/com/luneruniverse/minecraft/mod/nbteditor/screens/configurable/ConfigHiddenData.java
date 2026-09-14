package com.luneruniverse.minecraft.mod.nbteditor.screens.configurable;

import java.util.function.BiFunction;

import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.gui.GuiGraphics;

public class ConfigHiddenData<S extends ConfigPath, D> implements ConfigPath {
	
	protected final S visible;
	protected D data;
	protected final BiFunction<D, Boolean, D> onClone;
	
	public ConfigHiddenData(S visible, D data, BiFunction<D, Boolean, D> onClone) {
		this.visible = visible;
		this.data = data;
		this.onClone = onClone;
		visible.setParent(this);
	}
	
	public S getVisible() {
		return visible;
	}
	public void setData(D data) {
		this.data = data;
	}
	public D getData() {
		return data;
	}
	
	@Override
	public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
		visible.render(context, mouseX, mouseY, delta);
	}
	
	@Override
	public boolean isValueValid() {
		return visible.isValueValid();
	}
	
	@Override
	public ConfigPath addValueListener(ConfigValueListener<ConfigValue<?, ?>> listener) {
		visible.addValueListener(listener);
		return this;
	}
	
	@Override
	public int getSpacingWidth() {
		return visible.getSpacingWidth();
	}
	
	@Override
	public int getRenderWidth() {
		return visible.getRenderWidth();
	}
	
	@Override
	public int getSpacingHeight() {
		return visible.getSpacingHeight();
	}
	
	@Override
	public int getRenderHeight() {
		return visible.getRenderHeight();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public ConfigHiddenData<S, D> clone(boolean defaults) {
		return new ConfigHiddenData<>((S) visible.clone(defaults), onClone.apply(data, defaults), onClone);
	}
	
	
	@Override
	public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
		double mouseX = click.x(); double mouseY = click.y(); int button = click.button();
		return visible.mouseClicked(click, doubled);
	}
	@Override
	public boolean mouseReleased(MouseButtonEvent click) {
		double mouseX = click.x(); double mouseY = click.y(); int button = click.button();
		return visible.mouseReleased(click);
	}
	@Override
	public void mouseMoved(double mouseX, double mouseY) {
		visible.mouseMoved(mouseX, mouseY);
	}
	@Override
	public boolean mouseDragged(MouseButtonEvent click, double deltaX, double deltaY) {
		double mouseX = click.x(); double mouseY = click.y(); int button = click.button();
		return visible.mouseDragged(click, deltaX, deltaY);
	}
	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double xAmount, double yAmount) {
		return visible.mouseScrolled(mouseX, mouseY, xAmount, yAmount);
	}
	@Override
	public boolean isMouseOver(double mouseX, double mouseY) {
		return visible.isMouseOver(mouseX, mouseY);
	}
	
	@Override
	public boolean keyPressed(KeyEvent input) {
		int keyCode = input.key(); int scanCode = input.scancode(); int modifiers = input.modifiers();
		return visible.keyPressed(input);
	}
	@Override
	public boolean keyReleased(KeyEvent input) {
		int keyCode = input.key(); int scanCode = input.scancode(); int modifiers = input.modifiers();
		return visible.keyReleased(input);
	}
	@Override
	public boolean charTyped(CharacterEvent input) {
		char chr = (char) input.codepoint(); int modifiers = input.modifiers();
		return visible.charTyped(input);
	}
	
	@Override
	public void tick() {
		visible.tick();
	}
	
}
