package com.luneruniverse.minecraft.mod.nbteditor.multiversion;

import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class MVScreen extends Screen implements OldEventBehavior, IgnoreCloseScreenPacket {
	
	protected MVScreen(Component title) {
		super(title);
	}
	
	public void setInitialFocus(GuiEventListener element) {
		MVMisc.setInitialFocus(this, element, super::setInitialFocus);
	}
	@Override
	protected void setInitialFocus() {}
	
}
