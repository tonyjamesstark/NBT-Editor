package com.luneruniverse.minecraft.mod.nbteditor.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.luneruniverse.minecraft.mod.nbteditor.NBTEditorClient;
import com.luneruniverse.minecraft.mod.nbteditor.misc.MixinLink;

import net.minecraft.client.Minecraft;
import net.minecraft.client.main.GameConfig;

@Mixin(Minecraft.class)
public class MinecraftClientMixin {
	
	@Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/thread/ReentrantBlockableEventLoop;<init>(Ljava/lang/String;Z)V", shift = At.Shift.AFTER))
	private void init(GameConfig args, CallbackInfo info) {
		MixinLink.MAIN_THREAD = Thread.currentThread();
	}
	
	@Inject(method = "stop", at = @At("HEAD"))
	private void stop(CallbackInfo info) {
		if (NBTEditorClient.CLIENT_CHEST != null)
			NBTEditorClient.CLIENT_CHEST.stop();
	}
	
}
