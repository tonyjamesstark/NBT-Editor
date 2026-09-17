package com.luneruniverse.minecraft.mod.nbteditor.multiversion;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.lwjgl.glfw.GLFW;

import com.luneruniverse.minecraft.mod.nbteditor.util.MainUtil;

import net.minecraft.client.gui.Click;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public class MVSliderWidget extends MVButtonWidget {
	
	private static final Identifier HANDLE = IdentifierInst.of("nbteditor", "textures/slider_handle.png");
	private static final Identifier HANDLE_HIGHLIGHTED = IdentifierInst.of("nbteditor", "textures/slider_handle_highlighted.png");
	
	private double value;
	private final Supplier<Text> msg;
	private final Consumer<Double> onValue;
	
	public MVSliderWidget(int x, int y, int width, int height, double value, Supplier<Text> msg, Consumer<Double> onValue, MVTooltip tooltip) {
		super(x, y, width, height, msg.get(), btn -> {}, tooltip);
		this.value = value;
		this.msg = msg;
		this.onValue = onValue;
	}
	public MVSliderWidget(int x, int y, int width, int height, double value, Supplier<Text> msg, Consumer<Double> onValue) {
		this(x, y, width, height, value, msg, onValue, null);
	}
	
	public double getValue() {
		return value;
	}
	public void setValue(double value) {
		this.value = MathHelper.clamp(value, 0, 1);
		onValue.accept(this.value);
		setMessage(msg.get());
	}
	private void setValueFromMouse(double mouseX) {
		setValue((mouseX - x - 4) / (width - 8));
	}
	
	@Override
	public void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
		if (renderSlider(context, mouseX, mouseY, delta)) {
			MVDrawableHelper.drawTexture(context, this.hovered || this.isFocused() ? HANDLE_HIGHLIGHTED : HANDLE,
					x + (int) (value * (width - 8)), y, 0, 0, 8, 20, 8, 20);
			MVDrawableHelper.drawCenteredTextWithShadow(context, MainUtil.client.textRenderer, getMessage(),
					x + width / 2, y + height / 2 - MainUtil.client.textRenderer.fontHeight / 2, -1);
		} else {
			new SliderWidget(x, y, width, height, getMessage(), value) {
				@Override
				protected void updateMessage() {}
				@Override
				protected void applyValue() {}
			}.render(context, mouseX, mouseY, delta);
		}
	}
	protected boolean renderSlider(DrawContext context, int mouseX, int mouseY, float delta) {
		return false;
	}
	
	@Override
	public boolean mouseClicked(Click click, boolean doubled) {
		double mouseX = click.x(); double mouseY = click.y(); int button = click.button();
		if (button != GLFW.GLFW_MOUSE_BUTTON_1 || !isMouseOver(mouseX, mouseY))
			return false;
		setValueFromMouse(mouseX);
		return true;
	}
	
	@Override
	public boolean mouseDragged(Click click, double deltaX, double deltaY) {
		double mouseX = click.x(); int button = click.button();
		if (button != GLFW.GLFW_MOUSE_BUTTON_1)
			return false;
		setValueFromMouse(mouseX);
		return true;
	}
	
	@Override
	public boolean mouseReleased(Click click) {
		int button = click.button();
		if (button != GLFW.GLFW_MOUSE_BUTTON_1)
			return false;
		playDownSound(MinecraftClient.getInstance().getSoundManager());
		return true;
	}
	
}
