package com.luneruniverse.minecraft.mod.nbteditor.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.network.chat.Style;

/**
 * The formatter colours SNBT by the spans 26.2's own grammar claims, so these pin the forms the
 * hand-written reader it replaced got wrong: every number form 1.21.5 added came out
 * string-coloured, because none of its patterns allowed an {@code 0x} or {@code 0b} prefix or an
 * underscore separator, and a builtin call threw, because a compound stopped at the first thing
 * that was neither a comma nor a brace.
 */
class NbtFormatterTest {

	@Test
	void hexBinaryAndSeparatedLiteralsAreNumbers() throws CommandSyntaxException {
		assertEquals("[- {][aqua a][- :][gold 0x1F][- }]", format("{a:0x1F}"));
		assertEquals("[- {][aqua a][- :][gold 0b101][- }]", format("{a:0b101}"));
		assertEquals("[- {][aqua a][- :][gold 1_000][- }]", format("{a:1_000}"));
	}

	@Test
	void aBuiltinCallFormatsRatherThanFailing() throws CommandSyntaxException {
		String uuid = "00000000-0000-0000-0000-000000000000";
		assertEquals("[- {][aqua a][- :][light_purple uuid][- ('][green " + uuid + "][- ')}]",
				format("{a:uuid('" + uuid + "')}"));
	}

	@Test
	void aTypeSuffixIsColouredApartFromItsNumber() throws CommandSyntaxException {
		assertEquals("[- {][aqua a][- :][gold 1][red b][- }]", format("{a:1b}"));
		assertEquals("[- {][aqua a][- :][gold 1.5][red f][- }]", format("{a:1.5f}"));
	}

	@Test
	void anArrayPrefixIsColouredLikeATypeSuffix() throws CommandSyntaxException {
		assertEquals("[- {][aqua a][- :[][red I][- ;][gold 1][- ,][gold 2][- ]}]", format("{a:[I;1,2]}"));
	}

	@Test
	void aQuotedKeyIsStillAKey() throws CommandSyntaxException {
		assertEquals("[- {\"][aqua quoted key][- \":][gold 1][- }]", format("{\"quoted key\":1}"));
		assertEquals("[- {][aqua a][- :\"][green hi][- \"}]", format("{a:\"hi\"}"));
	}

	@Test
	void booleansReadAsNumbersRatherThanStrings() throws CommandSyntaxException {
		assertEquals("[- {][aqua a][- :][gold true][- }]", format("{a:true}"));
	}

	@Test
	void incompleteSnbtIsRejected() {
		assertThrows(CommandSyntaxException.class, () -> NbtFormatter.format("{a:", false));
	}

	@Test
	void anythingAfterTheValueIsRejected() {
		assertThrows(CommandSyntaxException.class, () -> NbtFormatter.format("{a:1} trailing", false));
	}

	/**
	 * Upstream got both of these wrong twice against a hand-written reader: 1e1 was called a
	 * number where it was not one, and a number too big for its suffix was called one where it
	 * was not either. Neither is a judgement this formatter makes -- the grammar decides, and it
	 * is the same grammar that decides whether the SNBT is accepted at all.
	 */
	@Test
	void whatCountsAsANumberIsTheParsersAnswer() throws CommandSyntaxException {
		assertEquals("[- {][aqua a][- :][gold 1e1][- }]", format("{a:1e1}"), "26.2 reads this as a double");
		assertThrows(CommandSyntaxException.class, () -> NbtFormatter.format("{a:999999999999b}", false));
		assertThrows(CommandSyntaxException.class, () -> NbtFormatter.format("{a:99999999999999999999}", false));
	}

	/** Renders as {@code [<colour> <text>]} per run, with {@code -} for an uncoloured one. */
	private static String format(String snbt) throws CommandSyntaxException {
		StringBuilder out = new StringBuilder();
		NbtFormatter.format(snbt, false).visit((style, str) -> {
			out.append('[').append(style.getColor() == null ? "-" : style.getColor().serialize())
					.append(' ').append(str).append(']');
			return Optional.empty();
		}, Style.EMPTY);
		return out.toString();
	}

}
