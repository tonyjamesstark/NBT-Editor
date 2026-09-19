package com.luneruniverse.minecraft.mod.nbteditor.screens.widgets;

import java.awt.Point;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;

public class SuggestingTextFieldWidget extends NamedTextFieldWidget {
	
	private final CommandSuggestions suggestor;
	private BiFunction<String, Integer, CompletableFuture<Suggestions>> suggestions;
	
	public SuggestingTextFieldWidget(Screen screen, int x, int y, int width, int height, EditBox copyFrom) {
		super(x, y, width, height, copyFrom);
		suggestor = new CommandSuggestions(Minecraft.getInstance(), screen, this, Minecraft.getInstance().font, false, true, 0, 7, false, 0x80000000) {
			@Override
			public void updateCommandInfo() {
				if (!this.keepSuggestions) {
					SuggestingTextFieldWidget.this.setSuggestion(null);
					this.suggestions = null;
				}
				this.commandUsage.clear();
				if (this.suggestions == null || !this.keepSuggestions) {
					if (SuggestingTextFieldWidget.this.suggestions == null)
						this.pendingSuggestions = new SuggestionsBuilder("", 0).buildFuture();
					else
						this.pendingSuggestions = SuggestingTextFieldWidget.this.suggestions.apply(SuggestingTextFieldWidget.this.getValue(), SuggestingTextFieldWidget.this.getCursorPosition());
					this.pendingSuggestions.thenRun(() -> {
						if (!this.pendingSuggestions.isDone())
							return;
						showSuggestions(false);
					});
				}
			}
			@Override
			protected FormattedCharSequence formatChat(String original, int firstCharacterIndex) {
				return FormattedCharSequence.forward(original, Style.EMPTY);
			}
		};
		suggestor.currentParse = new ParseResults<>(null);
		
		setResponder(null);
	}
	public SuggestingTextFieldWidget(Screen screen, int x, int y, int width, int height) {
		this(screen, x, y, width, height, null);
	}
	
	@Override
	public void setResponder(Consumer<String> listener) {
		super.setResponder(str -> {
			suggestor.updateCommandInfo();
			if (listener != null)
				listener.accept(str);
		});
	}
	
	@Override
	public SuggestingTextFieldWidget name(Component name) {
		super.name(name);
		return this;
	}
	
	public SuggestingTextFieldWidget suggest(BiFunction<String, Integer, CompletableFuture<Suggestions>> suggestions) {
		this.suggestions = suggestions;
		suggestor.updateCommandInfo();
		return this;
	}
	
	@Override
	public void extractWidgetRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		if (!isDropdownOnly())
			super.extractWidgetRenderState(context, mouseX, mouseY, delta);
		suggestor.extractRenderState(context, mouseX, mouseY);
	}
	@Override
	protected boolean shouldShowName() {
		return suggestor.suggestions == null;
	}
	public boolean isDropdownOnly() {
		return false;
	}
	public Point getSpecialDropdownPos() {
		return null;
	}
	
	@Override
	public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
		double mouseX = click.x(); double mouseY = click.y(); int button = click.button();
		return suggestor.mouseClicked(click) || !isDropdownOnly() && super.mouseClicked(click, doubled);
	}
	
	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		return suggestor.mouseScrolled(verticalAmount) || !isDropdownOnly() && super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}
	
	@Override
	public boolean keyPressed(KeyEvent input) {
		if (!isMultiFocused())
			return false;
		return suggestor.keyPressed(input) || !isDropdownOnly() && super.keyPressed(input);
	}
	
	@Override
	public boolean isMouseOver(double mouseX, double mouseY) {
		if (suggestor.suggestions != null) {
			if (suggestor.suggestions.rect.contains((int) mouseX, (int) mouseY))
				return true;
		}
		return !isDropdownOnly() && super.isMouseOver(mouseX, mouseY);
	}
	
	@Override
	public void onMultiFocusedSet(boolean focused, boolean prevFocused) {
		suggestor.setAllowSuggestions(focused);
		suggestor.updateCommandInfo();
	}
	
}
