package com.luneruniverse.minecraft.mod.nbteditor.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Pins the character filter that used to live in {@code MVMisc}. Every text field in the mod
 * runs typed and pasted text through it, so it decides what a user can put in an item name.
 */
class TextUtilTest {

	private static final char SECTION_SIGN = '\u00A7';

	@Test
	void ordinaryPrintableCharactersAreValid() {
		assertTrue(TextUtil.isValidChar('a'));
		assertTrue(TextUtil.isValidChar('Z'));
		assertTrue(TextUtil.isValidChar('0'));
		assertTrue(TextUtil.isValidChar(' '), "space is the lowest valid character");
		assertTrue(TextUtil.isValidChar('~'));
	}

	@Test
	void theSectionSignIsRejected() {
		assertFalse(TextUtil.isValidChar(SECTION_SIGN), "the section sign would inject raw formatting");
	}

	@Test
	void controlCharactersAndDeleteAreRejected() {
		assertFalse(TextUtil.isValidChar('\n'));
		assertFalse(TextUtil.isValidChar('\t'));
		assertFalse(TextUtil.isValidChar('\0'));
		assertFalse(TextUtil.isValidChar((char) 31), "the character just below space");
		assertFalse(TextUtil.isValidChar((char) 127), "DEL");
	}

	@Test
	void charactersAboveDeleteAreValid() {
		assertTrue(TextUtil.isValidChar((char) 128));
		assertTrue(TextUtil.isValidChar('\u00E9'), "e-acute");
	}

	@Test
	void strippingRemovesOnlyTheInvalidCharacters() {
		assertEquals("abc", TextUtil.stripInvalidChars("a" + SECTION_SIGN + "bc", false));
		assertEquals("hello world", TextUtil.stripInvalidChars("hello\0 world", false));
	}

	@Test
	void lineBreaksSurviveOnlyWhenAllowed() {
		assertEquals("ab", TextUtil.stripInvalidChars("a\nb", false));
		assertEquals("a\nb", TextUtil.stripInvalidChars("a\nb", true));
	}

	@Test
	void allowingLineBreaksDoesNotSpareOtherControlCharacters() {
		assertEquals("a\nb", TextUtil.stripInvalidChars("a\n\tb", true), "tab is still dropped");
		assertEquals("a\nb", TextUtil.stripInvalidChars("a\r\nb", true), "carriage return is still dropped");
	}

	@Test
	void strippingIsIdentityWhenNothingIsInvalid() {
		String clean = "Diamond Sword #1";
		assertEquals(clean, TextUtil.stripInvalidChars(clean, false));
		assertEquals("", TextUtil.stripInvalidChars("", true));
	}

	/**
	 * Identifier.tryParse reads all three of these as minecraft:custom_name, so all three have to
	 * qualify to the same string -- the bare-colon one used to come back untouched, because it
	 * already contained a colon.
	 */
	@Test
	void everySpellingOfAComponentNameQualifiesTheSame() {
		assertEquals("minecraft:custom_name", TextUtil.addNamespace("custom_name"));
		assertEquals("minecraft:custom_name", TextUtil.addNamespace(":custom_name"));
		assertEquals("minecraft:custom_name", TextUtil.addNamespace("minecraft:custom_name"));
		assertEquals("!minecraft:custom_name", TextUtil.addNamespace("!custom_name"));
		assertEquals("!minecraft:custom_name", TextUtil.addNamespace("!:custom_name"));
		assertEquals("!minecraft:custom_name", TextUtil.addNamespace("!minecraft:custom_name"));
		assertEquals("othermod:thing", TextUtil.addNamespace("othermod:thing"), "another namespace is left alone");
	}

}
