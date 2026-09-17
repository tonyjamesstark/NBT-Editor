package com.luneruniverse.minecraft.mod.nbteditor.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

public class IntFieldsTest {
	
	@Test
	public void parseOptionalIntReadsAnInt() {
		assertEquals(12, IntFields.parseOptionalInt("12"));
		assertEquals(-12, IntFields.parseOptionalInt("-12"));
	}
	
	@Test
	public void parseOptionalIntRejectsWhatIsNotOne() {
		assertNull(IntFields.parseOptionalInt(""));
		assertNull(IntFields.parseOptionalInt("abc"));
		assertNull(IntFields.parseOptionalInt("1.5"));
	}
	
	@Test
	public void anIntTooLargeToHoldIsNotAnInt() {
		assertNull(IntFields.parseOptionalInt("99999999999"));
	}
	
	@Test
	public void parseDefaultIntFallsBack() {
		assertEquals(7, IntFields.parseDefaultInt("7", -1));
		assertEquals(-1, IntFields.parseDefaultInt("seven", -1));
	}
	
	@Test
	public void theUnboundedFilterTakesAnyInteger() {
		Predicate<String> any = IntFields.intPredicate();
		assertTrue(any.test("0"));
		assertTrue(any.test("-2147483648"));
		assertFalse(any.test("abc"));
		assertFalse(any.test("1.5"));
	}
	
	@Test
	public void halfTypedValuesPassWhenEmptyIsAllowed() {
		Predicate<String> any = IntFields.intPredicate();
		assertTrue(any.test(""));
		assertTrue(any.test("+"));
		assertTrue(any.test("-"));
	}
	
	@Test
	public void halfTypedValuesFailWhenEmptyIsNot() {
		Predicate<String> strict = IntFields.intPredicate(0, 10, false);
		assertFalse(strict.test(""));
		assertFalse(strict.test("+"));
		assertFalse(strict.test("-"));
	}
	
	@Test
	public void boundsAreInclusive() {
		Predicate<String> zeroToTen = IntFields.intPredicate(0, 10, false);
		assertTrue(zeroToTen.test("0"));
		assertTrue(zeroToTen.test("10"));
		assertFalse(zeroToTen.test("-1"));
		assertFalse(zeroToTen.test("11"));
	}
	
	@Test
	public void aBareSignIsRejectedWhenNoValueOfThatSignCouldFit() {
		// Typing "-" into a field that only takes positives can never become a valid value
		assertFalse(IntFields.intPredicate(1, 10, true).test("-"));
		assertFalse(IntFields.intPredicate(-10, -1, true).test("+"));
		assertTrue(IntFields.intPredicate(-10, 10, true).test("-"));
	}
	
	@Test
	public void suppliedBoundsAreReadOnEveryTest() {
		int[] max = {5};
		Predicate<String> upTo = IntFields.intPredicate(() -> 0, () -> max[0], false);
		assertFalse(upTo.test("7"));
		max[0] = 10;
		assertTrue(upTo.test("7"));
	}
	
}
