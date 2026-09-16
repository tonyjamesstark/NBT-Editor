package com.luneruniverse.minecraft.mod.nbteditor.util;

import java.awt.Color;
import java.util.Locale;
import java.util.Objects;

import com.luneruniverse.minecraft.mod.nbteditor.localnbt.LocalBlock;
import com.luneruniverse.minecraft.mod.nbteditor.localnbt.LocalEntity;
import com.luneruniverse.minecraft.mod.nbteditor.localnbt.LocalItem;
import com.luneruniverse.minecraft.mod.nbteditor.localnbt.LocalNBT;

import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.DyeColor;

public class StyleUtil {
	
	public static final boolean SHADOW_COLOR_EXISTS = true;
	
	/** Multiplies an ARGB colour's channels, keeping its alpha. Used to darken text into its shadow. */
	public static int scaleRgb(int argb, double scale) {
		Color color = new Color(argb, true);
		int r = (int) (color.getRed() * scale);
		int g = (int) (color.getGreen() * scale);
		int b = (int) (color.getBlue() * scale);
		return new Color(r, g, b, color.getAlpha()).getRGB();
	}
	
	public static final Style RESET_STYLE = Style.EMPTY.withColor(ChatFormatting.WHITE)
			.withBold(false).withItalic(false).withUnderlined(false).withStrikethrough(false).withObfuscated(false);
	
	public static Style getBaseNameStyle(LocalNBT localNBT, boolean itemName) {
		Style baseNameStyle = Style.EMPTY;
		if (localNBT instanceof LocalItem item) {
			if (!itemName)
				baseNameStyle = baseNameStyle.applyFormat(ChatFormatting.ITALIC);
			baseNameStyle = baseNameStyle.applyFormat(item.getEditableItem().getRarity().color);
		} else if (localNBT instanceof LocalBlock)
			;
		else if (localNBT instanceof LocalEntity)
			baseNameStyle = baseNameStyle.applyFormat(ChatFormatting.WHITE);
		else
			throw new IllegalStateException("Cannot get base name style for " + localNBT.getClass().getName());
		
		return baseNameStyle;
	}
	
	public static final Style BASE_LORE_STYLE = Style.EMPTY.applyFormats(ChatFormatting.ITALIC, ChatFormatting.DARK_PURPLE);
	
	public static final Style BOOK_STYLE = Style.EMPTY.applyFormat(ChatFormatting.BLACK);
	
	public static boolean identical(Style a, Style b) {
		boolean output = Objects.equals(a.getColor(), b.getColor()) &&
				a.bold == b.bold &&
				a.italic == b.italic &&
				a.underlined == b.underlined &&
				a.strikethrough == b.strikethrough &&
				a.obfuscated == b.obfuscated &&
				Objects.equals(a.getClickEvent(), b.getClickEvent()) &&
				Objects.equals(a.getHoverEvent(), b.getHoverEvent()) &&
				Objects.equals(a.getInsertion(), b.getInsertion()) &&
				Objects.equals(a.getFont(), b.getFont());
		
		if (SHADOW_COLOR_EXISTS)
			output &= Objects.equals(a.getShadowColor(), b.getShadowColor());
		
		return output;
	}
	
	public static boolean hasFormatting(Style style, ChatFormatting formatting) {
		return identical(style, style.applyFormat(formatting));
	}
	
	public static boolean hasFormatting(Style style, Style base) {
		return !identical(style.applyTo(base), base);
	}
	
