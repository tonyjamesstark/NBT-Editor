package com.luneruniverse.minecraft.mod.nbteditor.screens;

import java.util.function.Predicate;

/**
 * 26.2 dropped {@code EditBox.setFilter}. Grafted back on by
 * {@code TextFieldWidgetMixin} so number fields can keep rejecting text
 * that does not parse.
 */
public interface FilterableTextField {
	default void nbte$setFilter(Predicate<String> filter) {
		throw new RuntimeException("Missing implementation for FilterableTextField#nbte$setFilter");
	}
}
