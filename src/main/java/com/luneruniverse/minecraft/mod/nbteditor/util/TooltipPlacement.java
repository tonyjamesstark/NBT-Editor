package com.luneruniverse.minecraft.mod.nbteditor.util;

/**
 * Where an oversized tooltip goes on screen.
 *
 * <p>A tooltip is drawn at its natural size unless it does not fit, in which case it is scaled down
 * to fit and moved next to the cursor. Either way the result is clamped inside the screen. The
 * caller maps {@link #source()} onto {@link #target()}; when the tooltip fits, the two are equal
 * and the mapping is the identity.
 */
public record TooltipPlacement(Rect source, Rect target) {

	public record Rect(int x, int y, int width, int height) {}

	/** Padding added around the tooltip's content on every side. */
	private static final int PADDING = 5;

	/** Offset from the cursor used when a scaled-down tooltip is repositioned. */
	private static final int CURSOR_OFFSET = 12;

	/**
	 * @param x      left edge of the tooltip content, before padding
	 * @param y      top edge of the tooltip content, before padding
	 * @param width  width of the tooltip content, before padding
	 * @param height height of the tooltip content, before padding
	 */
	public static TooltipPlacement fit(int x, int y, int width, int height,
			int screenWidth, int screenHeight, int mouseX, int mouseY) {
		x -= PADDING;
		y -= PADDING;
		width += PADDING * 2;
		height += PADDING * 2;

		int newX = x;
		int newY = y;
		int newWidth = width;
		int newHeight = height;

		if (width > screenWidth || height > screenHeight) {
			double scale = Math.min((double) screenWidth / width, (double) screenHeight / height);
			newWidth = (int) (width * scale);
			newHeight = (int) (height * scale);
			newX = mouseX + CURSOR_OFFSET;
			newY = mouseY - CURSOR_OFFSET;
		}

		if (newX < 0)
			newX = 0;
		else if (newX + newWidth > screenWidth)
			newX = screenWidth - newWidth;

		if (newY < 0)
			newY = 0;
		else if (newY + newHeight > screenHeight)
			newY = screenHeight - newHeight;

		return new TooltipPlacement(new Rect(x, y, width, height), new Rect(newX, newY, newWidth, newHeight));
	}

	/** True when the tooltip is drawn where and how it was asked for. */
	public boolean isUnchanged() {
		return source.equals(target);
	}

}
