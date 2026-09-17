package com.luneruniverse.minecraft.mod.nbteditor.screens.widgets;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVElement;
import com.luneruniverse.minecraft.mod.nbteditor.screens.Tickable;

import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.Minecraft;

public interface InitializableOverlay<T extends Screen> extends Renderable, MVElement, Tickable {
	public void init(T parent, int width, int height);
	public default void tick() {}
	
	@SuppressWarnings("unchecked")
	public default void initUnchecked(Screen parent) {
		init((T) parent, Minecraft.getInstance().getWindow().getGuiScaledWidth(), Minecraft.getInstance().getWindow().getGuiScaledHeight());
	}
}
