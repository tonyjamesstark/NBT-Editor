package com.luneruniverse.minecraft.mod.nbteditor.screens.widgets;

import java.util.function.Consumer;

import org.lwjgl.glfw.GLFW;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVDrawableHelper;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVElement;
import com.luneruniverse.minecraft.mod.nbteditor.screens.OverlayScreen;
import com.luneruniverse.minecraft.mod.nbteditor.screens.OverlaySupportingScreen;
import com.luneruniverse.minecraft.mod.nbteditor.util.MainUtil;

import com.luneruniverse.minecraft.mod.nbteditor.screens.widgets.Buttons;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;

public class InputOverlay<T> extends GroupWidget implements InitializableOverlay<Screen> {
	
	public static interface Input<T> extends Renderable, MVElement {
		public void init(int x, int y);
		public int getWidth();
		public int getHeight();
		public T getValue();
		public boolean isValid();
	}
	
	public static <T> void show(Component title, Input<T> input, Consumer<T> valueConsumer) {
		OverlayScreen.setOverlayOrScreen(
				new InputOverlay<>(title, input, valueConsumer, () -> OverlaySupportingScreen.setOverlayStatic(null)), true);
	}
	
	private final Component title;
	private final Input<T> input;
	private final Consumer<T> valueConsumer;
	private final Runnable close;
	private int x;
	private int y;
	private Button ok;
	
	public InputOverlay(Component title, Input<T> input, Consumer<T> valueConsumer, Runnable close) {
		this.title = title;
		this.input = input;
		this.valueConsumer = valueConsumer;
		this.close = close;
	}
	
	@Override
	public void init(Screen parent, int width, int height) {
		clearWidgets();
		
		x = (width - input.getWidth()) / 2;
		y = (height - input.getHeight() - 24) / 2;
		input.init(x, y);
		addWidget(input);
		setFocused(input);
		
		ok = addWidget(Buttons.of(x, y + input.getHeight() + 4,
				(input.getWidth() - 4) / 2, 20, Component.translatableEscape("nbteditor.ok"), btn -> {
			close.run();
			valueConsumer.accept(input.getValue());
		}));
		addWidget(Buttons.of(width / 2 + 2, y + input.getHeight() + 4,
				(input.getWidth() - 4) / 2, 20, Component.translatableEscape("nbteditor.cancel"), btn -> close.run()));
		
		ok.active = input.isValid();
	}
	
	@Override
	public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		ok.active = input.isValid();
		
		context.pose().pushMatrix();
		context.pose().translate((float) (0.0), (float) (0.0));
		MVDrawableHelper.renderBackground(Minecraft.getInstance().gui.screen(), context);
		if (title != null) {
			MVDrawableHelper.drawCenteredTextWithShadow(context, Minecraft.getInstance().font, title,
					x + input.getWidth() / 2, y - 4 - Minecraft.getInstance().font.lineHeight, -1);
		}
		super.extractRenderState(context, mouseX, mouseY, delta);
		MainUtil.renderLogo(context);
		context.pose().popMatrix();
	}
	
	@Override
	public boolean keyPressed(KeyEvent keyInput) {
		int keyCode = keyInput.key();
		if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
			close.run();
			return true;
		}
		if (keyCode == GLFW.GLFW_KEY_ENTER && ok.active) {
			close.run();
			valueConsumer.accept(input.getValue());
			return true;
		}
		
		return super.keyPressed(keyInput);
	}
	
}
