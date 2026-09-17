package com.luneruniverse.minecraft.mod.nbteditor.util;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.gson.JsonParseException;
import com.luneruniverse.minecraft.mod.nbteditor.NBTEditor;
import com.luneruniverse.minecraft.mod.nbteditor.fancytext.FancyText;
import com.luneruniverse.minecraft.mod.nbteditor.misc.MixinLink;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVTextEvents;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.Attempt;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.DynamicRegistryManagerHolder;
import com.luneruniverse.minecraft.mod.nbteditor.screens.util.FancyConfirmScreen;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import org.jetbrains.annotations.Nullable;
import com.mojang.serialization.DynamicOps;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.nbt.NbtFormatException;
import net.minecraft.network.chat.FormattedText.StyledContentConsumer;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;

public class TextUtil {
	
	/** Minecraft rejects the section sign, control characters, and DEL in typed text. */
	public static boolean isValidChar(char c) {
		return c != '§' && c >= ' ' && c != 127;
	}
	
	/** Drops every character {@link #isValidChar} rejects, optionally sparing line breaks. */
	public static String stripInvalidChars(String str, boolean allowLinebreaks) {
		StringBuilder output = new StringBuilder();
		for (char c : str.toCharArray()) {
			if (isValidChar(c)) {
				output.append(c);
			} else if (allowLinebreaks && c == '\n') {
				output.append(c);
			}
		}
		return output.toString();
	}
	
	/**
	 * Qualifies an unqualified id with the <code>minecraft</code> namespace, keeping a leading
	 * <code>!</code>, which the component syntax uses to mean "without this one".
	 */
	public static String addNamespace(String id) {
		if (id.contains(":"))
			return id;
		if (id.startsWith("!"))
			return "!minecraft:" + id.substring(1);
		return "minecraft:" + id;
	}
	
	/** The text's own literal content, without the content of its children. */
	public static String getContent(Component text) {
		StringBuilder output = new StringBuilder();
		text.getContents().visit(str -> {
			output.append(str);
			return Optional.empty();
		});
		return output.toString();
	}
	
	public static List<Component> getLongTranslatableTextLines(String key) {
		List<Component> lines = new ArrayList<>();
		for (int i = 1; i <= 50; i++) {
			Component line = Component.translatableEscape(key + "_" + i);
			String str = line.getString();
			if (str.equals(key + "_" + i))
				break;
			
			if (str.startsWith("[LINK] ")) {
				String url = str.substring("[LINK] ".length());
				URI uri;
				try {
					uri = new URI(url);
				} catch (URISyntaxException e) {
					throw new IllegalArgumentException("Invalid link: " + url, e);
				}
				line = Component.literal(url)
						.withStyle(style -> style.withClickEvent(MVTextEvents.ClickAction.OPEN_URL.newEvent(uri))
						.withUnderlined(true).withItalic(true).withColor(ChatFormatting.GOLD));
			}
			if (str.startsWith("[FORMAT] ")) {
				String toFormat = str.substring("[FORMAT] ".length());
				line = FancyText.parse(toFormat);
			}
			lines.add(line);
		}
		return lines;
	}
	public static Component getLongTranslatableText(String key) {
		List<Component> lines = getLongTranslatableTextLines(key);
		if (lines.isEmpty())
			return Component.nullToEmpty(key);
		MutableComponent output = lines.get(0).copy();
		for (int i = 1; i < lines.size(); i++)
			output.append("\n").append(lines.get(i));
		return output;
	}
	
	public static Component parseTranslatableFormatted(String key, Object... args) {
		return FancyText.parse(Component.translatableEscape(key, args).getString());
	}
	
	public static Component substring(Component text, int start, int end) {
		MutableComponent output = Component.literal("");
		text.visit(new StyledContentConsumer<Boolean>() {
			private int i;
			@Override
			public Optional<Boolean> accept(Style style, String str) {
				if (i + str.length() <= start) {
					i += str.length();
					return Optional.empty();
				}
				if (i >= start) {
					if (end >= 0 && i + str.length() > end)
						return accept(style, str.substring(0, end - i));
					output.append(Component.literal(str).withStyle(style));
					i += str.length();
					if (end >= 0 && i == end)
						return Optional.of(true);
					return Optional.empty();
				} else {
					str = str.substring(start - i);
					i = start;
					accept(style, str);
					return Optional.empty();
				}
			}
		}, Style.EMPTY);
		return output;
	}
	public static Component substring(Component text, int start) {
		return substring(text, start, -1);
	}
	
