package com.luneruniverse.minecraft.mod.nbteditor.multiversion;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class MVScreen extends Screen implements OldEventBehavior, IgnoreCloseScreenPacket {
	
	protected MVScreen(Text title) {
		super(title);
	}
	
	public void setInitialFocus(Element element) {
		MVMisc.setInitialFocus(this, element, super::setInitialFocus);
	}
	@Override
	protected void setInitialFocus() {}
	
}
