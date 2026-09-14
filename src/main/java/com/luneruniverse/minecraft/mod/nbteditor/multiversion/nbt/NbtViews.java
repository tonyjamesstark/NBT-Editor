package com.luneruniverse.minecraft.mod.nbteditor.multiversion.nbt;

import java.util.function.Consumer;
import java.util.function.Function;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.DynamicRegistryManagerHolder;

import org.slf4j.LoggerFactory;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.util.ProblemReporter;

/**
 * Bridges CompoundTag to the ValueInput/ValueOutput pair that replaced the direct
 * NBT accessors on Entity and BlockEntity in 1.21.9.
 */
public class NbtViews {
	
	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger("nbteditor");
	
	/**
	 * A view whose ProblemReporter outlives the call. Vanilla's own reporters only log
	 * on close, so leaking one costs a dropped warning, not a resource.
	 */
	public static TagValueOutput newWriteView() {
		return TagValueOutput.createWithContext(new ProblemReporter.ScopedCollector(LOGGER), DynamicRegistryManagerHolder.get());
	}
	
	public static CompoundTag write(Consumer<ValueOutput> writer) {
		TagValueOutput view = newWriteView();
		writer.accept(view);
		return view.buildResult();
	}
	
	public static void read(CompoundTag nbt, Consumer<ValueInput> reader) {
		apply(nbt, view -> {
			reader.accept(view);
			return null;
		});
	}
	
	public static <T> T apply(CompoundTag nbt, Function<ValueInput, T> reader) {
		try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(LOGGER)) {
			return reader.apply(TagValueInput.create(reporter, DynamicRegistryManagerHolder.get(), nbt));
		}
	}
	
}
