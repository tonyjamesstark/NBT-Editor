package com.luneruniverse.minecraft.mod.nbteditor.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.luneruniverse.minecraft.mod.nbteditor.util.TextSearch.Match;

/**
 * Pins the matching behind the find-and-replace overlay of a multi-line text field. The backward
 * regex search and the group-reference expansion are the subtle parts; the rest is here so that a
 * future rewrite of either has something to fail against.
 */
class TextSearchTest {

	@Test
	void literalSearchFindsTheFirstOccurrenceAtOrAfterTheStart() {
		Match match = TextSearch.find("hello world", "o", 0, false, false);
		assertEquals(4, match.start());
		assertEquals(5, match.end());
		assertEquals(7, TextSearch.find("hello world", "o", 5, false, false).start());
	}

	@Test
	void literalSearchPastTheLastOccurrenceFindsNothing() {
		assertNull(TextSearch.find("hello world", "o", 8, false, false));
	}

	@Test
	void backwardLiteralSearchFindsTheLastOccurrenceEndingByTheStart() {
		assertEquals(7, TextSearch.find("hello world", "o", 11, true, false).start());
		assertEquals(4, TextSearch.find("hello world", "o", 5, true, false).start());
	}

	@Test
	void backwardLiteralSearchWillNotReturnAnOccurrenceStraddlingTheStart() {
		assertNull(TextSearch.find("abcd", "bc", 2, true, false), "bc ends at 3, past the start");
	}

	@Test
	void regexSearchFindsTheFirstMatch() {
		Match match = TextSearch.find("a1b22c", "[0-9]+", 0, false, true);
		assertEquals(1, match.start());
		assertEquals(2, match.end());
	}

	@Test
	void backwardRegexSearchFindsTheLastMatchAndLeavesTheMatcherOnIt() {
		Match match = TextSearch.find("a1b22c", "([0-9]+)", 6, true, true);
		assertEquals(3, match.start());
		assertEquals(5, match.end());
		assertEquals("22", match.matcher().group(1), "the matcher has to be walked back to the last match");
	}

	@Test
	void aPatternThatMatchesEmptyFindsNothing() {
		assertNull(TextSearch.find("aaa", "b*", 0, false, true), "an empty match would never advance");
	}

	@Test
	void anUnparsablePatternFindsNothing() {
		assertNull(TextSearch.find("aaa", "[", 0, false, true));
	}

	@Test
	void replacementResolvesGroupReferences() {
		Match match = TextSearch.find("john smith", "(\\w+) (\\w+)", 0, false, true);
		assertEquals("smith john", TextSearch.expandReplacement(match, "$2 $1"));
	}

	@Test
	void replacementExcludesTheTextPrecedingTheMatch() {
		Match match = TextSearch.find("x john smith", "(\\w+) (\\w+)", 2, false, true);
		assertNotNull(match);
		assertEquals("smith john", TextSearch.expandReplacement(match, "$2 $1"), "the leading \"x \" is not part of the replacement");
	}

	@Test
	void replacementReferencingAGroupThatIsNotThereIsUsedLiterally() {
		Match match = TextSearch.find("john smith", "(\\w+) (\\w+)", 0, false, true);
		assertEquals("$9", TextSearch.expandReplacement(match, "$9"));
		assertEquals("a$", TextSearch.expandReplacement(match, "a$"));
	}

}
