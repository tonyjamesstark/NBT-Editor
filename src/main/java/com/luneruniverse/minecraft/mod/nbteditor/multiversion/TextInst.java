package com.luneruniverse.minecraft.mod.nbteditor.multiversion;


import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.DynamicOps;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.luneruniverse.minecraft.mod.nbteditor.util.TextUtil;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.nbt.InvalidNbtException;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;

public class TextInst {
	
	public static Text of(String msg) {
		return Text.of(msg);
	}
	public static EditableText literal(String msg) {
		return new EditableText(Text.literal(msg));
	}
	public static EditableText translatable(String key, Object... args) {
		return new EditableText(Text.stringifiedTranslatable(key, args));
	}
	
	public static EditableText copy(Text text) {
		return new EditableText(text.copy());
	}
	public static EditableText copyContentOnly(Text text) {
		return new EditableText(text.copyContentOnly());
	}
	
	public static EditableText bracketed(Text text) {
		return translatable("chat.square_brackets", text);
	}
	
	
	/**
	 * <strong>CONSIDER USING {@link TextUtil#fromStringSafely(String, boolean)}</strong>
	 */
	public static @Nullable Text fromString(String str, boolean eitherFormat) throws IllegalArgumentException {
		IllegalArgumentException wrapper;
		try {
			return fromSNbt(str);
		} catch (CommandSyntaxException | InvalidNbtException e) {
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
	public static String toString(Text text) throws IllegalArgumentException {
		try {
			return toSNbt(text);
		} catch (InvalidNbtException | JsonParseException e) {
			throw new IllegalArgumentException("Failed to stringify text", e);
		}
	}
	
	public static @Nullable Text fromMinecraft(NbtElement mc) throws IllegalArgumentException {
		try {
			return fromNbt(mc);
		} catch (InvalidNbtException | JsonParseException e) {
			throw new IllegalArgumentException("Failed to parse text", e);
		}
	}
	public static NbtElement toMinecraft(Text text) throws IllegalArgumentException {
		try {
			return toNbt(text);
		} catch (InvalidNbtException | JsonParseException e) {
			throw new IllegalArgumentException("Failed to stringify text", e);
		}
	}
	
	/**
	 * <strong>CONSIDER USING {@link TextUtil#fromSNbtSafely(String)}</strong>
	 */
	public static Text fromSNbt(String snbt) throws CommandSyntaxException, InvalidNbtException {
		return fromNbt(MVMisc.parseNbt(snbt));
	}
	public static String toSNbt(Text text) throws InvalidNbtException {
		return toNbt(text).toString();
	}
	
	/**
	 * <strong>CONSIDER USING {@link TextUtil#fromJsonSafely(String)}</strong>
	 */
	private static DynamicOps<JsonElement> jsonOps() {
		return DynamicRegistryManagerHolder.get().getOps(JsonOps.INSTANCE);
	}
	public static @Nullable Text fromJson(String json) throws JsonParseException {
		return Attempt.ofResult(TextCodecs.CODEC.parse(jsonOps(), JsonParser.parseString(json)))
				.getSuccessOrThrow(JsonParseException::new);
	}
	public static String toJson(Text text) throws JsonParseException {
		return Attempt.ofResult(TextCodecs.CODEC.encodeStart(jsonOps(), text))
				.getSuccessOrThrow(JsonParseException::new).toString();
	}
	
	public static Text fromNbt(NbtElement nbt) throws InvalidNbtException {
		return Attempt.ofResult(TextCodecs.CODEC.parse(NbtOps.INSTANCE, nbt)).getSuccessOrThrow(InvalidNbtException::new);
	}
	public static NbtElement toNbt(Text text) throws InvalidNbtException {
		return Attempt.ofResult(TextCodecs.CODEC.encodeStart(NbtOps.INSTANCE, text)).getSuccessOrThrow(InvalidNbtException::new);
	}
	
}
