package com.luneruniverse.minecraft.mod.nbteditor.screens.widgets;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVDrawableHelper;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.Identifier;

/**
 * A button whose whole appearance is one texture strip: the resting state on top, then the
 * hovered state, then the disabled state, each {@code hoveredVOffset} pixels below the last.
 *
 * <p>The strip is assumed to be exactly as wide as the button, which is how every mod texture
 * is drawn. Build one through {@link Buttons#textured}.
 */
public class TexturedButton extends Button {
	
	private final Identifier texture;
	private final int hoveredVOffset;
	
	public TexturedButton(int x, int y, int width, int height, int hoveredVOffset, Identifier texture,
			Button.OnPress pressAction) {
		super(x, y, width, height, CommonComponents.EMPTY, pressAction, DEFAULT_NARRATION);
		this.texture = texture;
		this.hoveredVOffset = hoveredVOffset;
	}
	
	@Override
	protected void extractContents(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		int v = 0;
		if (!isActive())
			v += hoveredVOffset * 2;
		else if (isHoveredOrFocused())
			v += hoveredVOffset;
		MVDrawableHelper.drawTexture(context, texture, getX(), getY(), 0, v, width, height,
				width, height + hoveredVOffset);
	}
	
}
