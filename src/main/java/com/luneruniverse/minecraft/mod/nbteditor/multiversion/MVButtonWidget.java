package com.luneruniverse.minecraft.mod.nbteditor.multiversion;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

public class MVButtonWidget extends AbstractButton {
	
	@FunctionalInterface
	public interface PressAction {
		public void onPress(MVButtonWidget button);
	}
	
	private final PressAction onPress;
	private final MVTooltip tooltip;
	
	public MVButtonWidget(int x, int y, int width, int height, Component text, PressAction onPress, MVTooltip tooltip) {
		super(x, y, width, height, text);
		this.onPress = onPress;
		this.tooltip = tooltip;
		if (tooltip != null)
			setTooltip(tooltip.toNewTooltip());
	}
	public MVButtonWidget(int x, int y, int width, int height, Component text, PressAction onPress) {
		this(x, y, width, height, text, onPress, null);
	}
	
	@Override
	public void onPress(InputWithModifiers input) {
		onPress.onPress(this);
	}
	
	@Override
	protected void updateWidgetNarration(NarrationElementOutput builder) {
		defaultButtonNarrationText(builder);
	}
	
	/**
	 * Paints the whole button. This is the hook subclasses replace; 1.21.9 made
	 * {@code renderWidget} final and moved widget painting into {@code drawIcon}.
	 */
	public void renderButton(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		extractDefaultSprite(context);
		extractDefaultLabel(context.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE));
	}
	@Override
	protected final void extractContents(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		renderButton(context, mouseX, mouseY, delta);
	}
	
}
