package com.luneruniverse.minecraft.mod.nbteditor.util;

import java.util.List;

import com.luneruniverse.minecraft.mod.nbteditor.async.UpdateCheckerThread;


import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.client.Minecraft;

/**
 * Thin cover over {@link GuiGraphicsExtractor} for the calls the mod makes in more than one place.
 * The MatrixStack half of this class died with the 1.21.9 GUI rewrite, and 26.2 turned the
 * remaining immediate-mode draws into render-state extraction; the method names here are the
 * mod's, so callers did not have to move with them.
 *
 * <p>This was <code>multiversion/MVDrawableHelper</code>. Nothing about it spans game versions --
 * the package was the only version-flavoured thing left -- and most of what remains is a
 * one-line forward that A1 of the audit wants inlined.
 */
public class Drawing {
	
	
	private static final Identifier LOGO = Identifier.fromNamespaceAndPath("nbteditor", "textures/logo.png");
	private static final Identifier LOGO_UPDATE_AVAILABLE = Identifier.fromNamespaceAndPath("nbteditor", "textures/logo_update_available.png");
	
	/** The mod's badge, top left of every editor screen. It changes when an update is waiting. */
	public static void renderLogo(GuiGraphicsExtractor context) {
		drawTexture(context, UpdateCheckerThread.UPDATE_AVAILABLE ? LOGO_UPDATE_AVAILABLE : LOGO,
				16, 16, 0, 0, 32, 32, 32, 32);
	}
	
	/** The cursor in gui coordinates, which is what every caller wants and neither axis reports. */
	public static int[] getMousePos() {
		double scale = Minecraft.getInstance().getWindow().getGuiScale();
		int x = (int) (Minecraft.getInstance().mouseHandler.xpos() / scale);
		int y = (int) (Minecraft.getInstance().mouseHandler.ypos() / scale);
		return new int[] {x, y};
	}
	
	/** Draws <code>text</code> broken over as many lines as {@link TextWrapping} needs. */
	public static void drawWrappingString(GuiGraphicsExtractor context, Font renderer, String text, int x, int y, int maxWidth, int color, boolean centerHorizontal, boolean centerVertical) {
		List<String> lines = TextWrapping.wrap(text, maxWidth, renderer::width);
		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			int offsetY = i * renderer.lineHeight + (centerVertical ? -renderer.lineHeight * lines.size() / 2 : 0);
			if (centerHorizontal)
				drawCenteredTextWithShadow(context, renderer, Component.nullToEmpty(line), x, y + offsetY, color);
			else
				drawTextWithShadow(context, renderer, Component.nullToEmpty(line), x, y + offsetY, color);
		}
	}
	
	public static void fill(GuiGraphicsExtractor context, int x1, int y1, int x2, int y2, int color) {
		context.fill(x1, y1, x2, y2, color);
	}
	
	public static void drawText(GuiGraphicsExtractor context, Font textRenderer, Component text, int x, int y, int color, boolean shadow) {
		context.text(textRenderer, text, x, y, color, shadow);
	}
	
	public static void drawTextWithoutShadow(GuiGraphicsExtractor context, Font textRenderer, Component text, int x, int y, int color) {
		context.text(textRenderer, text, x, y, color, false);
	}
	
	public static void drawTextWithShadow(GuiGraphicsExtractor context, Font textRenderer, Component text, int x, int y, int color) {
		context.text(textRenderer, text, x, y, color);
	}
	
	public static void drawCenteredTextWithShadow(GuiGraphicsExtractor context, Font textRenderer, Component text, int x, int y, int color) {
		context.centeredText(textRenderer, text, x, y, color);
	}
	
	public static void drawTexture(GuiGraphicsExtractor context, Identifier texture, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
		context.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height, textureWidth, textureHeight);
	}
	public static void drawTexture(GuiGraphicsExtractor context, Identifier texture, int x, int y, float u, float v, int width, int height) {
		drawTexture(context, texture, x, y, u, v, width, height, 256, 256);
	}
	
	public static void renderTooltip(GuiGraphicsExtractor context, Component text, int x, int y) {
		context.setTooltipForNextFrame(Minecraft.getInstance().font, text, x, y);
	}
	
	public static void renderTooltip(GuiGraphicsExtractor context, List<FormattedCharSequence> lines, int x, int y) {
		context.setTooltipForNextFrame(Minecraft.getInstance().font, lines, x, y);
	}
	
	public static void renderItem(GuiGraphicsExtractor context, float zOffset, boolean setScreenZOffset, ItemStack item, int x, int y) {
		context.item(item, x, y);
		context.itemDecorations(Minecraft.getInstance().font, item, x, y);
	}
	
	public static void renderBackground(Screen screen, GuiGraphicsExtractor context) {
		int[] mousePos = Drawing.getMousePos();
		if (Minecraft.getInstance().level == null)
			screen.extractBackground(context, mousePos[0], mousePos[1], Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true));
		else
			screen.extractTransparentBackground(context);
	}
	
	public static void drawSlotHighlight(GuiGraphicsExtractor context, int x, int y, int color) {
		context.fill(RenderPipelines.GUI, x, y, x + 16, y + 16, color);
	}
	
	public static void enableScissor(GuiGraphicsExtractor context, int x, int y, int width, int height) {
		context.enableScissor(x, y, x + width, y + height);
	}
	public static void disableScissor(GuiGraphicsExtractor context) {
		context.disableScissor();
	}
	
}
