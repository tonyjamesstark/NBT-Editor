package com.luneruniverse.minecraft.mod.nbteditor.mixin;

import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.luneruniverse.minecraft.mod.nbteditor.misc.ResetableDataTracker;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.Reflection;

import net.minecraft.network.syncher.SynchedEntityData;

@Mixin(SynchedEntityData.class)
public class DataTrackerMixin implements ResetableDataTracker {
	@Shadow
	private boolean dirty;
	private static final Supplier<Reflection.FieldReference> DataTracker_entries_array =
			Reflection.getOptionalField(SynchedEntityData.class, "field_13331", "[Lnet/minecraft/class_2945$class_2946;");
	@Override
	public void reset() {
		@SuppressWarnings("unchecked")
		SynchedEntityData.DataItem<?>[] entries = DataTracker_entries_array.get().get(this);
		for (SynchedEntityData.DataItem<?> entry : entries) {
			resetEntry(entry);
			entry.setDirty(true);
		}
		dirty = true;
	}
	private <T> void resetEntry(SynchedEntityData.DataItem<T> entry) {
		entry.setValue(entry.initialValue);
	}
}
