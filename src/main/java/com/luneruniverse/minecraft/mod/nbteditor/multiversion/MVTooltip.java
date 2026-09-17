package com.luneruniverse.minecraft.mod.nbteditor.multiversion;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.joml.Matrix3x2fStack;

import com.luneruniverse.minecraft.mod.nbteditor.misc.MixinLink;
import com.luneruniverse.minecraft.mod.nbteditor.mixin.TooltipAccessor;
import com.luneruniverse.minecraft.mod.nbteditor.util.TextUtil;

import net.minecraft.network.chat.MutableComponent;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.Component;
import com.luneruniverse.minecraft.mod.nbteditor.util.Drawing;

public class MVTooltip {
	
	public static final MVTooltip EMPTY = new MVTooltip(new Component[0]);
	private static boolean oneTooltip = false;
	private static boolean lastTooltip = false;
	private static MVTooltip theOneTooltip;
	
	public static MVTooltip setOneTooltip(boolean oneTooltip, boolean lastTooltip) {
		MVTooltip.oneTooltip = oneTooltip;
		MVTooltip.lastTooltip = lastTooltip;
		MVTooltip output = theOneTooltip;
		theOneTooltip = null;
		return output;
	}
	public static boolean isOneTooltip() {
		return oneTooltip;
	}
	public static boolean isLastTooltip() {
		return lastTooltip;
	}
	public static MVTooltip getTheOneTooltip() {
		return theOneTooltip;
	}
	public static boolean setExternalOneTooltip(List<FormattedCharSequence> tooltip) {
		if (isOneTooltip()) {
			if (lastTooltip || theOneTooltip == null)
				theOneTooltip = new MVTooltip(tooltip, null);
			return true;
		}
		return false;
	}
	public static boolean renderOneTooltip(GuiGraphicsExtractor context, int mouseX, int mouseY) {
		MVTooltip tooltip = setOneTooltip(false, false);
		if (tooltip == null)
			return false;
		tooltip.render(context, mouseX, mouseY);
		return true;
	}
	
	private static Component combine(List<Component> lines) {
		MutableComponent combined = Component.literal("");
		for (int i = 0; i < lines.size(); i++) {
			if (i > 0)
				combined = combined.append(" ");
			combined = combined.append(lines.get(i));
		}
		return combined;
	}
	
	private final List<FormattedCharSequence> lines;
	private final Component combined;
	
	private MVTooltip(List<FormattedCharSequence> lines, Component combined) {
		this.lines = lines;
		this.combined = combined;
	}
	public MVTooltip(List<Component> lines) {
		this(lines.stream().map(Component::getVisualOrderText).collect(Collectors.toList()), combine(lines));
	}
	public MVTooltip(Component... lines) {
		this(Arrays.stream(lines).flatMap(line -> TextUtil.splitText(line).stream()).toList());
	}
	public MVTooltip(String... keys) {
		this(Arrays.asList(keys).stream().map(Component::translatableEscape).toList().toArray(new MutableComponent[0]));
	}
	
	public List<FormattedCharSequence> getLines() {
		return lines;
	}
	
	public Component getCombined() {
		return combined;
	}
	
	public boolean isEmpty() {
		return this == EMPTY || lines.isEmpty();
	}
	
	public Tooltip toNewTooltip() {
		if (isEmpty())
			return null;
		
		Tooltip output = Tooltip.create(combined);
		((TooltipAccessor) (Object) output).setCachedTooltip(lines);
		MixinLink.NEW_TOOLTIPS.put(output, true);
		return output;
	}
	
	public void render(GuiGraphicsExtractor context, int mouseX, int mouseY) {
		if (oneTooltip) {
			if (lastTooltip || theOneTooltip == null)
				theOneTooltip = this;
			return;
		}
		
		// Undo translations and render at actual position
		// This allows Screen#renderTooltip to adjust for window height
		Matrix3x2fStack matrices = context.pose();
		float dx = matrices.m20();
		float dy = matrices.m21();
		matrices.pushMatrix();
		matrices.translate(-dx, -dy);
		Drawing.renderTooltip(context, lines, mouseX + (int) dx, mouseY + (int) dy);
		matrices.popMatrix();
	}
	
}
