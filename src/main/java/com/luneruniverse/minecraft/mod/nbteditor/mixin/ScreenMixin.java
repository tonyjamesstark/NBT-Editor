package com.luneruniverse.minecraft.mod.nbteditor.mixin;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVMisc;
import com.luneruniverse.minecraft.mod.nbteditor.misc.MixinLink;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVTextEvents;
import com.luneruniverse.minecraft.mod.nbteditor.screens.ConfigScreen;
import com.luneruniverse.minecraft.mod.nbteditor.screens.ImportScreen;
import com.luneruniverse.minecraft.mod.nbteditor.screens.widgets.CreativeTabWidget;
import com.luneruniverse.minecraft.mod.nbteditor.util.MainUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.ClickEvent;

@Mixin(Screen.class)
public class ScreenMixin {
	@Inject(method = "clearChildren", at = @At("RETURN"))
	private void clearChildren(CallbackInfo info) {
		CreativeTabWidget.addCreativeTabs((Screen) (Object) this);
	}
	@Inject(method = "init(Lnet/minecraft/client/Minecraft;II)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;init()V"), require = 0)
	private void init(Minecraft client, int width, int height, CallbackInfo info) {
		CreativeTabWidget.addCreativeTabs((Screen) (Object) this);
	}
	
	@Inject(method = "onFilesDropped", at = @At("HEAD"))
	private void onFilesDropped(List<Path> paths, CallbackInfo info) {
		Screen source = (Screen) (Object) this;
		if (source instanceof AbstractContainerScreen || source instanceof PauseScreen)
			ImportScreen.importFiles(paths, Optional.empty());
	}
	
	@Inject(method = "handleClickEvent", at = @At("HEAD"), cancellable = true)
	private static void handleClickEvent(ClickEvent event, Minecraft client, Screen screen, CallbackInfo info) {
		if (event == null || MVMisc.hasShiftDown())
			return;
		MVTextEvents.ClickAction<?> clickAction = MVTextEvents.ClickAction.getAction(event);
		if (clickAction == MVTextEvents.ClickAction.OPEN_FILE &&
				MixinLink.tryRunClickEvent(clickAction.getStringifiedValue(event)))
			info.cancel();
	}
	
	// See toggled.DrawContextMixin#drawTooltip
	@Inject(method = "method_32633(Lnet/minecraft/class_4587;Ljava/util/List;II)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/class_4587;method_22903()V", shift = At.Shift.AFTER), remap = false, require = 0)
	@SuppressWarnings("target")
	private void renderTooltipFromComponents(GuiGraphics context, List<ClientTooltipComponent> tooltip, int x, int y, CallbackInfo info) {
		if (!ConfigScreen.isTooltipOverflowFix())
			return;
		
		int[] size = MixinLink.getTooltipSize(tooltip);
		int width = size[0];
		int height = size[1];
		int screenWidth = MainUtil.client.screen.width;
		int screenHeight = MainUtil.client.screen.height;
		
		x += 12;
		y -= 12;
		if (x + width > screenWidth)
			x -= 28 + width;
		if (y + height + 6 > screenHeight)
			y = screenHeight - height - 6;
		
		MixinLink.renderTooltipFromComponents(context, x, y, width, height, screenWidth, screenHeight);
	}
}
