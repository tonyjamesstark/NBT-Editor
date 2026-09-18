package com.luneruniverse.minecraft.mod.nbteditor.screens.configurable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;

public abstract class ConfigGroupingVertical<K, T extends ConfigGroupingVertical<K, T>> extends ConfigGrouping<K, T> {
	
	protected ConfigGroupingVertical(Component name, Constructor<K, T> cloneImpl) {
		super(name, cloneImpl);
	}
	
	protected int getNameHeight() {
		return name == null ? 0 : Minecraft.getInstance().font.lineHeight + PADDING;
	}
	
	@Override
	public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		context.fill(0, 0, PADDING, getSpacingHeight(), isValueValid() ? 0xFFAAAAAA : 0xFFDF4949);
		
		int yOffset = 0;
		Component fullName = getFullName();
		if (fullName != null) {
			context.text(Minecraft.getInstance().font, fullName, PADDING * 2, 0, 0xFFFFFFFF);
			yOffset += getNameHeight();
		}
		
		// Render in reverse order to allow dropdowns to display over below components
		List<ConfigPath> paths = new ArrayList<>(this.paths.values());
		Collections.reverse(paths);
		for (ConfigPath path : paths)
			yOffset += path.getSpacingHeight() + PADDING;
		
		for (ConfigPath path : paths) {
			yOffset -= path.getSpacingHeight() + PADDING;
			
			context.pose().pushMatrix();
			context.pose().translate((float) (PADDING * 2), (float) (yOffset));
			path.extractRenderState(context, mouseX - PADDING * 2, mouseY - yOffset, delta);
			context.pose().popMatrix();
		}
	}
	
	@Override
	public int getSpacingWidth() {
		int output = 0;
		for (ConfigPath path : paths.values()) {
			int width = path.getSpacingWidth();
			if (width > output)
				output = width;
		}
		return output;
	}
	
	@Override
	public int getSpacingHeight() {
		return getNameHeight() + paths.values().stream().mapToInt(Configurable::getSpacingHeight).reduce((a, b) -> a + PADDING + b).orElse(0);
	}
	
	@Override
	public int getRenderWidth() {
		int output = 0;
		for (ConfigPath path : paths.values()) {
			int width = path.getRenderWidth();
			if (width > output)
				output = width;
		}
		return output;
	}
	
	@Override
	public int getRenderHeight() {
		int output = getNameHeight();
		int yOffset = output;
		for (ConfigPath path : paths.values()) {
			int bottomY = yOffset + path.getRenderHeight();
			if (bottomY > output)
				output = bottomY;
			yOffset += path.getSpacingHeight() + PADDING;
		}
		return output;
	}
	
	@Override
	public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
		double mouseX = click.x(); double mouseY = click.y(); int button = click.button();
		int yOffset = getNameHeight();
		
		for (ConfigPath path : new ArrayList<>(paths.values())) {
			if (path.mouseClicked(new MouseButtonEvent(mouseX - PADDING * 2, mouseY - yOffset, click.buttonInfo()), doubled))
				return true;
			yOffset += path.getSpacingHeight() + PADDING;
		}
		return false;
	}
	@Override
	public boolean mouseReleased(MouseButtonEvent click) {
		double mouseX = click.x(); double mouseY = click.y(); int button = click.button();
		int yOffset = getNameHeight();
		
		for (ConfigPath path : new ArrayList<>(paths.values())) {
			if (path.mouseReleased(new MouseButtonEvent(mouseX - PADDING * 2, mouseY - yOffset, click.buttonInfo())))
				return true;
			yOffset += path.getSpacingHeight() + PADDING;
		}
		return false;
	}
	@Override
	public void mouseMoved(double mouseX, double mouseY) {
		int yOffset = getNameHeight();
		
		for (ConfigPath path : new ArrayList<>(paths.values())) {
			path.mouseMoved(mouseX - PADDING * 2, mouseY - yOffset);
			yOffset += path.getSpacingHeight() + PADDING;
		}
	}
	@Override
	public boolean mouseDragged(MouseButtonEvent click, double deltaX, double deltaY) {
		double mouseX = click.x(); double mouseY = click.y(); int button = click.button();
		int yOffset = getNameHeight();
		
		for (ConfigPath path : new ArrayList<>(paths.values())) {
			// Buttons return true by default, causing problems with returning early
			path.mouseDragged(new MouseButtonEvent(mouseX - PADDING * 2, mouseY - yOffset, click.buttonInfo()), deltaX, deltaY);
			yOffset += path.getSpacingHeight() + PADDING;
		}
		return false;
	}
	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double xAmount, double yAmount) {
		int yOffset = getNameHeight();
		
		for (ConfigPath path : new ArrayList<>(paths.values())) {
			if (path.mouseScrolled(mouseX - PADDING * 2, mouseY - yOffset, xAmount, yAmount))
				return true;
			yOffset += path.getSpacingHeight() + PADDING;
		}
		return false;
	}
	
}
