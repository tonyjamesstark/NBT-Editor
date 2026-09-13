package com.luneruniverse.minecraft.mod.nbteditor.screens.widgets;

import java.util.Arrays;

import org.lwjgl.glfw.GLFW;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVDrawableHelper;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVMisc;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.TextInst;
import com.luneruniverse.minecraft.mod.nbteditor.util.MainUtil;
import com.luneruniverse.minecraft.mod.nbteditor.util.TextUtil;

import net.minecraft.client.input.KeyInput;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public class AlertWidget extends GroupWidget implements InitializableOverlay<Screen> {
	
	private final Runnable onClose;
	private final Text[] lines;
	private int x;
	private int y;
	
	public AlertWidget(Runnable onClose, Text... lines) {
		this.onClose = onClose;
		this.lines = Arrays.stream(lines).flatMap(line -> TextUtil.splitText(line).stream()).toArray(Text[]::new);
	}
	
	@Override
	public void init(Screen parent, int width, int height) {
		clearWidgets();
		
		x = width / 2;
		y = height / 2 - lines.length * MainUtil.client.textRenderer.fontHeight / 2;
		
		addWidget(MVMisc.newButton(width / 2 - 50, height - 28, 100, 20, TextInst.translatable("nbteditor.ok"), btn -> {
			onClose.run();
		}));
	}
	
	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		MVDrawableHelper.renderBackground(MainUtil.client.currentScreen, context);
		for (int i = 0; i < lines.length; i++) {
			MVDrawableHelper.drawCenteredTextWithShadow(context, MainUtil.client.textRenderer, lines[i],
					x, y + i * MainUtil.client.textRenderer.fontHeight, -1);
		}
		super.render(context, mouseX, mouseY, delta);
		MainUtil.renderLogo(context);
	}
	
	@Override
	public boolean keyPressed(KeyInput input) {
		int keyCode = input.key(); int scanCode = input.scancode(); int modifiers = input.modifiers();
		if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER) {
			onClose.run();
			return true;
		}
		
		return super.keyPressed(input);
	}
	
}
