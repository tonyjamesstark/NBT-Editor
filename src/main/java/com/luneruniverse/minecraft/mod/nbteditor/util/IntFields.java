package com.luneruniverse.minecraft.mod.nbteditor.util;

import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Reading and validating an integer a player is typing.
 *
 * <p>A filter runs on every keystroke, so it has to accept the halfway states a finished number
 * passes through: an empty field, and a bare sign with no digits after it yet. Rejecting those
 * makes the field impossible to type a negative number into.
 */
public class IntFields {
	
	/**
	 * @param min Read on each test, so a bound that moves -- a page count, a list size -- stays
	 *         current. Null for no lower bound.
	 * @param max As <code>min</code>, for the upper bound
	 * @param allowEmpty Whether an unfinished value is acceptable. False where the field feeds
	 *         something that must always have a number in it
	 */
	public static Predicate<String> intPredicate(Supplier<Integer> min, Supplier<Integer> max, boolean allowEmpty) {
		return str -> {
			if (str.isEmpty())
				return allowEmpty;
			if (str.equals("+"))
				return allowEmpty && (max == null || max.get() >= 0);
			if (str.equals("-"))
				return allowEmpty && (min == null || min.get() <= 0);
			try {
				int value = Integer.parseInt(str);
				return (min == null || min.get() <= value) && (max == null || value <= max.get());
			} catch (NumberFormatException e) {
				return false;
			}
		};
	}
	/** Fixed bounds. Use {@link Integer#MIN_VALUE} / {@link Integer#MAX_VALUE} for no bound. */
	public static Predicate<String> intPredicate(int min, int max, boolean allowEmpty) {
		return intPredicate(() -> min, () -> max, allowEmpty);
	}
	/** Any integer, in any state of being typed. */
	public static Predicate<String> intPredicate() {
		return intPredicate((Supplier<Integer>) null, null, true);
	}
	
	/** Null when <code>str</code> is not an int, including when it is too large to be one. */
	public static Integer parseOptionalInt(String str) {
		try {
			return Integer.parseInt(str);
		} catch (NumberFormatException e) {
			return null;
		}
	}
	public static int parseDefaultInt(String str, int defaultValue) {
		Integer output = parseOptionalInt(str);
		if (output == null)
			return defaultValue;
		return output;
	}
	
	private IntFields() {}
	
}
