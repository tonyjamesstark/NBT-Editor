package com.luneruniverse.minecraft.mod.nbteditor.mixin;

import java.util.function.Consumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.luneruniverse.minecraft.mod.nbteditor.misc.MixinLink;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.TextInst;
import com.luneruniverse.minecraft.mod.nbteditor.screens.ConfigScreen;
import com.luneruniverse.minecraft.mod.nbteditor.util.TextUtil;

import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.Component;

@Mixin(Screenshot.class)
public class ScreenshotRecorderMixin {
	@ModifyVariable(method = "grab(Ljava/io/File;Ljava/lang/String;Lcom/mojang/blaze3d/pipeline/RenderTarget;ILjava/util/function/Consumer;)V", at = @At("HEAD"), ordinal = 0)
	private static Consumer<Component> saveScreenshot3(Consumer<Component> receiver) {
		return saveScreenshotImpl(receiver);
	}
	
	
	
	private static Consumer<Component> saveScreenshotImpl(Consumer<Component> receiver) {
		if (!ConfigScreen.isScreenshotOptions())
			return receiver;
		return msg -> receiver.accept(TextUtil.attachFileTextOptions(TextInst.copy(msg), MixinLink.screenshotTarget));
	}
}
