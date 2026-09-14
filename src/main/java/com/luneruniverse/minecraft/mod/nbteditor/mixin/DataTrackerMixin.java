package com.luneruniverse.minecraft.mod.nbteditor.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.luneruniverse.minecraft.mod.nbteditor.misc.ResetableDataTracker;

import net.minecraft.network.syncher.SynchedEntityData;

@Mixin(SynchedEntityData.class)
public class DataTrackerMixin implements ResetableDataTracker {
	@Shadow
	private boolean isDirty;
	@Shadow
	private SynchedEntityData.DataItem<?>[] itemsById;
	@Override
	public void reset() {
		for (SynchedEntityData.DataItem<?> entry : itemsById) {
			resetEntry(entry);
			entry.setDirty(true);
		}
		isDirty = true;
	}
	private <T> void resetEntry(SynchedEntityData.DataItem<T> entry) {
		entry.setValue(entry.initialValue);
	}
}
