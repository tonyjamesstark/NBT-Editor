package com.luneruniverse.minecraft.mod.nbteditor.multiversion;

import java.util.List;

import com.luneruniverse.minecraft.mod.nbteditor.util.MainUtil;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Thin cover over {@link DrawContext} for the calls the mod makes in more than one place.
 * The MatrixStack half of this class died with the 1.21.9 GUI rewrite: DrawContext now
 * carries a 2D matrix stack of its own, so there is nothing left to bridge.
 */
public class MVDrawableHelper {
	
	public static VertexConsumerProvider.Immediate getVertexConsumerProvider() {
		return MainUtil.client.gameRenderer.buffers.getEntityVertexConsumers();
	}
	
	public static void fill(DrawContext context, int x1, int y1, int x2, int y2, int color) {
		context.fill(x1, y1, x2, y2, color);
	}
	
	public static void drawText(DrawContext context, TextRenderer textRenderer, Text text, int x, int y, int color, boolean shadow) {
		context.drawText(textRenderer, text, x, y, color, shadow);
	}
	
	public static void drawTextWithoutShadow(DrawContext context, TextRenderer textRenderer, Text text, int x, int y, int color) {
		context.drawText(textRenderer, text, x, y, color, false);
	}
	
	public static void drawTextWithShadow(DrawContext context, TextRenderer textRenderer, Text text, int x, int y, int color) {
		context.drawTextWithShadow(textRenderer, text, x, y, color);
	}
	
	public static void drawCenteredTextWithShadow(DrawContext context, TextRenderer textRenderer, Text text, int x, int y, int color) {
		context.drawCenteredTextWithShadow(textRenderer, text, x, y, color);
	}
	
	public static void drawTexture(DrawContext context, Identifier texture, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
		context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height, textureWidth, textureHeight);
	}
	public static void drawTexture(DrawContext context, Identifier texture, int x, int y, float u, float v, int width, int height) {
		drawTexture(context, texture, x, y, u, v, width, height, 256, 256);
	}
	
	public static void renderTooltip(DrawContext context, Text text, int x, int y) {
		context.drawTooltip(MainUtil.client.textRenderer, text, x, y);
	}
	
	public static void renderTooltip(DrawContext context, List<OrderedText> lines, int x, int y) {
		context.drawOrderedTooltip(MainUtil.client.textRenderer, lines, x, y);
	}
	
	public static void renderItem(DrawContext context, float zOffset, boolean setScreenZOffset, ItemStack item, int x, int y) {
		context.drawItem(item, x, y);
		context.drawStackOverlay(MainUtil.client.textRenderer, item, x, y);
	}
	
	public static void renderBackground(Screen screen, DrawContext context) {
		int[] mousePos = MainUtil.getMousePos();
		if (MainUtil.client.world == null)
			screen.renderBackground(context, mousePos[0], mousePos[1], MVMisc.getTickDelta());
		else
			screen.renderInGameBackground(context);
	}
	
	public static void drawSlotHighlight(DrawContext context, int x, int y, int color) {
		context.fill(RenderPipelines.GUI, x, y, x + 16, y + 16, color);
	}
	
	public static void enableScissor(DrawContext context, int x, int y, int width, int height) {
		context.enableScissor(x, y, x + width, y + height);
	}
	public static void disableScissor(DrawContext context) {
		context.disableScissor();
	}
	
}
