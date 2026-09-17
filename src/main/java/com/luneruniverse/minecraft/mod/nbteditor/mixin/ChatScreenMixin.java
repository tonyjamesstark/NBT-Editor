package com.luneruniverse.minecraft.mod.nbteditor.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.luneruniverse.minecraft.mod.nbteditor.misc.MixinLink;
import com.luneruniverse.minecraft.mod.nbteditor.screens.ConfigScreen;
import com.luneruniverse.minecraft.mod.nbteditor.util.MainUtil;

import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {
	@ModifyArg(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/EditBox;setMaxLength(I)V"), index = 0)
	private int setMaxLength(int length) {
		if (ConfigScreen.isChatLimitExtended())
			return Integer.MAX_VALUE;
		return length;
	}
	@Inject(method = "render", at = @At("HEAD"))
	private void render(GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo info) {
		MixinLink.renderChatLimitWarning((ChatScreen) (Object) this, context);
	}
	
	@Inject(method = "keyPressed", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V"), cancellable = true)
	private void keyPressed(KeyEvent input, CallbackInfoReturnable<Boolean> info) {
		if (!(MainUtil.client.screen instanceof ChatScreen)) {
			info.setReturnValue(true);
			info.cancel();
		}
	}
}
