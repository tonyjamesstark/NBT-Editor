package com.luneruniverse.minecraft.mod.nbteditor.screens.widgets;

import java.awt.Color;
import java.util.function.Consumer;

import org.lwjgl.glfw.GLFW;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVDrawableHelper;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVElement;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVSliderWidget;
import com.luneruniverse.minecraft.mod.nbteditor.util.MainUtil;

import net.minecraft.network.chat.Component;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class ColorSelectorWidget extends GroupWidget {
	
	public static class ColorSelectorInput extends GroupWidget implements InputOverlay.Input<Integer> {
		
		private int color;
		
		public ColorSelectorInput(int color) {
			this.color = color;
		}
		
		@Override
		public void init(int x, int y) {
			clearWidgets();
			addWidget(new ColorSelectorWidget(x, y, 128, color, newColor -> color = newColor));
		}
		
		@Override
		public Integer getValue() {
			return color;
		}
		
		@Override
		public boolean isValid() {
			return true;
		}
		
		@Override
		public int getWidth() {
			return 128 + 4 + 64;
		}
		
		@Override
		public int getHeight() {
			return 128 + 24;
		}
		
	}
	
	private static final Identifier HUES = Identifier.fromNamespaceAndPath("nbteditor", "textures/hues.png");
	
	private class ColorArea implements Renderable, MVElement {
		@Override
		public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
			// For a fixed hue, HSV is separable: each column is a straight fade from
			// full value down to black, so vanilla's gradient fill draws it exactly.
			// ponytail: one fill per column (128); a custom GUI render pipeline would
			// be one quad, at the cost of the shader stack this replaced.
			for (int i = 0; i < areaSize; i++) {
				int top = 0xFF000000 | Color.HSBtoRGB(hueValue / 360.0f, (float) i / areaSize, 1);
				context.fillGradient(x + i, y, x + i + 1, y + areaSize, top, 0xFF000000);
			}
		}
		@Override
		public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
			double mouseX = click.x(); double mouseY = click.y(); int button = click.button();
			if (button != GLFW.GLFW_MOUSE_BUTTON_1 || !isMouseOver(mouseX, mouseY))
				return false;
			mouseDragged(click, 0, 0);
			return true;
		}
		@Override
		public boolean mouseDragged(MouseButtonEvent click, double deltaX, double deltaY) {
			double mouseX = click.x(); double mouseY = click.y(); int button = click.button();
			if (button != GLFW.GLFW_MOUSE_BUTTON_1)
				return false;
			mouseX = Mth.clamp(mouseX, x, x + areaSize);
			mouseY = Mth.clamp(mouseY, y, y + areaSize);
			color = Color.HSBtoRGB(hueValue / 360.0f, (float) (mouseX - x) / areaSize, 1 - (float) (mouseY - y) / areaSize);
			field.setValue("#" + String.format("%08X", color).substring(2, 8)); // Calls onColor
			return true;
		}
		@Override
		public boolean isMouseOver(double mouseX, double mouseY) {
			return mouseX >= x && mouseX <= x + areaSize && mouseY >= y && mouseY <= y + areaSize;
		}
	}
	
	private final int x;
	private final int y;
	private final int areaSize;
	private final EditBox field;
	private int color;
	private int hueValue;
	
	public ColorSelectorWidget(int x, int y, int areaSize, int color, Consumer<Integer> onColor) {
		this.x = x;
		this.y = y;
		this.areaSize = areaSize;
		this.color = color;
		
		addWidget(new ColorArea());
		
		Color colorObj = new Color(color);
		hueValue = (int) (Color.RGBtoHSB(colorObj.getRed(), colorObj.getGreen(), colorObj.getBlue(), new float[3])[0] * 360);
		addWidget(new MVSliderWidget(x, y + areaSize + 4, areaSize, 20, hueValue / 359.0,
				() -> Component.translatableEscape("nbteditor.color_selector.hue", hueValue), value -> hueValue = (int) (value * 359)) {
			@Override
			protected boolean renderSlider(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
				MVDrawableHelper.drawTexture(context, HUES, x + 4, y, 0, 0, width - 8, 20, width - 8, 20);
				return true;
			}
			@Override
			public boolean keyPressed(KeyEvent input) {
				int keyCode = input.key();
				if (keyCode == GLFW.GLFW_KEY_RIGHT) {
					setValue(getValue() + 1 / 359.0);
					return true;
				}
				if (keyCode == GLFW.GLFW_KEY_LEFT) {
					setValue(getValue() - 1 / 359.0);
					return true;
				}
				return false;
			}
		});
		
		field = new EditBox(MainUtil.client.font, x + areaSize + 4, y + areaSize + 4, areaSize / 2, 20, Component.nullToEmpty(""));
		field.setMaxLength(7);
		field.setValue("#" + String.format("%08X", color).substring(2, 8));
		field.setResponder(str -> {
			if (!str.matches("#[0-9a-fA-F]{6}"))
				return;
			this.color = Integer.parseInt(str.substring(1), 16);
			onColor.accept(this.color);
		});
		addWidget(field);
	}
	
	@Override
	public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		super.extractRenderState(context, mouseX, mouseY, delta);
		MVDrawableHelper.fill(context, x + areaSize + 4, y, x + areaSize + 4 + areaSize / 2, y + areaSize, color | 0xFF000000);
	}
	
}
