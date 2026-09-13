package com.luneruniverse.minecraft.mod.nbteditor.multiversion;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.client.input.AbstractInput;
import net.minecraft.text.Text;

public class MVButtonWidget extends PressableWidget {
	
	@FunctionalInterface
	public interface PressAction {
		public void onPress(MVButtonWidget button);
	}
	
	private final PressAction onPress;
	private final MVTooltip tooltip;
	
	public MVButtonWidget(int x, int y, int width, int height, Text text, PressAction onPress, MVTooltip tooltip) {
		super(x, y, width, height, text);
		this.onPress = onPress;
		this.tooltip = tooltip;
		if (tooltip != null)
			setTooltip(tooltip.toNewTooltip());
	}
	public MVButtonWidget(int x, int y, int width, int height, Text text, PressAction onPress) {
		this(x, y, width, height, text, onPress, null);
	}
	
	@Override
	public void onPress(AbstractInput input) {
		onPress.onPress(this);
	}
	
	@Override
	protected void appendClickableNarrations(NarrationMessageBuilder builder) {
		appendDefaultNarrations(builder);
	}
	
	/**
	 * Paints the whole button. This is the hook subclasses replace; 1.21.9 made
	 * {@code renderWidget} final and moved widget painting into {@code drawIcon}.
	 */
	public void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
		drawButton(context);
		drawLabel(context.getHoverListener(this, DrawContext.HoverType.NONE));
	}
	@Override
	protected final void drawIcon(DrawContext context, int mouseX, int mouseY, float delta) {
		renderButton(context, mouseX, mouseY, delta);
	}
	
}
