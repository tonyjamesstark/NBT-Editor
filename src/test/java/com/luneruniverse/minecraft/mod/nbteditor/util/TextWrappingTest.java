package com.luneruniverse.minecraft.mod.nbteditor.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;

import java.util.List;
import java.util.function.ToIntFunction;

import org.junit.jupiter.api.Test;

/** One unit of width per character, so the expected lines can be read off the assertions. */
public class TextWrappingTest {
	
	private static final ToIntFunction<String> WIDTH = String::length;
	
	private static List<String> wrap(String text, int maxWidth) {
		return TextWrapping.wrap(text, maxWidth, WIDTH);
	}
	
	@Test
	public void textThatFitsStaysOnOneLine() {
		assertEquals(List.of("hello"), wrap("hello", 10));
	}
	
	@Test
	public void emptyTextWrapsToNoLines() {
		assertEquals(List.of(), wrap("", 10));
	}
	
	@Test
	public void aSpaceSurvivesWhenBothSidesShareALine() {
		assertEquals(List.of("a b"), wrap("a b", 10));
	}
	
	@Test
	public void aSpaceIsDroppedWhenItBecomesALineBreak() {
		assertEquals(List.of("aaa", "bbb"), wrap("aaa bbb", 4));
	}
	
	@Test
	public void caseChangesAreBreakingSpotsButNotBreaks() {
		assertEquals(List.of("NBTEditor"), wrap("NBTEditor", 100));
	}
	
	@Test
	public void aRunOfCapitalsKeepsItsLastLetterForTheNextWord() {
		assertEquals(List.of("NBT", "Editor"), wrap("NBTEditor", 6));
	}
	
	@Test
	public void lowerToUpperBreaksBetweenTheTwo() {
		assertEquals(List.of("Minion", "YT"), wrap("MinionYT", 6));
	}
	
	@Test
	public void aWordWiderThanTheLineIsSplitMidWord() {
		assertEquals(List.of("NBT", "Edit", "or"), wrap("NBTEditor", 5));
	}
	
	@Test
	public void aCharacterWiderThanTheLineGetsALineToItself() {
		// Clamping maxWidth to the width of "ww" does not save this: a glyph can be wider than
		// two of them. Splitting used to take zero characters off the front here and spin.
		ToIntFunction<String> wideW = s -> s.chars().map(c -> c == 'W' ? 50 : 1).sum();
		assertTimeoutPreemptively(Duration.ofSeconds(2),
				() -> assertEquals(List.of("W", "W"), TextWrapping.wrap("WW", 2, wideW)));
	}
	
	@Test
	public void maxWidthIsRaisedToTheWidthOfTwoCharacters() {
		assertEquals(List.of("a", "b", "c", "d"), wrap("abcd", 0));
	}
	
}