	public static Style minus(Style style, Style base) {
		Style output = Style.EMPTY;
		
		if (style.getColor() != null && !style.getColor().equals(base.getColor()))
			output = output.withColor(style.getColor());
		if (style.bold != null && !style.bold.equals(base.bold))
			output = output.withBold(style.bold);
		if (style.italic != null && !style.italic.equals(base.italic))
			output = output.withItalic(style.italic);
		if (style.underlined != null && !style.underlined.equals(base.underlined))
			output = output.withUnderlined(style.underlined);
		if (style.strikethrough != null && !style.strikethrough.equals(base.strikethrough))
			output = output.withStrikethrough(style.strikethrough);
		if (style.obfuscated != null && !style.obfuscated.equals(base.obfuscated))
			output = output.withObfuscated(style.obfuscated);
		if (style.bold != null && !style.bold.equals(base.bold))
			output = output.withBold(style.bold);
		if (style.getClickEvent() != null && !style.getClickEvent().equals(base.getClickEvent()))
			output = output.withClickEvent(style.getClickEvent());
		if (style.getHoverEvent() != null && !style.getHoverEvent().equals(base.getHoverEvent()))
			output = output.withHoverEvent(style.getHoverEvent());
		if (style.getInsertion() != null && !style.getInsertion().equals(base.getInsertion()))
			output = output.withInsertion(style.getInsertion());
		if (style.getFont() != null && !style.getFont().equals(base.getFont()))
			output = output.withFont(style.getFont());
		
		if (SHADOW_COLOR_EXISTS && style.getShadowColor() != null && !style.getShadowColor().equals(base.getShadowColor()))
			output = output.withShadowColor(style.getShadowColor());
		
		return output;
	}
	
	public static Style minusFormatting(Style style, Style base, ChatFormatting formatting) {
		if (formatting == ChatFormatting.RESET)
			return base;
		if (isColor(formatting))
			return style.withColor(base.getColor());
		return switch (formatting) {
			case BOLD -> style.withBold(base.bold);
			case ITALIC -> style.withItalic(base.italic);
			case UNDERLINE -> style.withUnderlined(base.underlined);
			case STRIKETHROUGH -> style.withStrikethrough(base.strikethrough);
			case OBFUSCATED -> style.withObfuscated(base.obfuscated);
			default -> throw new IllegalArgumentException("Unknown formatting: " + formatting);
		};
	}
	
	// 26.2 stripped ChatFormatting down to a code and a toString. Everything the
	// mod still asked it for now comes from TextColor or the enum constant itself.
	
	/**
	 * The closest dye to a chat colour. Not a bijection: two formattings map to blue and two to
	 * red, and the ones with no dye at all land on brown.
	 */
	public static DyeColor getDyeColor(ChatFormatting color) {
		switch (color) {
			case AQUA:
				return DyeColor.LIGHT_BLUE;
			case BLACK:
				return DyeColor.BLACK;
			case BLUE:
				return DyeColor.BLUE;
			case DARK_AQUA:
				return DyeColor.CYAN;
			case DARK_BLUE:
				return DyeColor.BLUE;
			case DARK_GRAY:
				return DyeColor.GRAY;
			case DARK_GREEN:
				return DyeColor.GREEN;
			case DARK_PURPLE:
				return DyeColor.PURPLE;
			case DARK_RED:
				return DyeColor.RED;
			case GOLD:
				return DyeColor.ORANGE;
			case GRAY:
				return DyeColor.LIGHT_GRAY;
			case GREEN:
				return DyeColor.LIME;
			case LIGHT_PURPLE:
				return DyeColor.PINK;
			case RED:
				return DyeColor.RED;
			case WHITE:
				return DyeColor.WHITE;
			case YELLOW:
				return DyeColor.YELLOW;
			default:
				return DyeColor.BROWN;
		}
	}
	
	public static boolean isColor(ChatFormatting formatting) {
		return TextColor.fromLegacyFormat(formatting) != null;
	}
	
	/** Null when the formatting is not a color. */
	public static Integer getColor(ChatFormatting formatting) {
		TextColor color = TextColor.fromLegacyFormat(formatting);
		return color == null ? null : color.getValue();
	}
	
	public static String getName(ChatFormatting formatting) {
		return formatting.name().toLowerCase(Locale.ROOT);
	}
	
	/** Null when no formatting goes by that name. */
	public static ChatFormatting getByName(String name) {
		try {
			return ChatFormatting.valueOf(name.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
	
}
