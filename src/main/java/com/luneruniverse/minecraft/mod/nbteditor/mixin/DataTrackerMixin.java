package com.luneruniverse.minecraft.mod.nbteditor.mixin;

import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.luneruniverse.minecraft.mod.nbteditor.misc.ResetableDataTracker;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.Reflection;

import net.minecraft.entity.data.DataTracker;

@Mixin(DataTracker.class)
public class DataTrackerMixin implements ResetableDataTracker {
	@Shadow
	private boolean dirty;
	private static final Supplier<Reflection.FieldReference> DataTracker_entries_array =
			Reflection.getOptionalField(DataTracker.class, "field_13331", "[Lnet/minecraft/class_2945$class_2946;");
	@Override
	public void reset() {
		@SuppressWarnings("unchecked")
		DataTracker.Entry<?>[] entries = DataTracker_entries_array.get().get(this);
		for (DataTracker.Entry<?> entry : entries) {
			resetEntry(entry);
			entry.setDirty(true);
		}
		dirty = true;
	}
	private <T> void resetEntry(DataTracker.Entry<T> entry) {
		entry.set(entry.initialValue);
	}
}
