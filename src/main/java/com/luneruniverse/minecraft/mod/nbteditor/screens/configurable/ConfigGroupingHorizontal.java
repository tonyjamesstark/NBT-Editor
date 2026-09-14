package com.luneruniverse.minecraft.mod.nbteditor.screens.configurable;

import java.util.ArrayList;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVDrawableHelper;
import com.luneruniverse.minecraft.mod.nbteditor.util.MainUtil;

import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public abstract class ConfigGroupingHorizontal<K, T extends ConfigGroupingHorizontal<K, T>> extends ConfigGrouping<K, T> {
	
	protected ConfigGroupingHorizontal(Component name, Constructor<K, T> cloneImpl) {
		super(name, cloneImpl);
	}
	
	protected int getNameWidth() {
		return name == null ? 0 : MainUtil.client.font.width(name) + PADDING;
	}
	
	@Override
	public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		int xOffset = 0;
		Component fullName = getFullName();
		if (fullName != null) {
			MVDrawableHelper.drawTextWithShadow(context, MainUtil.client.font, fullName, PADDING * 2, 0, 0xFFFFFFFF);
			xOffset += getNameWidth();
		}
		
		for (ConfigPath path : new ArrayList<>(paths.values())) {
			context.pose().pushMatrix();
			context.pose().translate((float) (xOffset), (float) (0.0));
			path.extractRenderState(context, mouseX - xOffset, mouseY, delta);
			context.pose().popMatrix();
			
			xOffset += path.getSpacingWidth() + PADDING;
		}
	}
	
	@Override
	public int getSpacingWidth() {
		return getNameWidth() + paths.values().stream().mapToInt(Configurable::getSpacingWidth).reduce((a, b) -> a + PADDING + b).orElse(0);
	}
	
	@Override
	public int getSpacingHeight() {
		int output = 0;
		for (ConfigPath path : paths.values()) {
			int height = path.getSpacingHeight();
			if (height > output)
				output = height;
		}
		return output;
	}
	
	@Override
	public int getRenderWidth() {
		int output = getNameWidth();
		int xOffset = output;
		for (ConfigPath path : paths.values()) {
			int rightX = xOffset + path.getRenderWidth();
			if (rightX > output)
				output = rightX;
			xOffset += path.getSpacingWidth() + PADDING;
		}
		return output;
	}
	
	@Override
	public int getRenderHeight() {
		int output = 0;
		for (ConfigPath path : paths.values()) {
			int height = path.getRenderHeight();
			if (height > output)
				output = height;
		}
		return output;
	}
	
	@Override
	public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
		double mouseX = click.x(); double mouseY = click.y(); int button = click.button();
		int xOffset = getNameWidth();
		
		for (ConfigPath path : new ArrayList<>(paths.values())) {
			if (path.mouseClicked(new MouseButtonEvent(mouseX - xOffset, mouseY, click.buttonInfo()), doubled))
				return true;
			xOffset += path.getSpacingWidth() + PADDING;
		}
		return false;
	}
	@Override
	public boolean mouseReleased(MouseButtonEvent click) {
		double mouseX = click.x(); double mouseY = click.y(); int button = click.button();
		int xOffset = getNameWidth();
		
		for (ConfigPath path : new ArrayList<>(paths.values())) {
			if (path.mouseReleased(new MouseButtonEvent(mouseX - xOffset, mouseY, click.buttonInfo())))
				return true;
			xOffset += path.getSpacingWidth() + PADDING;
		}
		return false;
	}
	@Override
	public void mouseMoved(double mouseX, double mouseY) {
		int xOffset = getNameWidth();
		
		for (ConfigPath path : new ArrayList<>(paths.values())) {
			path.mouseMoved(mouseX - xOffset, mouseY);
			xOffset += path.getSpacingWidth() + PADDING;
		}
	}
	@Override
	public boolean mouseDragged(MouseButtonEvent click, double deltaX, double deltaY) {
		double mouseX = click.x(); double mouseY = click.y(); int button = click.button();
		int xOffset = getNameWidth();
		
		for (ConfigPath path : new ArrayList<>(paths.values())) {
			if (path.mouseDragged(new MouseButtonEvent(mouseX - xOffset, mouseY, click.buttonInfo()), deltaX, deltaY))
				return true;
			xOffset += path.getSpacingWidth() + PADDING;
		}
		return false;
	}
	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double xAmount, double yAmount) {
		int xOffset = getNameWidth();
		
		for (ConfigPath path : new ArrayList<>(paths.values())) {
			if (path.mouseScrolled(mouseX - xOffset, mouseY, xAmount, yAmount))
				return true;
			xOffset += path.getSpacingWidth() + PADDING;
		}
		return false;
	}
	
}
