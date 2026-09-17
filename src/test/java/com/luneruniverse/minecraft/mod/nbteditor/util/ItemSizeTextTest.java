package com.luneruniverse.minecraft.mod.nbteditor.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.luneruniverse.minecraft.mod.nbteditor.util.ItemSizeText.Rendered;
import com.luneruniverse.minecraft.mod.nbteditor.util.ItemSizeText.Unit;

import net.minecraft.ChatFormatting;

/**
 * Pins the item size readout that used to live inline in {@code MixinLink.modifyTooltip}.
 *
 * <p>Rendering uses the default locale for its decimal separator, which is long-standing
 * behaviour, so these tests pin the locale rather than assume the machine's.
 */
class ItemSizeTextTest {

	private Locale original;

	@BeforeEach
	void pinLocale() {
		original = Locale.getDefault();
		Locale.setDefault(Locale.US);
	}

	@AfterEach
	void restoreLocale() {
		Locale.setDefault(original);
	}

	@Test
	void bytesRenderWholeAndGreen() {
		Rendered rendered = ItemSizeText.render(512, 0);
		assertEquals("512B", rendered.text(), "a byte count must not gain a decimal point");
		assertEquals(Optional.of(ChatFormatting.GREEN), rendered.color());
	}

	@Test
	void theAutoUnitClimbsWithTheSize() {
		assertEquals("0B", ItemSizeText.render(0, 0).text());
		assertEquals("999B", ItemSizeText.render(999, 0).text());
		assertEquals("1.0KB", ItemSizeText.render(1_000, 0).text());
		assertEquals("999.9KB", ItemSizeText.render(999_949, 0).text());
		assertEquals("1.0MB", ItemSizeText.render(1_000_000, 0).text());
		assertEquals("1.0GB", ItemSizeText.render(1_000_000_000, 0).text());
	}

	@Test
	void eachAutoUnitCarriesItsOwnColour() {
		assertEquals(Optional.of(ChatFormatting.GREEN), ItemSizeText.render(1, 0).color());
		assertEquals(Optional.of(ChatFormatting.YELLOW), ItemSizeText.render(1_000, 0).color());
		assertEquals(Optional.of(ChatFormatting.RED), ItemSizeText.render(1_000_000, 0).color());
		assertEquals(Optional.empty(), ItemSizeText.render(1_000_000_000, 0).color(),
				"gigabytes cycle their colour rather than pinning one");
	}

	@Test
	void aConfiguredUnitOverridesTheSize() {
		assertEquals("2048B", ItemSizeText.render(2048, 1).text());
		assertEquals("2.0KB", ItemSizeText.render(2048, 1_000).text());
		assertEquals("0.0MB", ItemSizeText.render(2048, 1_000_000).text());
	}

	@Test
	void aSizeAboveEveryThresholdStaysInGigabytes() {
		Rendered rendered = ItemSizeText.render(9_000_000_000L, 0);
		assertTrue(rendered.text().endsWith("GB"), "expected gigabytes, got " + rendered.text());
		assertEquals(Optional.empty(), rendered.color());
	}

	@Test
	void anUnknownMagnitudeIsRejected() {
		IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> ItemSizeText.render(1, 500));
		assertEquals("Invalid magnitude!", thrown.getMessage());
	}

	@Test
	void everyUnitIsReachableByItsOwnMagnitude() {
		for (Unit unit : Unit.values())
			assertTrue(ItemSizeText.render(unit.getMagnitude(), unit.getMagnitude()).text().endsWith(unit.getSuffix()));
	}

}