	public static Component deleteCharAt(Component text, int index) {
		MutableComponent output = Component.literal("");
		AtomicInteger pos = new AtomicInteger(0);
		text.visit((style, str) -> {
			int strLen = str.length();
			if (pos.getPlain() <= index && index < pos.getPlain() + strLen)
				str = new StringBuilder(str).deleteCharAt(index - pos.getPlain()).toString();
			if (!str.isEmpty())
				output.append(Component.literal(str).setStyle(style));
			pos.setPlain(pos.getPlain() + strLen);
			return Optional.empty();
		}, Style.EMPTY);
		return output;
	}
	
	public static Component joinLines(List<Component> lines) {
		MutableComponent output = Component.literal("");
		for (int i = 0; i < lines.size(); i++) {
			if (i > 0)
				output.append("\n");
			output.append(lines.get(i));
		}
		return output;
	}
	public static List<Component> splitText(Component text) {
		List<Component> output = new ArrayList<>();
		int i;
		while ((i = text.getString().indexOf('\n')) != -1) {
			output.add(substring(text, 0, i));
			text = substring(text, i + 1);
		}
		output.add(text);
		return output;
	}
	
	public static Component stripInvalidChars(Component text, boolean allowLineBreaks) {
		MutableComponent output = Component.literal("");
		text.visit((style, str) -> {
			output.append(Component.literal(stripInvalidChars(str, allowLineBreaks)).setStyle(style));
			return Optional.empty();
		}, Style.EMPTY);
		return output;
	}
	
	public static Component attachFileTextOptions(MutableComponent link, File file) {
		return link.append(" ").append(Component.translatableEscape("nbteditor.file_options.show").withStyle(style ->
				style.withClickEvent(MVTextEvents.ClickAction.OPEN_FILE.newEvent(
						file.getAbsoluteFile().getParentFile().getAbsolutePath()))))
				.append(" ").append(Component.translatableEscape("nbteditor.file_options.delete").withStyle(style ->
				MixinLink.withRunClickEvent(style, () -> Minecraft.getInstance().setScreenAndShow(
						new FancyConfirmScreen(confirmed -> {
							if (confirmed) {
								if (file.exists()) {
									try {
										Files.deleteIfExists(file.toPath());
										Minecraft.getInstance().player.sendSystemMessage(Component.translatableEscape("nbteditor.file_options.delete.success", "§6" + file.getName()));
									} catch (IOException e) {
										NBTEditor.LOGGER.error("Error deleting file", e);
										Minecraft.getInstance().player.sendSystemMessage(Component.translatableEscape("nbteditor.file_options.delete.error", "§6" + file.getName()));
									}
								} else
									Minecraft.getInstance().player.sendSystemMessage(Component.translatableEscape("nbteditor.file_options.delete.missing", "§6" + file.getName()));
							}
							Minecraft.getInstance().setScreenAndShow(null);
						}, Component.translatableEscape("nbteditor.file_options.delete.title", file.getName()),
								Component.translatableEscape("nbteditor.file_options.delete.desc", file.getName()))))));
	}
	
	public static boolean isTextFormatted(Component text, Style base) {
		if (StyleUtil.hasFormatting(text.getStyle(), base))
			return true;
		
		for (Component sibling : text.getSiblings()) {
			if (isTextFormatted(sibling, base))
				return true;
		}
		
		return false;
	}
	
	public static int lastIndexOf(Component text, int ch) {
		AtomicInteger output = new AtomicInteger(-1);
		AtomicInteger pos = new AtomicInteger(0);
		text.visit(str -> {
			int i = str.lastIndexOf(ch);
			if (i != -1)
				output.setPlain(pos.getPlain() + i);
			pos.setPlain(pos.getPlain() + str.length());
			return Optional.empty();
		});
		return output.getPlain();
	}
	
