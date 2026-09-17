package com.luneruniverse.minecraft.mod.nbteditor.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.luneruniverse.minecraft.mod.nbteditor.util.TooltipPlacement.Rect;

/**
 * Pins the tooltip clamping that used to live inline in
 * {@code MixinLink.renderTooltipFromComponents}.
 */
class TooltipPlacementTest {

	private static final int SCREEN_WIDTH = 400;
	private static final int SCREEN_HEIGHT = 300;

	private static TooltipPlacement fit(int x, int y, int width, int height) {
		return TooltipPlacement.fit(x, y, width, height, SCREEN_WIDTH, SCREEN_HEIGHT, 200, 150);
	}

	@Test
	void aTooltipThatFitsIsPaddedAndLeftAlone() {
		TooltipPlacement placement = fit(100, 100, 50, 40);

		assertEquals(new Rect(95, 95, 60, 50), placement.source(), "padding is 5 on every side");
		assertEquals(placement.source(), placement.target(), "a tooltip that fits must not be moved or scaled");
		assertTrue(placement.isUnchanged());
	}

	@Test
	void aTooltipOffTheLeftEdgeIsPushedRight() {
		TooltipPlacement placement = fit(2, 100, 50, 40);

		assertEquals(-3, placement.source().x(), "the source keeps its true position");
		assertEquals(0, placement.target().x(), "the target is clamped to the screen");
		assertFalse(placement.isUnchanged());
	}

	@Test
	void aTooltipOffTheRightEdgeIsPushedLeft() {
		TooltipPlacement placement = fit(380, 100, 50, 40);

		assertEquals(SCREEN_WIDTH, placement.target().x() + placement.target().width(),
				"the target must end exactly at the screen edge");
	}

	@Test
	void aTooltipOffTheTopAndBottomIsClamped() {
		assertEquals(0, fit(100, 2, 50, 40).target().y());

		TooltipPlacement low = fit(100, 280, 50, 40);
		assertEquals(SCREEN_HEIGHT, low.target().y() + low.target().height());
	}

	@Test
	void anOversizedTooltipIsScaledDownAndMovedToTheCursor() {
		TooltipPlacement placement = TooltipPlacement.fit(0, 0, 790, 100, SCREEN_WIDTH, SCREEN_HEIGHT, 120, 90);

		assertEquals(new Rect(-5, -5, 800, 110), placement.source());
		assertEquals(SCREEN_WIDTH, placement.target().width(), "width is scaled to exactly fill the screen");
		assertEquals(55, placement.target().height(), "height is scaled by the same factor");
		assertEquals(0, placement.target().x(), "cursor x + 12 would overflow, so it clamps to 0");
		assertEquals(78, placement.target().y(), "cursor y - 12");
	}

	@Test
	void anOversizedTooltipStaysOnScreenAfterBeingMoved() {
		TooltipPlacement placement = TooltipPlacement.fit(0, 0, 200, 600, SCREEN_WIDTH, SCREEN_HEIGHT, 395, 295);
		Rect target = placement.target();

		assertTrue(target.x() >= 0, "target x went off the left edge: " + target);
		assertTrue(target.y() >= 0, "target y went off the top edge: " + target);
		assertTrue(target.x() + target.width() <= SCREEN_WIDTH, "target overflowed the right edge: " + target);
		assertTrue(target.y() + target.height() <= SCREEN_HEIGHT, "target overflowed the bottom edge: " + target);
	}

	@Test
	void aTooltipLargerThanTheScreenInBothDirectionsUsesTheSmallerScale() {
		TooltipPlacement placement = TooltipPlacement.fit(0, 0, 790, 890, SCREEN_WIDTH, SCREEN_HEIGHT, 0, 0);
		Rect target = placement.target();

		assertTrue(target.width() <= SCREEN_WIDTH, "scaled width still overflows: " + target);
		assertTrue(target.height() <= SCREEN_HEIGHT, "scaled height still overflows: " + target);
	}

}
