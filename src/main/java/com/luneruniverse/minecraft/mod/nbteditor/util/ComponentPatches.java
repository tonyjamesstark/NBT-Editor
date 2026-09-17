package com.luneruniverse.minecraft.mod.nbteditor.util;

import java.util.Map;
import java.util.Optional;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;

/** Reading a {@link DataComponentPatch} as the editor needs to see it. */
public class ComponentPatches {
	
	/**
	 * What the patch says about a component: {@code null} if the patch does not mention it, an
	 * empty {@link Optional} if the patch explicitly removes it, otherwise the patched value.
	 *
	 * <p>26.2 folded a prototype lookup into {@link DataComponentPatch#get}, which collapses the
	 * first two cases. The editor has to tell them apart: "unchanged" and "deleted" are different
	 * edits.
	 */
	@SuppressWarnings("unchecked")
	public static <T> Optional<? extends T> get(DataComponentPatch patch, DataComponentType<? extends T> type) {
		for (Map.Entry<DataComponentType<?>, Optional<?>> entry : patch.entrySet()) {
			if (entry.getKey() == type)
				return (Optional<? extends T>) entry.getValue();
		}
		return null;
	}
	
}
