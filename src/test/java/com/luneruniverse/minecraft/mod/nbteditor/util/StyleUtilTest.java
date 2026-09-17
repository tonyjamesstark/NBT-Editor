package com.luneruniverse.minecraft.mod.nbteditor.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Pins the colour scaling that used to live in {@code MVMisc}. Text shadows are drawn by
 * scaling the text colour, so an off-by-one here is visible on every formatted screen.
 */
class StyleUtilTest {

	@Test
	void scalingByOneLeavesAColourAlone() {
		assertEquals(0xFF336699, StyleUtil.scaleRgb(0xFF336699, 1.0));
	}

	@Test
	void scalingDarkensEachChannelIndependently() {
		assertEquals(0xFF102040, StyleUtil.scaleRgb(0xFF204080, 0.5));
	}

	@Test
	void alphaIsCarriedThroughUnscaled() {
		assertEquals(0x80, StyleUtil.scaleRgb(0x80FFFFFF, 0.25) >>> 24, "alpha 0x80 survives a 0.25 scale");
		assertEquals(0x00, StyleUtil.scaleRgb(0x00FFFFFF, 0.25) >>> 24, "a fully transparent colour stays transparent");
	}

	@Test
	void theShadowScaleUsedByTheTextFieldsDarkensToAQuarter() {
		assertEquals(0xFF3F3F3F, StyleUtil.scaleRgb(0xFFFFFFFF, 0.25), "white shadows to 0x3F, not 0x40");
		assertEquals(0xFF000000, StyleUtil.scaleRgb(0xFF000000, 0.25));
	}

	@Test
	void scalingTruncatesRatherThanRounds() {
		assertEquals(0xFF000000, StyleUtil.scaleRgb(0xFF010101, 0.5), "0x01 * 0.5 truncates to 0");
		assertEquals(0xFF010101, StyleUtil.scaleRgb(0xFF030303, 0.5), "0x03 * 0.5 truncates to 1");
	}

	@Test
	void scalingByZeroProducesBlackAtTheOriginalAlpha() {
		assertEquals(0xFF000000, StyleUtil.scaleRgb(0xFFAABBCC, 0.0));
	}

}
