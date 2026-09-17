package com.luneruniverse.minecraft.mod.nbteditor.util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;

/**
 * Reading and writing NBT: binary through streams and files, and SNBT through text.
 *
 * <p>The size limit is deliberately removed. Vanilla caps reads because it is parsing packets
 * and world data it did not write; this mod reads files the user chose to open, and refusing
 * one for being large would be a bug rather than a safeguard.
 *
 * <p>The file overloads exist because every caller that has a {@link File} was opening a stream
 * in a try-with-resources and closing it by hand.
 */
public class NbtIO {
	
	public static CompoundTag read(InputStream stream) throws IOException {
		return NbtIo.read(new DataInputStream(stream), NbtAccounter.unlimitedHeap());
	}
	
	public static CompoundTag readCompressed(InputStream stream) throws IOException {
		return NbtIo.readCompressed(stream, NbtAccounter.unlimitedHeap());
	}
	
	public static void write(CompoundTag nbt, OutputStream stream) throws IOException {
		NbtIo.write(nbt, new DataOutputStream(stream));
	}
	
	public static void writeCompressed(CompoundTag nbt, OutputStream stream) throws IOException {
		NbtIo.writeCompressed(nbt, stream);
	}
	
	public static CompoundTag read(File file) throws IOException {
		try (FileInputStream stream = new FileInputStream(file)) {
			return read(stream);
		}
	}
	
	public static CompoundTag readCompressed(File file) throws IOException {
		try (FileInputStream stream = new FileInputStream(file)) {
			return readCompressed(stream);
		}
	}
	
	public static void write(CompoundTag nbt, File file) throws IOException {
		try (FileOutputStream stream = new FileOutputStream(file)) {
			write(nbt, stream);
		}
	}
	
	public static void writeCompressed(CompoundTag nbt, File file) throws IOException {
		try (FileOutputStream stream = new FileOutputStream(file)) {
			writeCompressed(nbt, stream);
		}
	}
	
	/** Parses SNBT, rejecting anything left over after the tag. */
	public static Tag parseSnbt(StringReader snbt) throws CommandSyntaxException {
		return TagParser.create(NbtOps.INSTANCE).parseFully(snbt);
	}
	
	public static Tag parseSnbt(String snbt) throws CommandSyntaxException {
		return parseSnbt(new StringReader(snbt));
	}
	
}
