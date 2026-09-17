package com.luneruniverse.minecraft.mod.nbteditor.screens.widgets;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.luneruniverse.minecraft.mod.nbteditor.util.Drawing;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVElement;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVTooltip;
import com.luneruniverse.minecraft.mod.nbteditor.screens.ConfigScreen;

import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.Identifier;

public class CreativeTabWidget implements Renderable, MVElement {
	
	/**
	 * The icon is a supplier because 26.2 binds item components at world load, so an
	 * ItemStack cannot exist yet when a tab is registered during client init.
	 */
	public static record CreativeTabData(Supplier<ItemStack> item, Runnable onClick, Predicate<Screen> whenToShow) {}
	public static final List<CreativeTabData> TABS = new ArrayList<>();
	
	public static void addCreativeTabs(Screen screen) {
		List<CreativeTabWidget.CreativeTabData> tabs = TABS.stream().filter(tab -> tab.whenToShow().test(screen)).toList();
		if (!tabs.isEmpty()) {
			GroupWidget group = new GroupWidget() {
				@Override
				public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
					MVTooltip.setOneTooltip(true, false);
					super.extractRenderState(context, mouseX, mouseY, delta);
					MVTooltip.renderOneTooltip(context, mouseX, mouseY);
				}
			};
			for (int i = 0; i < tabs.size(); i++) {
				CreativeTabWidget.CreativeTabData tab = tabs.get(i);
				Point pos = ConfigScreen.getCreativeTabsPos().position(i, tabs.size(), screen.width, screen.height);
				group.addWidget(new CreativeTabWidget(ConfigScreen.getCreativeTabsPos().isTop(), pos.x, pos.y, tab.item().get(), tab.onClick()));
			}
			screen.addRenderableWidget(group);
		}
	}
	
	public static final int WIDTH = 26;
	public static final int HEIGHT = 32;
	
	private static final Identifier TEXTURE_TOP;
	private static final Identifier TEXTURE_BOTTOM;
	private static final int V_TOP;
	private static final int V_BOTTOM;
	static {
		TEXTURE_TOP = Identifier.fromNamespaceAndPath("nbteditor", "textures/gui/sprites/container/creative_inventory/tab_top_unselected.png");
		TEXTURE_BOTTOM = Identifier.fromNamespaceAndPath("nbteditor", "textures/gui/sprites/container/creative_inventory/tab_bottom_unselected.png");
		V_TOP = 0;
		V_BOTTOM = 0;
	}
	
	private final boolean bottom;
	private final int x;
	private final int y;
	private final ItemStack item;
	private final Runnable onClick;
	private final MVTooltip tooltip;
	
	public CreativeTabWidget(boolean bottom, int x, int y, ItemStack item, Runnable onClick) {
		this.bottom = bottom;
		this.x = x;
		this.y = y;
		this.item = item;
		this.onClick = onClick;
		this.tooltip = new MVTooltip(item.getHoverName());
	}
	
	@Override
	public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		Drawing.drawTexture(context, bottom ? TEXTURE_BOTTOM : TEXTURE_TOP, x, y + (bottom ? 0 : 2), 0, bottom ? V_BOTTOM : V_TOP, WIDTH, 32);
		
		int xOffset = 5;
		Drawing.renderItem(context, 100.0F, false, item, x + xOffset, y + (bottom ? 5 : 11));
		
		if (isMouseOver(mouseX, mouseY))
			tooltip.render(context, mouseX, mouseY);
	}
	
	@Override
	public boolean isMouseOver(double mouseX, double mouseY) {
		return x <= mouseX && mouseX < x + WIDTH && y <= mouseY && mouseY < y + HEIGHT;
	}
	
	public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
		double mouseX = click.x(); double mouseY = click.y();
		if (isMouseOver(mouseX, mouseY)) {
			onClick.run();
			return true;
		}
		
		return false;
	}
	
}
