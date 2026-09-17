package com.luneruniverse.minecraft.mod.nbteditor.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Locating a literal or regex occurrence in a string, forwards or backwards from a position, and
 * expanding a replacement for the occurrence that was located.
 *
 * <p>This is find-and-replace with the widget taken away: no cursor, no selection, no undo. A
 * search that cannot succeed -- an unparsable pattern, a pattern that matches nothing, a pattern
 * whose match is empty -- is a null result rather than an exception.
 */
public class TextSearch {
	
	/**
	 * A located occurrence.
	 *
	 * @param start The index of the first character
	 * @param end The index after the last character
	 * @param matcher Positioned on this occurrence, for {@link #expandReplacement}; null for a
	 *         literal search, which has no groups to resolve
	 */
	public record Match(int start, int end, Matcher matcher) {}
	
	/**
	 * @param text The string to search
	 * @param query A literal substring, or a regex when <code>regex</code> is set
	 * @param from Where to search from; the first occurrence at or after it, or when searching
	 *         backwards the last occurrence ending at or before it
	 * @param backward Whether to search towards the start of <code>text</code> instead
	 * @param regex Whether to treat <code>query</code> as a pattern rather than a literal
	 * @return The occurrence, or null if there is none
	 */
	public static Match find(String text, String query, int from, boolean backward, boolean regex) {
		if (regex)
			return findRegex(backward ? text.substring(0, from) : text, query, from, backward);
		int start = backward ? text.substring(0, from).lastIndexOf(query) : text.indexOf(query, from);
		if (start == -1)
			return null;
		return new Match(start, start + query.length(), null);
	}
	
	private static Match findRegex(String text, String query, int from, boolean backward) {
		try {
			Matcher matcher = Pattern.compile(query).matcher(text);
			if (!matcher.find(backward ? 0 : from))
				return null;
			// Backwards means the last of them, which a Matcher can only reach by walking forwards,
			// so count the way there and then walk it again to leave the matcher on that occurrence.
			int numMatches = 0;
			int start;
			int end;
			do {
				numMatches++;
				start = matcher.start();
				end = matcher.end();
				if (start == end)
					return null;
			} while (backward && matcher.find());
			if (backward) {
				matcher.reset();
				for (int i = 0; i < numMatches; i++)
					matcher.find();
			}
			return new Match(start, end, matcher);
		} catch (PatternSyntaxException e) {
			return null;
		}
	}
	
	/**
	 * @param match From a regex {@link #find}
	 * @param replacement May reference groups of the match, as <code>$1</code>
	 * @return <code>replacement</code> with its group references resolved, or unchanged if it
	 *         references a group the match does not have
	 */
	public static String expandReplacement(Match match, String replacement) {
		StringBuilder expanded = new StringBuilder();
		try {
			match.matcher().appendReplacement(expanded, replacement);
			// appendReplacement also copies everything preceding the match; drop it.
			expanded.delete(0, match.start());
		} catch (IllegalArgumentException | IndexOutOfBoundsException e) {
			return replacement;
		}
		return expanded.toString();
	}
	
	private TextSearch() {}
	
}
