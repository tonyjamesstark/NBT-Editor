package com.luneruniverse.minecraft.mod.nbteditor.mixin;

import java.awt.Point;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import com.luneruniverse.minecraft.mod.nbteditor.screens.widgets.SuggestingTextFieldWidget;

import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;

@Mixin(CommandSuggestions.class)
public class ChatInputSuggestorMixin {
	@Shadow
	EditBox input;
	
	@ModifyArgs(method = "showSuggestions", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/CommandSuggestions$SuggestionsList;<init>(Lnet/minecraft/client/gui/components/CommandSuggestions;IIILjava/util/List;Z)V"))
	private void SuggestionWindow(Args args) {
		if (!(input instanceof SuggestingTextFieldWidget suggestor))
			return;
		
		if (suggestor.isDropdownOnly()) {
			Point pos = suggestor.getSpecialDropdownPos();
			args.set(1, pos.x);
			args.set(2, pos.y);
		} else
			args.set(2, input.getY() + input.getHeight() + 2);
	}
}
