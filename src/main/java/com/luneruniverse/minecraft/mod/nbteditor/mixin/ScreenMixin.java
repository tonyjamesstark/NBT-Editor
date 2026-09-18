package com.luneruniverse.minecraft.mod.nbteditor.mixin;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.luneruniverse.minecraft.mod.nbteditor.misc.MixinLink;
import com.luneruniverse.minecraft.mod.nbteditor.util.TextEvents;
import com.luneruniverse.minecraft.mod.nbteditor.screens.ConfigScreen;
import com.luneruniverse.minecraft.mod.nbteditor.screens.ImportScreen;
import com.luneruniverse.minecraft.mod.nbteditor.screens.widgets.CreativeTabWidget;

import com.luneruniverse.minecraft.mod.nbteditor.util.Keys;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.ClickEvent;

@Mixin(Screen.class)
public class ScreenMixin {
	@Inject(method = "clearWidgets", at = @At("RETURN"))
	private void clearWidgets(CallbackInfo info) {
		CreativeTabWidget.addCreativeTabs((Screen) (Object) this);
	}
	
	@Inject(method = "onFilesDrop", at = @At("HEAD"))
	private void onFilesDrop(List<Path> paths, CallbackInfo info) {
		Screen source = (Screen) (Object) this;
		if (source instanceof AbstractContainerScreen || source instanceof PauseScreen)
			ImportScreen.importFiles(paths, Optional.empty());
	}
	
	@Inject(method = "defaultHandleGameClickEvent", at = @At("HEAD"), cancellable = true)
	private static void defaultHandleGameClickEvent(ClickEvent event, Minecraft client, Screen screen, CallbackInfo info) {
		if (event == null || Keys.hasShiftDown())
			return;
		TextEvents.ClickAction<?> clickAction = TextEvents.ClickAction.getAction(event);
		if (clickAction == TextEvents.ClickAction.OPEN_FILE &&
				MixinLink.tryRunClickEvent(clickAction.getStringifiedValue(event)))
			info.cancel();
	}
	
	// See toggled.DrawContextMixin#drawTooltip
}
