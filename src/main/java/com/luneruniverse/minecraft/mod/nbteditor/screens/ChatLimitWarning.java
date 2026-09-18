package com.luneruniverse.minecraft.mod.nbteditor.screens;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;

import com.luneruniverse.minecraft.mod.nbteditor.mixin.ChatScreenAccessor;

/**
 * The banner drawn over the chat screen once a message has run past the length vanilla would
 * have allowed. The limit itself is lifted in {@code mixin.ChatScreenMixin}; whether a server
 * accepts the message is not the mod's to know, so a warning is all the user gets.
 */
public class ChatLimitWarning {
	
	private static final int VANILLA_LIMIT = 256;
	
	public static void render(ChatScreen screen, GuiGraphicsExtractor context) {
		if (!ConfigScreen.isChatLimitExtended())
			return;
		
		EditBox chatField = ((ChatScreenAccessor) screen).getInput();
		if (chatField.getValue().length() <= VANILLA_LIMIT)
			return;
		
		context.fill(screen.width - 202, screen.height - 40, screen.width - 2, screen.height - 14, 0xAAFFAA00);
		Font textRenderer = Minecraft.getInstance().font;
		context.centeredText(textRenderer, Component.translatableEscape("nbteditor.chat_length_warning_1"), screen.width - 102, screen.height - 40 + textRenderer.lineHeight / 2, 0xFFAA5500);
		context.centeredText(textRenderer, Component.translatableEscape("nbteditor.chat_length_warning_2"), screen.width - 102, screen.height - 28 + textRenderer.lineHeight / 2, 0xFFAA5500);
	}
	
}