	public static Component fromStringSafely(String str, boolean eitherFormat) {
		try {
			Component output = fromString(str, eitherFormat);
			if (output != null)
				return output;
		} catch (IllegalArgumentException e) {}
		return Component.nullToEmpty(str);
	}
	public static Component fromSNbtSafely(String snbt) {
		try {
			return fromSNbt(snbt);
		} catch (CommandSyntaxException | NbtFormatException e) {}
		return Component.nullToEmpty(snbt);
	}
	public static Component fromJsonSafely(String json) {
		try {
			Component output = fromJson(json);
			if (output != null)
				return output;
		} catch (JsonParseException e) {}
		return Component.nullToEmpty(json);
	}
	
	
	/**
	 * Throws when <code>str</code> is neither valid SNBT nor valid JSON; the suppressed causes say
	 * which parse failed and why. {@link #fromStringSafely} yields the raw string instead.
	 */
	public static @Nullable Component fromString(String str, boolean eitherFormat) throws IllegalArgumentException {
		IllegalArgumentException wrapper;
		try {
			return fromSNbt(str);
		} catch (CommandSyntaxException | NbtFormatException e) {
			wrapper = new IllegalArgumentException("Failed to parse text");
			wrapper.addSuppressed(e);
			if (!eitherFormat)
				throw wrapper;
		}

		try {
			return fromJson(str);
		} catch (JsonParseException e) {
			wrapper.addSuppressed(e);
			throw wrapper;
		}
	}
	public static String toString(Component text) throws IllegalArgumentException {
		try {
			return toSNbt(text);
		} catch (NbtFormatException | JsonParseException e) {
			throw new IllegalArgumentException("Failed to stringify text", e);
		}
	}
	
	public static @Nullable Component fromMinecraft(Tag mc) throws IllegalArgumentException {
		try {
			return fromNbt(mc);
		} catch (NbtFormatException | JsonParseException e) {
			throw new IllegalArgumentException("Failed to parse text", e);
		}
	}
	/**
	 * Reads the text at <code>key</code>, falling back when the key is missing or holds
	 * something that is not text. An entity whose CustomName was hand-edited into nonsense still
	 * has to render a name.
	 */
	public static Component fromMinecraftSafely(@Nullable CompoundTag nbt, String key, Supplier<Component> fallback) {
		if (nbt != null) {
			Tag textNbt = nbt.get(key);
			if (textNbt != null) {
				try {
					Component text = fromMinecraft(textNbt);
					if (text != null)
						return text;
				} catch (IllegalArgumentException e) {}
			}
		}
		return fallback.get();
	}
	
	public static Tag toMinecraft(Component text) throws IllegalArgumentException {
		try {
			return toNbt(text);
		} catch (NbtFormatException | JsonParseException e) {
			throw new IllegalArgumentException("Failed to stringify text", e);
		}
	}
	
	/** Throws when <code>snbt</code> is not valid SNBT; {@link #fromSNbtSafely} yields the raw string instead. */
	public static Component fromSNbt(String snbt) throws CommandSyntaxException, NbtFormatException {
		return fromNbt(NbtIO.parseSnbt(snbt));
	}
	public static String toSNbt(Component text) throws NbtFormatException {
		return toNbt(text).toString();
	}
	
	private static DynamicOps<JsonElement> jsonOps() {
		return DynamicRegistryManagerHolder.get().createSerializationContext(JsonOps.INSTANCE);
	}
	/** Throws when <code>json</code> is not valid text JSON; {@link #fromJsonSafely} yields the raw string instead. */
	public static @Nullable Component fromJson(String json) throws JsonParseException {
		return Attempt.ofResult(ComponentSerialization.CODEC.parse(jsonOps(), JsonParser.parseString(json)))
				.getSuccessOrThrow(JsonParseException::new);
	}
	
	public static Component fromNbt(Tag nbt) throws NbtFormatException {
		return Attempt.ofResult(ComponentSerialization.CODEC.parse(NbtOps.INSTANCE, nbt)).getSuccessOrThrow(NbtFormatException::new);
	}
	public static Tag toNbt(Component text) throws NbtFormatException {
		return Attempt.ofResult(ComponentSerialization.CODEC.encodeStart(NbtOps.INSTANCE, text)).getSuccessOrThrow(NbtFormatException::new);
	}
	
}
