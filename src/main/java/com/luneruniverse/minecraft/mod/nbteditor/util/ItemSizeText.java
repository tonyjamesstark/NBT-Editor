package com.luneruniverse.minecraft.mod.nbteditor.util;

import java.util.Optional;

import net.minecraft.ChatFormatting;

/**
 * Renders a byte count for the item size line in a tooltip.
 *
 * <p>The unit is either pinned by config or picked from the size itself. Each unit carries its own
 * suffix and colour, so adding one is a row here rather than another branch at the call site.
 */
public final class ItemSizeText {

	/** A unit, its threshold, and how the size reads once expressed in it. */
	public enum Unit {
		BYTES(1, "B", ChatFormatting.GREEN),
		KILOBYTES(1_000, "KB", ChatFormatting.YELLOW),
		MEGABYTES(1_000_000, "MB", ChatFormatting.RED),
		GIGABYTES(1_000_000_000, "GB", null);

		private final int magnitude;
		private final String suffix;
		private final ChatFormatting color;

		private Unit(int magnitude, String suffix, ChatFormatting color) {
			this.magnitude = magnitude;
			this.suffix = suffix;
			this.color = color;
		}

		public int getMagnitude() {
			return magnitude;
		}

		public String getSuffix() {
			return suffix;
		}

		/** Empty when the unit has no fixed colour and the caller should cycle one instead. */
		public Optional<ChatFormatting> getColor() {
			return Optional.ofNullable(color);
		}

		private static Unit ofMagnitude(int magnitude) {
			for (Unit unit : values()) {
				if (unit.magnitude == magnitude)
					return unit;
			}
			throw new IllegalStateException("Invalid magnitude!");
		}

		/** The largest unit the size fills. */
		private static Unit forSize(long size) {
			Unit largest = BYTES;
			for (Unit unit : values()) {
				if (size >= unit.magnitude)
					largest = unit;
			}
			return largest;
		}
	}

	/** A rendered size: the text to show, and the colour to show it in. */
	public record Rendered(String text, Optional<ChatFormatting> color) {}

	private ItemSizeText() {}

	/**
	 * @param configuredMagnitude the magnitude from the size config, or 0 to pick one from the size
	 * @throws IllegalStateException if the magnitude is neither 0 nor a known unit
	 */
	public static Rendered render(long size, int configuredMagnitude) {
		Unit unit = configuredMagnitude == 0 ? Unit.forSize(size) : Unit.ofMagnitude(configuredMagnitude);
		// Locale-sensitive, as it has always been: the decimal separator follows the client's locale.
		String amount = unit == Unit.BYTES ? Long.toString(size)
				: String.format("%.1f", (double) size / unit.getMagnitude());
		return new Rendered(amount + unit.getSuffix(), unit.getColor());
	}

}
