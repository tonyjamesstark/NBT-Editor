package com.luneruniverse.minecraft.mod.nbteditor.util;

import java.util.Map;
import java.util.regex.Pattern;

import com.luneruniverse.minecraft.mod.nbteditor.misc.MixinLink;
import com.luneruniverse.minecraft.mod.nbteditor.screens.ConfigScreen;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import net.minecraft.network.chat.MutableComponent;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.TagType;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

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
	
	
	private static final SimpleCommandExceptionType TRAILING_DATA = new SimpleCommandExceptionType(Component.translatableEscape("argument.nbt.trailing"));
	private static final SimpleCommandExceptionType EXPECTED_KEY = new SimpleCommandExceptionType(Component.translatableEscape("argument.nbt.expected.key"));
	private static final SimpleCommandExceptionType EXPECTED_VALUE = new SimpleCommandExceptionType(Component.translatableEscape("argument.nbt.expected.value"));
	private static final DynamicCommandExceptionType ARRAY_INVALID = new DynamicCommandExceptionType(type -> Component.translatableEscape("argument.nbt.array.invalid", type));
    private static final Pattern DOUBLE_PATTERN_IMPLICIT = Pattern.compile("[-+]?(?:[0-9]+[.]|[0-9]*[.][0-9]+)(?:e[-+]?[0-9]+)?", 2);
    private static final Pattern DOUBLE_PATTERN = Pattern.compile("[-+]?(?:[0-9]+[.]?|[0-9]*[.][0-9]+)(?:e[-+]?[0-9]+)?d", 2);
    private static final Pattern FLOAT_PATTERN = Pattern.compile("[-+]?(?:[0-9]+[.]?|[0-9]*[.][0-9]+)(?:e[-+]?[0-9]+)?f", 2);
    private static final Pattern BYTE_PATTERN = Pattern.compile("[-+]?(?:0|[1-9][0-9]*)b", 2);
    private static final Pattern LONG_PATTERN = Pattern.compile("[-+]?(?:0|[1-9][0-9]*)l", 2);
    private static final Pattern SHORT_PATTERN = Pattern.compile("[-+]?(?:0|[1-9][0-9]*)s", 2);
    private static final Pattern INT_PATTERN = Pattern.compile("[-+]?(?:0|[1-9][0-9]*)");
	private static final ChatFormatting NAME_COLOR = ChatFormatting.AQUA;
	private static final ChatFormatting STRING_COLOR = ChatFormatting.GREEN;
	private static final ChatFormatting NUMBER_COLOR = ChatFormatting.GOLD;
	private static final ChatFormatting TYPE_SUFFIX_COLOR = ChatFormatting.RED;
	
	public static final Map<String, Number> SPECIAL_NUMS = Map.of(
			"NaNd", Double.NaN,
			"Infinityd", Double.POSITIVE_INFINITY,
			"-Infinityd", Double.NEGATIVE_INFINITY,
			"NaNf", Float.NaN,
			"Infinityf", Float.POSITIVE_INFINITY,
			"-Infinityf", Float.NEGATIVE_INFINITY);
	
	
	
	public static Component formatElement(StringReader reader) throws CommandSyntaxException {
		// Check list types
		int cursor = reader.getCursor();
		MixinLink.parseSpecialElement(reader);
		reader.setCursor(cursor);
		
		// Format
		NbtFormatter formatter = new NbtFormatter(reader);
		MutableComponent output = formatter.parseElement();
		output.append(formatter.skipWhitespace());
		if (reader.canRead())
			throw TRAILING_DATA.createWithContext(reader);
		return output;
	}
	public static Component formatElement(String str) throws CommandSyntaxException {
		return formatElement(new StringReader(str));
	}
	
	
	private StringReader reader;
	
	private NbtFormatter(StringReader reader) {
		this.reader = reader;
	}
	
	private MutableComponent skipWhitespace() {
		StringBuilder output = new StringBuilder();
		while (reader.canRead() && Character.isWhitespace(reader.peek()))
			output.append(reader.read());
		return Component.literal(output.toString());
	}
	
	private String readStringUntil(char terminator) throws CommandSyntaxException {
		final StringBuilder result = new StringBuilder();
		boolean escaped = false;
		while (reader.canRead()) {
			final char c = reader.read();
			if (escaped) {
				if (c == terminator || c == '\\') {
					result.append(c);
					escaped = false;
				} else {
					reader.setCursor(reader.getCursor() - 1);
					throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerInvalidEscape().createWithContext(reader,
							String.valueOf(c));
				}
			} else if (c == '\\') {
				escaped = true;
				result.append(c);
			} else if (c == terminator) {
				return result.toString();
			} else {
				result.append(c);
			}
		}
		
		throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerExpectedEndOfQuote().createWithContext(reader);
	}
	
	private String readString() throws CommandSyntaxException {
		if (!reader.canRead()) {
			return "";
		}
		final char next = reader.peek();
		if (StringReader.isQuotedStringStart(next)) {
			reader.skip();
			return next + readStringUntil(next) + next;
		}
		return reader.readUnquotedString();
	}
	
	private MutableComponent readString(ChatFormatting color) throws CommandSyntaxException {
		MutableComponent output = Component.literal("");
		output.append(this.skipWhitespace());
        if (!this.reader.canRead()) {
            throw EXPECTED_KEY.createWithContext(this.reader);
        }
        String str = this.readString();
        if (str.isEmpty())
        	return null;
        output.append(Component.literal(str).withStyle(color));
        return output;
	}
	
	private MutableComponent readQuotedString() throws CommandSyntaxException {
		MutableComponent output = Component.literal("");
		if (!reader.canRead()) {
			return output;
		}
		final char next = reader.peek();
		if (!StringReader.isQuotedStringStart(next)) {
			throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerExpectedStartOfQuote().createWithContext(reader);
		}
		reader.skip();
		output.append(Component.literal(this.readStringUntil(next)));
		return output;
	}
	
	private Map.Entry<Boolean, MutableComponent> readComma() {
		MutableComponent output = Component.literal("");
		output.append(this.skipWhitespace());
		if (this.reader.canRead() && this.reader.peek() == ',') {
			output.append(Component.literal(this.reader.read() + ""));
			output.append(this.skipWhitespace());
			return Map.entry(true, output);
		} else
			return Map.entry(false, output);
	}
	
	private MutableComponent readArray(TagType<?> arrayTypeReader, TagType<?> typeReader) throws CommandSyntaxException {
		MutableComponent output = Component.literal("");
		while (this.reader.peek() != ']') {
			output.append(this.parseElement());
			Map.Entry<Boolean, MutableComponent> comma = this.readComma();
			output.append(comma.getValue());
			if (!comma.getKey())
				break;
			if (this.reader.canRead())
				continue;
			throw EXPECTED_VALUE.createWithContext(this.reader);
		}
		output.append(this.expect(']'));
		return output;
	}
	
	private MutableComponent parseElement() throws CommandSyntaxException {
		MutableComponent output = Component.literal("");
		output.append(skipWhitespace());
		if (!this.reader.canRead()) {
			throw EXPECTED_VALUE.createWithContext(this.reader);
		}
		char c = this.reader.peek();
		if (c == '{') {
			output.append(this.parseCompound());
		} else if (c == '[') {
			output.append(this.parseArray());
		} else {
			output.append(this.parseElementPrimitive());
		}
		return output;
	}
	
	private MutableComponent parseCompound() throws CommandSyntaxException {
		MutableComponent output = Component.literal("");
		output.append(this.expect('{'));
		this.reader.skipWhitespace();
		while (this.reader.canRead() && this.reader.peek() != '}') {
			int i = this.reader.getCursor();
			MutableComponent string = this.readString(NAME_COLOR);
			if (string == null) {
				this.reader.setCursor(i);
				throw EXPECTED_KEY.createWithContext(this.reader);
			}
			output.append(string);
			output.append(this.expect(':'));
			output.append(this.parseElement());
			Map.Entry<Boolean, MutableComponent> comma = this.readComma();
			output.append(comma.getValue());
			if (!comma.getKey())
				break;
			if (this.reader.canRead())
				continue;
			throw EXPECTED_KEY.createWithContext(this.reader);
		}
		output.append(this.expect('}'));
		return output;
	}
	
	private MutableComponent parseArray() throws CommandSyntaxException {
		if (this.reader.canRead(3) && !StringReader.isQuotedStringStart(this.reader.peek(1))
				&& this.reader.peek(2) == ';') {
			return this.parseElementPrimitiveArray();
		}
		return this.parseList();
	}
	
	private MutableComponent parseElementPrimitiveArray() throws CommandSyntaxException {
		MutableComponent output = Component.literal("");
		output.append(this.expect('['));
		int i = this.reader.getCursor();
		char c = this.reader.read();
		output.append(Component.literal(c + "").withStyle(TYPE_SUFFIX_COLOR));
		output.append(Component.literal(this.reader.read() + ""));
		output.append(this.skipWhitespace());
		if (!this.reader.canRead()) {
			throw EXPECTED_VALUE.createWithContext(this.reader);
		}
		if (c == 'B') {
			output.append(this.readArray(ByteArrayTag.TYPE, ByteTag.TYPE));
			return output;
		}
		if (c == 'L') {
			output.append(this.readArray(LongArrayTag.TYPE, LongTag.TYPE));
			return output;
		}
		if (c == 'I') {
			output.append(this.readArray(IntArrayTag.TYPE, IntTag.TYPE));
			return output;
		}
		this.reader.setCursor(i);
		throw ARRAY_INVALID.createWithContext(this.reader, String.valueOf(c));
	}
	
	private MutableComponent parseList() throws CommandSyntaxException {
		MutableComponent output = Component.literal("");
		output.append(this.expect('['));
		output.append(this.skipWhitespace());
		if (!this.reader.canRead()) {
			throw EXPECTED_VALUE.createWithContext(this.reader);
		}
		while (this.reader.peek() != ']') {
			MutableComponent nbtElement = this.parseElement();
			output.append(nbtElement);
			Map.Entry<Boolean, MutableComponent> comma = this.readComma();
			output.append(comma.getValue());
			if (!comma.getKey())
				break;
			if (this.reader.canRead())
				continue;
			throw EXPECTED_VALUE.createWithContext(this.reader);
		}
		output.append(this.expect(']'));
		return output;
	}
	
	private MutableComponent parseElementPrimitive() throws CommandSyntaxException {
		MutableComponent output = Component.literal("");
		output.append(this.skipWhitespace());
		int i = this.reader.getCursor();
		char quote = this.reader.peek();
		if (StringReader.isQuotedStringStart(quote)) {
			output.append(Component.literal(quote + "").withStyle(STRING_COLOR))
					.append(this.readQuotedString().withStyle(STRING_COLOR))
					.append(Component.literal(quote + "").withStyle(STRING_COLOR));
			return output;
		}
		String string = this.reader.readUnquotedString();
		if (string.isEmpty()) {
			this.reader.setCursor(i);
			throw EXPECTED_VALUE.createWithContext(this.reader);
		}
		return this.parsePrimitive(string);
	}
	
	private MutableComponent parsePrimitive(String input) {
		try {
			MutableComponent numberInput = input.isEmpty() ? Component.literal("") :
				Component.literal(input.substring(0, input.length() - 1)).withStyle(NUMBER_COLOR)
					.append(Component.literal(input.substring(input.length() - 1)).withStyle(TYPE_SUFFIX_COLOR));
			if (FLOAT_PATTERN.matcher(input).matches()) {
				return numberInput;
			}
			if (BYTE_PATTERN.matcher(input).matches()) {
				return numberInput;
			}
			if (LONG_PATTERN.matcher(input).matches()) {
				return numberInput;
			}
			if (SHORT_PATTERN.matcher(input).matches()) {
				return numberInput;
			}
			if (INT_PATTERN.matcher(input).matches()) {
				return Component.literal(input).withStyle(NUMBER_COLOR);
			}
			if (DOUBLE_PATTERN.matcher(input).matches()) {
				return numberInput;
			}
			if (DOUBLE_PATTERN_IMPLICIT.matcher(input).matches()) {
				return Component.literal(input).withStyle(NUMBER_COLOR);
			}
			if ("true".equalsIgnoreCase(input)) {
				return Component.literal(input).withStyle(NUMBER_COLOR);
			}
			if ("false".equalsIgnoreCase(input)) {
				return Component.literal(input).withStyle(NUMBER_COLOR);
			}
			if (ConfigScreen.isSpecialNumbers() && SPECIAL_NUMS.containsKey(input))
				return numberInput;
		} catch (NumberFormatException numberFormatException) {
			// empty catch block
		}
		return Component.literal(input).withStyle(STRING_COLOR);
	}
	
	private MutableComponent expect(char c) throws CommandSyntaxException {
		MutableComponent output = Component.literal("");
		output.append(skipWhitespace());
		this.reader.expect(c);
		output.append(Component.literal(c + ""));
		return output;
	}
	
}
