package com.luneruniverse.minecraft.mod.nbteditor.screens.widgets;

import java.util.function.Consumer;

import org.lwjgl.glfw.GLFW;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVDrawableHelper;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVElement;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVMisc;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.TextInst;
import com.luneruniverse.minecraft.mod.nbteditor.screens.OverlayScreen;
import com.luneruniverse.minecraft.mod.nbteditor.screens.OverlaySupportingScreen;
import com.luneruniverse.minecraft.mod.nbteditor.util.MainUtil;

import net.minecraft.client.input.KeyInput;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.text.Text;

public class InputOverlay<T> extends GroupWidget implements InitializableOverlay<Screen> {
	
	public static interface Input<T> extends Drawable, MVElement {
		public void init(int x, int y);
		public int getWidth();
		public int getHeight();
		public T getValue();
		public boolean isValid();
	}
	
	public static <T> void show(Text title, Input<T> input, Consumer<T> valueConsumer) {
		OverlayScreen.setOverlayOrScreen(
				new InputOverlay<>(title, input, valueConsumer, () -> OverlaySupportingScreen.setOverlayStatic(null)), true);
	}
	
	private final Text title;
	private final Input<T> input;
	private final Consumer<T> valueConsumer;
	private final Runnable close;
	private int x;
	private int y;
	private ButtonWidget ok;
	
	public InputOverlay(Text title, Input<T> input, Consumer<T> valueConsumer, Runnable close) {
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
		
		ok = addWidget(MVMisc.newButton(x, y + input.getHeight() + 4,
				(input.getWidth() - 4) / 2, 20, TextInst.translatable("nbteditor.ok"), btn -> {
			close.run();
			valueConsumer.accept(input.getValue());
		}));
		addWidget(MVMisc.newButton(width / 2 + 2, y + input.getHeight() + 4,
				(input.getWidth() - 4) / 2, 20, TextInst.translatable("nbteditor.cancel"), btn -> close.run()));
		
		ok.active = input.isValid();
	}
	
	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		ok.active = input.isValid();
		
		context.getMatrices().pushMatrix();
		context.getMatrices().translate((float) (0.0), (float) (0.0));
		MVDrawableHelper.renderBackground(MainUtil.client.currentScreen, context);
		if (title != null) {
			MVDrawableHelper.drawCenteredTextWithShadow(context, MainUtil.client.textRenderer, title,
					x + input.getWidth() / 2, y - 4 - MainUtil.client.textRenderer.fontHeight, -1);
		}
		super.render(context, mouseX, mouseY, delta);
		MainUtil.renderLogo(context);
		context.getMatrices().popMatrix();
	}
	
	@Override
	public boolean keyPressed(KeyInput keyInput) {
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
