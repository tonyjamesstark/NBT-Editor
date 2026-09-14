package com.luneruniverse.minecraft.mod.nbteditor.multiversion;

import java.util.List;

import com.luneruniverse.minecraft.mod.nbteditor.util.MainUtil;

import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Thin cover over {@link GuiGraphics} for the calls the mod makes in more than one place.
 * The MatrixStack half of this class died with the 1.21.9 GUI rewrite: GuiGraphics now
 * carries a 2D matrix stack of its own, so there is nothing left to bridge.
 */
public class MVDrawableHelper {
	
	
	public static void fill(GuiGraphics context, int x1, int y1, int x2, int y2, int color) {
		context.fill(x1, y1, x2, y2, color);
	}
	
	public static void drawText(GuiGraphics context, Font textRenderer, Component text, int x, int y, int color, boolean shadow) {
		context.drawString(textRenderer, text, x, y, color, shadow);
	}
	
	public static void drawTextWithoutShadow(GuiGraphics context, Font textRenderer, Component text, int x, int y, int color) {
		context.drawString(textRenderer, text, x, y, color, false);
	}
	
	public static void drawTextWithShadow(GuiGraphics context, Font textRenderer, Component text, int x, int y, int color) {
		context.drawString(textRenderer, text, x, y, color);
	}
	
	public static void drawCenteredTextWithShadow(GuiGraphics context, Font textRenderer, Component text, int x, int y, int color) {
		context.drawCenteredString(textRenderer, text, x, y, color);
	}
	
	public static void drawTexture(GuiGraphics context, Identifier texture, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
		context.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height, textureWidth, textureHeight);
	}
	public static void drawTexture(GuiGraphics context, Identifier texture, int x, int y, float u, float v, int width, int height) {
		drawTexture(context, texture, x, y, u, v, width, height, 256, 256);
	}
	
	public static void renderTooltip(GuiGraphics context, Component text, int x, int y) {
		context.setTooltipForNextFrame(MainUtil.client.font, text, x, y);
	}
	
	public static void renderTooltip(GuiGraphics context, List<FormattedCharSequence> lines, int x, int y) {
		context.setTooltipForNextFrame(MainUtil.client.font, lines, x, y);
	}
	
	public static void renderItem(GuiGraphics context, float zOffset, boolean setScreenZOffset, ItemStack item, int x, int y) {
		context.renderItem(item, x, y);
		context.renderItemDecorations(MainUtil.client.font, item, x, y);
	}
	
	public static void renderBackground(Screen screen, GuiGraphics context) {
		int[] mousePos = MainUtil.getMousePos();
		if (MainUtil.client.level == null)
			screen.renderBackground(context, mousePos[0], mousePos[1], MVMisc.getTickDelta());
		else
			screen.renderTransparentBackground(context);
	}
	
	public static void drawSlotHighlight(GuiGraphics context, int x, int y, int color) {
		context.fill(RenderPipelines.GUI, x, y, x + 16, y + 16, color);
	}
	
	public static void enableScissor(GuiGraphics context, int x, int y, int width, int height) {
		context.enableScissor(x, y, x + width, y + height);
	}
	public static void disableScissor(GuiGraphics context) {
		context.disableScissor();
	}
	
}
