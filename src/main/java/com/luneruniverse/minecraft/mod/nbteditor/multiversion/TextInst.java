package com.luneruniverse.minecraft.mod.nbteditor.multiversion;


import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.DynamicOps;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.luneruniverse.minecraft.mod.nbteditor.util.TextUtil;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.network.chat.MutableComponent;
import net.minecraft.nbt.NbtFormatException;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

public class TextInst {
	
	public static Component of(String msg) {
		return Component.nullToEmpty(msg);
	}
	public static MutableComponent literal(String msg) {
		return Component.literal(msg);
	}
	public static MutableComponent translatable(String key, Object... args) {
		return Component.translatableEscape(key, args);
	}
	
	public static MutableComponent copy(Component text) {
		return text.copy();
	}
	public static MutableComponent copyContentOnly(Component text) {
		return text.plainCopy();
	}
	
	public static MutableComponent bracketed(Component text) {
		return translatable("chat.square_brackets", text);
	}
	
	
	/**
	 * <strong>CONSIDER USING {@link TextUtil#fromStringSafely(String, boolean)}</strong>
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
	public static Tag toMinecraft(Component text) throws IllegalArgumentException {
		try {
			return toNbt(text);
		} catch (NbtFormatException | JsonParseException e) {
			throw new IllegalArgumentException("Failed to stringify text", e);
		}
	}
	
	/**
	 * <strong>CONSIDER USING {@link TextUtil#fromSNbtSafely(String)}</strong>
	 */
	public static Component fromSNbt(String snbt) throws CommandSyntaxException, NbtFormatException {
		return fromNbt(MVMisc.parseNbt(snbt));
	}
	public static String toSNbt(Component text) throws NbtFormatException {
		return toNbt(text).toString();
	}
	
	/**
	 * <strong>CONSIDER USING {@link TextUtil#fromJsonSafely(String)}</strong>
	 */
	private static DynamicOps<JsonElement> jsonOps() {
		return DynamicRegistryManagerHolder.get().createSerializationContext(JsonOps.INSTANCE);
	}
	public static @Nullable Component fromJson(String json) throws JsonParseException {
		return Attempt.ofResult(ComponentSerialization.CODEC.parse(jsonOps(), JsonParser.parseString(json)))
				.getSuccessOrThrow(JsonParseException::new);
	}
	public static String toJson(Component text) throws JsonParseException {
		return Attempt.ofResult(ComponentSerialization.CODEC.encodeStart(jsonOps(), text))
				.getSuccessOrThrow(JsonParseException::new).toString();
	}
	
	public static Component fromNbt(Tag nbt) throws NbtFormatException {
		return Attempt.ofResult(ComponentSerialization.CODEC.parse(NbtOps.INSTANCE, nbt)).getSuccessOrThrow(NbtFormatException::new);
	}
	public static Tag toNbt(Component text) throws NbtFormatException {
		return Attempt.ofResult(ComponentSerialization.CODEC.encodeStart(NbtOps.INSTANCE, text)).getSuccessOrThrow(NbtFormatException::new);
	}
	
}
