package com.luneruniverse.minecraft.mod.nbteditor.multiversion.nbt;

import java.util.function.Consumer;
import java.util.function.Function;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.DynamicRegistryManagerHolder;

import org.slf4j.LoggerFactory;

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
	
	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger("nbteditor");
	
	/**
	 * A view whose ErrorReporter outlives the call. Vanilla's own reporters only log
	 * on close, so leaking one costs a dropped warning, not a resource.
	 */
	public static NbtWriteView newWriteView() {
		return NbtWriteView.create(new ErrorReporter.Logging(LOGGER), DynamicRegistryManagerHolder.get());
	}
	
	public static NbtCompound write(Consumer<WriteView> writer) {
		NbtWriteView view = newWriteView();
		writer.accept(view);
		return view.getNbt();
	}
	
	public static void read(NbtCompound nbt, Consumer<ReadView> reader) {
		apply(nbt, view -> {
			reader.accept(view);
			return null;
		});
	}
	
	public static <T> T apply(NbtCompound nbt, Function<ReadView, T> reader) {
		try (ErrorReporter.Logging reporter = new ErrorReporter.Logging(LOGGER)) {
			return reader.apply(NbtReadView.create(reporter, DynamicRegistryManagerHolder.get(), nbt));
		}
	}
	
}
