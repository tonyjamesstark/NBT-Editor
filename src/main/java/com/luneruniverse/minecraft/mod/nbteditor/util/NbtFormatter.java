package com.luneruniverse.minecraft.mod.nbteditor.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import com.luneruniverse.minecraft.mod.nbteditor.misc.MixinLink;
import com.luneruniverse.minecraft.mod.nbteditor.screens.ConfigScreen;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.SnbtGrammar;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.parsing.packrat.ErrorCollector;
import net.minecraft.util.parsing.packrat.NamedRule;
import net.minecraft.util.parsing.packrat.commands.Grammar;
import net.minecraft.util.parsing.packrat.commands.StringReaderParserState;

/**
 * Colours SNBT by running it through the grammar the game itself parses with.
 *
 * <p>Every rule the parser matches reports the span it claimed, and those spans become the
 * colours. The alternative, a second reader that knows what SNBT looks like, is a definition
 * that has to be kept in step with the game's: the hand-written one this replaced recognised
 * the SNBT of 1.20, so 26.2's hex and binary literals and underscore separators came out
 * string-coloured, and a builtin call such as {@code bool('x')} made it throw, which the
 * editor renders red and flags as unsafe.
 */
public class NbtFormatter {

	public static record FormatterResult(Component text, boolean isSuccess) {}

	@FunctionalInterface
	public interface Impl {
		Component format(String str) throws CommandSyntaxException;
		default FormatterResult formatSafely(String str) {
			try {
				return new FormatterResult(format(str), true);
			} catch (Exception e) {
				return new FormatterResult(Component.literal(str).withStyle(ChatFormatting.RED), false);
			}
		}
	}

	public static Impl FORMATTER = NbtFormatter::formatElement;


	private static final ChatFormatting NAME_COLOR = ChatFormatting.AQUA;
	private static final ChatFormatting STRING_COLOR = ChatFormatting.GREEN;
	private static final ChatFormatting NUMBER_COLOR = ChatFormatting.GOLD;
	private static final ChatFormatting TYPE_SUFFIX_COLOR = ChatFormatting.RED;
	private static final ChatFormatting OPERATOR_COLOR = ChatFormatting.LIGHT_PURPLE;

	public static final Map<String, Number> SPECIAL_NUMS = Map.of(
			"NaNd", Double.NaN,
			"Infinityd", Double.POSITIVE_INFINITY,
			"-Infinityd", Double.NEGATIVE_INFINITY,
			"NaNf", Float.NaN,
			"Infinityf", Float.POSITIVE_INFINITY,
			"-Infinityf", Float.NEGATIVE_INFINITY);

	private static final SimpleCommandExceptionType EXPECTED_VALUE =
			new SimpleCommandExceptionType(Component.translatableEscape("argument.nbt.expected.value"));
	private static final SimpleCommandExceptionType TRAILING_DATA =
			new SimpleCommandExceptionType(Component.translatableEscape("argument.nbt.trailing"));

	private static final ErrorCollector<StringReader> NO_ERRORS = new ErrorCollector.Nop<>();
	private static final Grammar<Tag> GRAMMAR = SnbtGrammar.createParser(NbtOps.INSTANCE);

	/** One span the parser claimed, and the name of the rule that claimed it. */
	private record ParsedSection(int start, int end, String rule) {}


	public static Component formatElement(String snbt) throws CommandSyntaxException {
		return format(snbt, ConfigScreen.isSpecialNumbers());
	}

	static Component format(String snbt, boolean allowSpecialNumbers) throws CommandSyntaxException {
		List<ParsedSection> sections = new ArrayList<>();

		StringReader reader = new StringReader(snbt);
		StringReaderParserState state = new StringReaderParserState(NO_ERRORS, reader) {
			@Override
			public <T> T parse(NamedRule<StringReader, T> rule) {
				int sectionMark = sections.size();
				int start = mark();

				T parsed = super.parse(rule);

				// A rule that failed has rewound the reader, so whatever its sub-rules recorded
				// describes text that ends up claimed by some other rule instead.
				if (parsed == null)
					sections.subList(sectionMark, sections.size()).clear();
				else
					sections.add(new ParsedSection(start, mark(), rule.name().name()));

				return parsed;
			}
		};

		Optional<Tag> nbt;
		if (allowSpecialNumbers)
			MixinLink.specialNumbers.add(Thread.currentThread());
		try {
			nbt = GRAMMAR.parse(state);
		} finally {
			if (allowSpecialNumbers)
				MixinLink.specialNumbers.remove(Thread.currentThread());
		}

		if (nbt.isEmpty())
			throw EXPECTED_VALUE.createWithContext(reader);
		reader.skipWhitespace();
		if (reader.canRead())
			throw TRAILING_DATA.createWithContext(reader);

		return color(snbt, sections, allowSpecialNumbers);
	}

	private static Component color(String snbt, List<ParsedSection> sections, boolean allowSpecialNumbers) {
		// Index to the color that starts there, or to null where one ends. Sub-rules land in the
		// list before the rules containing them, so an outer span never overwrites an inner one.
		TreeMap<Integer, ChatFormatting> colors = new TreeMap<>();

		for (ParsedSection section : sections) {
			// These two recolor a span that is already there rather than adding one: a builtin's
			// name and a map key are both read as strings before anything knows what they are.
			if (section.rule().equals("arguments")) {
				recolor(colors, colors.floorKey(section.start() - 2), OPERATOR_COLOR);
				continue;
			}
			if (section.rule().equals("map_key")) {
				recolor(colors, colors.ceilingKey(section.start()), NAME_COLOR);
				continue;
			}

			ChatFormatting color = switch (section.rule()) {
				case "integer_literal", "float_literal" -> NUMBER_COLOR;
				case "integer_suffix", "float_type_suffix", "array_prefix" -> TYPE_SUFFIX_COLOR;
				case "single_quoted_string_contents", "double_quoted_string_contents" -> STRING_COLOR;
				case "unquoted_string" -> unquotedColor(snbt, section, allowSpecialNumbers, colors);
				default -> null;
			};
			if (color == null)
				continue;

			colors.put(section.start(), color);
			colors.put(section.end(), null);
		}
		colors.put(snbt.length(), null);

		MutableComponent output = Component.literal("");
		int start = 0;
		ChatFormatting pending = null;
		for (Map.Entry<Integer, ChatFormatting> color : colors.entrySet()) {
			int end = color.getKey();
			MutableComponent section = Component.literal(snbt.substring(start, end));
			output.append(pending == null ? section : section.withStyle(pending));
			start = end;
			pending = color.getValue();
		}
		return output;
	}

	private static ChatFormatting unquotedColor(String snbt, ParsedSection section,
			boolean allowSpecialNumbers, TreeMap<Integer, ChatFormatting> colors) {
		String value = snbt.substring(section.start(), section.end()).stripLeading();
		if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false"))
			return NUMBER_COLOR;
		if (allowSpecialNumbers && SPECIAL_NUMS.containsKey(value)) {
			// Their trailing d or f is a type suffix like any other number's.
			colors.put(section.end() - 1, TYPE_SUFFIX_COLOR);
			return NUMBER_COLOR;
		}
		return STRING_COLOR;
	}

	private static void recolor(TreeMap<Integer, ChatFormatting> colors, Integer at, ChatFormatting color) {
		if (at != null)
			colors.put(at, color);
	}

}
