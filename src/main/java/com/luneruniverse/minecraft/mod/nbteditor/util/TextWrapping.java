package com.luneruniverse.minecraft.mod.nbteditor.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

/**
 * Breaking a label into lines that fit a width.
 *
 * <p>Minecraft's own wrapping breaks on spaces only. The editor labels things with identifiers
 * and class-ish names that have no spaces at all, so this also breaks where the capitalisation
 * changes: <code>NBTEditor</code> becomes <code>NBT</code> and <code>Editor</code> rather than
 * one unbreakable run or four useless letters.
 */
public class TextWrapping {
	
	/**
	 * @param width How wide a string renders. Taking this as a function is what keeps the class
	 *         off the font, and so testable.
	 * @param maxWidth Raised to the width of <code>ww</code> when it is narrower, because a line
	 *         that cannot hold two characters cannot be filled
	 */
	public static List<String> wrap(String text, int maxWidth, ToIntFunction<String> width) {
		maxWidth = Math.max(maxWidth, width.applyAsInt("ww"));
		
		// Split into breaking spots
		List<String> parts = new ArrayList<>();
		List<Integer> spaces = new ArrayList<>();
		StringBuilder currentPart = new StringBuilder();
		boolean wasUpperCase = false;
		for (char c : text.toCharArray()) {
			if (c == ' ') {
				wasUpperCase = false;
				parts.add(currentPart.toString());
				currentPart.setLength(0);
				spaces.add(parts.size());
				continue;
			}
			
			boolean upperCase = Character.isUpperCase(c);
			if (upperCase != wasUpperCase && !currentPart.isEmpty()) { // Handle NBTEditor; output NBT, Editor; not N, B, T, Editor AND Handle MinionYT; output Minion YT
				if (wasUpperCase) {
					parts.add(currentPart.substring(0, currentPart.length() - 1));
					currentPart.delete(0, currentPart.length() - 1);
				} else {
					parts.add(currentPart.toString());
					currentPart.setLength(0);
				}
			}
			wasUpperCase = upperCase;
			currentPart.append(c);
		}
		if (!currentPart.isEmpty())
			parts.add(currentPart.toString());
		
		// Generate lines, maximizing the number of parts per line
		List<String> lines = new ArrayList<>();
		String line = "";
		int i = 0;
		for (String part : parts) {
			String partAddition = (!line.isEmpty() && spaces.contains(i) ? " " : "") + part;
			if (width.applyAsInt(line + partAddition) > maxWidth) {
				if (!line.isEmpty()) {
					lines.add(line);
					line = "";
				}
				
				if (width.applyAsInt(part) > maxWidth) {
					while (true) {
						int numChars = 1;
						while (width.applyAsInt(part.substring(0, numChars)) < maxWidth)
							numChars++;
						numChars--;
						lines.add(part.substring(0, numChars));
						part = part.substring(numChars);
						if (width.applyAsInt(part) < maxWidth) {
							line = part;
							break;
						}
					}
				} else
					line = part;
			} else
				line += partAddition;
			i++;
		}
		if (!line.isEmpty())
			lines.add(line);
		
		return lines;
	}
	
	private TextWrapping() {}
	
}
