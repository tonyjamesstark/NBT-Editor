package com.luneruniverse.minecraft.mod.nbteditor.screens.widgets;

import java.util.Arrays;

import org.lwjgl.glfw.GLFW;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVDrawableHelper;
import com.luneruniverse.minecraft.mod.nbteditor.util.MainUtil;
import com.luneruniverse.minecraft.mod.nbteditor.util.TextUtil;

import com.luneruniverse.minecraft.mod.nbteditor.screens.widgets.Buttons;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;

public class AlertWidget extends GroupWidget implements InitializableOverlay<Screen> {
	
	private final Runnable onClose;
	private final Component[] lines;
	private int x;
	private int y;
	
	public AlertWidget(Runnable onClose, Component... lines) {
		this.onClose = onClose;
		this.lines = Arrays.stream(lines).flatMap(line -> TextUtil.splitText(line).stream()).toArray(Component[]::new);
	}
	
	@Override
	public void init(Screen parent, int width, int height) {
		clearWidgets();
		
		x = width / 2;
		y = height / 2 - lines.length * Minecraft.getInstance().font.lineHeight / 2;
		
		addWidget(Buttons.of(width / 2 - 50, height - 28, 100, 20, Component.translatableEscape("nbteditor.ok"), btn -> {
			onClose.run();
		}));
	}
	
	@Override
	public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		MVDrawableHelper.renderBackground(Minecraft.getInstance().gui.screen(), context);
		for (int i = 0; i < lines.length; i++) {
			MVDrawableHelper.drawCenteredTextWithShadow(context, Minecraft.getInstance().font, lines[i],
					x, y + i * Minecraft.getInstance().font.lineHeight, -1);
		}
		super.extractRenderState(context, mouseX, mouseY, delta);
		MainUtil.renderLogo(context);
	}
	
	@Override
	public boolean keyPressed(KeyEvent input) {
		int keyCode = input.key(); int scanCode = input.scancode(); int modifiers = input.modifiers();
		if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER) {
			onClose.run();
			return true;
		}
		
		return super.keyPressed(input);
	}
	
}
