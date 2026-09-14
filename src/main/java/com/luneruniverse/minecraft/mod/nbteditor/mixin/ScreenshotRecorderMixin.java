package com.luneruniverse.minecraft.mod.nbteditor.mixin;

import java.util.function.Consumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.luneruniverse.minecraft.mod.nbteditor.misc.MixinLink;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.TextInst;
import com.luneruniverse.minecraft.mod.nbteditor.screens.ConfigScreen;
import com.luneruniverse.minecraft.mod.nbteditor.util.TextUtil;

import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.Component;

@Mixin(Screenshot.class)
public class ScreenshotRecorderMixin {
	@ModifyVariable(method = "saveScreenshot(Ljava/io/File;Ljava/lang/String;Lnet/minecraft/client/gl/Framebuffer;Ljava/util/function/Consumer;)V", at = @At("HEAD"), ordinal = 0)
	@Group(name = "saveScreenshot", min = 1)
	private static Consumer<Component> saveScreenshot3(Consumer<Component> receiver) {
		return saveScreenshotImpl(receiver);
	}
	
	@ModifyVariable(method = "method_1662(Ljava/io/File;Ljava/lang/String;Lnet/minecraft/class_276;Ljava/util/function/Consumer;)V", at = @At("HEAD"), ordinal = 0, remap = false)
	@Group(name = "saveScreenshot", min = 1)
	@SuppressWarnings("target")
	private static Consumer<Component> saveScreenshot2(Consumer<Component> receiver) {
		return saveScreenshotImpl(receiver);
	}
	
	@ModifyVariable(method = "method_1662(Ljava/io/File;Ljava/lang/String;IILnet/minecraft/class_276;Ljava/util/function/Consumer;)V", at = @At("HEAD"), ordinal = 0, remap = false)
	@Group(name = "saveScreenshot", min = 1)
	@SuppressWarnings("target")
	private static Consumer<Component> saveScreenshot1(Consumer<Component> receiver) {
		return saveScreenshotImpl(receiver);
	}
	
	private static Consumer<Component> saveScreenshotImpl(Consumer<Component> receiver) {
		if (!ConfigScreen.isScreenshotOptions())
			return receiver;
		return msg -> receiver.accept(TextUtil.attachFileTextOptions(TextInst.copy(msg), MixinLink.screenshotTarget));
	}
}
