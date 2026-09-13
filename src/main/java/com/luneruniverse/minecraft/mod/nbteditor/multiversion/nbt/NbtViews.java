package com.luneruniverse.minecraft.mod.nbteditor.multiversion.nbt;

import java.util.function.Consumer;
import java.util.function.Function;

import com.luneruniverse.minecraft.mod.nbteditor.NBTEditor;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.DynamicRegistryManagerHolder;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.storage.NbtReadView;
import net.minecraft.storage.NbtWriteView;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.ErrorReporter;

/**
 * Bridges NbtCompound to the ReadView/WriteView pair that replaced the direct
 * NBT accessors on Entity and BlockEntity in 1.21.9.
 */
public class NbtViews {
	
	public static NbtCompound write(Consumer<WriteView> writer) {
		try (ErrorReporter.Logging reporter = new ErrorReporter.Logging(NBTEditor.LOGGER)) {
			NbtWriteView view = NbtWriteView.create(reporter, DynamicRegistryManagerHolder.get());
			writer.accept(view);
			return view.getNbt();
		}
	}
	
	public static void read(NbtCompound nbt, Consumer<ReadView> reader) {
		apply(nbt, view -> {
			reader.accept(view);
			return null;
		});
	}
	
	public static <T> T apply(NbtCompound nbt, Function<ReadView, T> reader) {
		try (ErrorReporter.Logging reporter = new ErrorReporter.Logging(NBTEditor.LOGGER)) {
			return reader.apply(NbtReadView.create(reporter, DynamicRegistryManagerHolder.get(), nbt));
		}
	}
	
}
