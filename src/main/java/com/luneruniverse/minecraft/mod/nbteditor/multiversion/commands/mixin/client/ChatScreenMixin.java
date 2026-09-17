package com.luneruniverse.minecraft.mod.nbteditor.multiversion.commands.mixin.client;

import org.apache.commons.lang3.StringUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.commands.ClientCommandInternals;
import com.luneruniverse.minecraft.mod.nbteditor.util.MainUtil;

import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.gui.components.EditBox;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {
	@Shadow
	protected EditBox input;
	
	@Inject(method = "keyPressed", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/ChatScreen;handleChatInput(Ljava/lang/String;Z)V"), cancellable = true)
	private void enterPressed(KeyEvent event, CallbackInfoReturnable<Boolean> info) {
		enterPressed_impl(info);
	}
	private void enterPressed_impl(CallbackInfoReturnable<Boolean> info) {
		String text = StringUtils.normalizeSpace(input.getValue().trim());
		if (text.isEmpty() || text.length() <= 256)
			return;
		if (text.charAt(0) == '/' && ClientCommandInternals.executeCommand(text.substring(1))) {
			MainUtil.client.gui.hud.getChat().addRecentChat(text);
			if (MainUtil.client.gui.screen() instanceof ChatScreen)
				MainUtil.client.setScreenAndShow(null);
			info.setReturnValue(true);
		} else
			input.value = (text.length() <= 256 ? text : text.substring(0, 256));
	}
}
