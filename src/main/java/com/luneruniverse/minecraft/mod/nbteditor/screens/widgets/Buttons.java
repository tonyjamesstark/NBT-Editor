package com.luneruniverse.minecraft.mod.nbteditor.screens.widgets;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVTooltip;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Buttons laid out by position and size, which is how every screen in the mod places them.
 *
 * <p>Vanilla's {@link Button#builder} is fluent and returns a partially built button; the mod
 * always wants the same four calls in the same order, and always has the bounds to hand. These
 * take the bounds directly and accept a {@link MVTooltip} where vanilla wants a {@link Tooltip}.
 */
public class Buttons {
	
	public static Button of(int x, int y, int width, int height, Component message, Button.OnPress onPress, MVTooltip tooltip) {
		Tooltip newTooltip = (tooltip == null ? null : tooltip.toNewTooltip());
		return Button.builder(message, onPress).bounds(x, y, width, height).tooltip(newTooltip).build();
	}
	
	public static Button of(int x, int y, int width, int height, Component message, Button.OnPress onPress) {
		return of(x, y, width, height, message, onPress, null);
	}
	
	/**
	 * A button drawn from a texture strip, where the hovered state sits {@code hoveredVOffset}
	 * pixels below the resting state in the same image.
	 */
	public static Button textured(int x, int y, int width, int height, int hoveredVOffset, Identifier img, Button.OnPress onPress, MVTooltip tooltip) {
		Button output = new TexturedButton(x, y, width, height, hoveredVOffset, img, onPress);
		if (tooltip != null) {
			output.setTooltip(tooltip.toNewTooltip());
		}
		return output;
	}
	
	public static Button textured(int x, int y, int width, int height, int hoveredVOffset, Identifier img, Button.OnPress onPress) {
		return textured(x, y, width, height, hoveredVOffset, img, onPress, null);
	}
	
}
