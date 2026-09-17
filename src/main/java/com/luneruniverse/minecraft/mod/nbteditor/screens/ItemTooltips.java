package com.luneruniverse.minecraft.mod.nbteditor.screens;

import java.awt.Color;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

import com.luneruniverse.minecraft.mod.nbteditor.NBTEditorClient;
import com.luneruniverse.minecraft.mod.nbteditor.async.ItemSize;
import com.luneruniverse.minecraft.mod.nbteditor.containers.ContainerIOs;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.TextInst;
import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.ItemTagReferences;
import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.specific.data.hideflags.HideFlag;
import com.luneruniverse.minecraft.mod.nbteditor.util.ItemSizeText;
import com.luneruniverse.minecraft.mod.nbteditor.util.MainUtil;
import com.luneruniverse.minecraft.mod.nbteditor.util.TooltipPlacement;
import com.luneruniverse.minecraft.mod.nbteditor.util.TooltipPlacement.Rect;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The lines the mod adds to an item tooltip, and where an oversized tooltip is drawn.
 *
 * <p>Called from the item and rendering mixins. The geometry and the size readout live in
 * {@link TooltipPlacement} and {@link ItemSizeText}, which are testable without a game runtime.
 */
public class ItemTooltips {

	/** Adds the item size line and the keybind hints, subject to config and hide flags. */
	public static void modifyTooltip(ItemStack source, List<Component> tooltip) {
		// Tooltips are requested for all items when GameJoinS2CPacket is received to setup the creative inventory's search
		// The world doesn't exist yet, so this causes the game to freeze when an exception from this mixin breaks everything
		if (MainUtil.client.level == null)
			return;

		if (HideFlag.TOOLTIP != null && ItemTagReferences.HIDE_FLAGS.get(source).get(HideFlag.TOOLTIP))
			return;

		ConfigScreen.ItemSizeFormat sizeConfig = ConfigScreen.getItemSizeFormat();
		if (sizeConfig != ConfigScreen.ItemSizeFormat.HIDDEN) {
			OptionalLong loadingSize = ItemSize.getItemSize(source, sizeConfig.isCompressed());
			String displaySize;
			Optional<ChatFormatting> sizeFormat;
			if (loadingSize.isEmpty()) {
				displaySize = "...";
				sizeFormat = Optional.of(ChatFormatting.GRAY);
			} else {
				ItemSizeText.Rendered rendered = ItemSizeText.render(loadingSize.getAsLong(), sizeConfig.getMagnitude());
				displaySize = rendered.text();
				sizeFormat = rendered.color();
			}
			TextColor sizeColor = sizeFormat.map(TextColor::fromLegacyFormat).orElseGet(
					() -> TextColor.fromRgb(Color.HSBtoRGB((System.currentTimeMillis() % 1000) / 1000.0f, 1, 1)));
			tooltip.add(TextInst.translatable("nbteditor.item_size." + (sizeConfig.isCompressed() ? "compressed" : "uncompressed"),
					TextInst.literal(displaySize).withStyle(style -> style.withColor(sizeColor))));
		}

		if (!ConfigScreen.isKeybindsHidden()) {
			// Checking slots in your hotbar vs item selection is difficult, so the lore is just disabled in non-inventory tabs
			boolean creativeInv = MainUtil.client.gui.screen() instanceof CreativeModeInventoryScreen creative
					&& creative.isInventoryOpen();

			if (creativeInv || (!(MainUtil.client.gui.screen() instanceof CreativeModeInventoryScreen) &&
					NBTEditorClient.SERVER_CONN.isScreenEditable())) {
				tooltip.add(TextInst.translatable("nbteditor.keybind.edit"));
				tooltip.add(TextInst.translatable("nbteditor.keybind.factory"));
				if (ContainerIOs.isSupported(source))
					tooltip.add(TextInst.translatable("nbteditor.keybind.container"));
				if (source.getItem() == Items.ENCHANTED_BOOK)
					tooltip.add(TextInst.translatable("nbteditor.keybind.enchant"));
				tooltip.add(TextInst.translatable("nbteditor.keybind.delete"));
			}
		}
	}

	/** Measures a laid-out tooltip as {@code {width, height}}. */
	public static int[] getTooltipSize(List<ClientTooltipComponent> tooltip) {
		int width = 0;
		int height = (tooltip.size() == 1 ? -2 : 0);
		for (ClientTooltipComponent line : tooltip) {
			width = Math.max(width, line.getWidth(MainUtil.client.font));
			height += line.getHeight(MainUtil.client.font);
		}
		return new int[] {width, height};
	}

	/** Maps the tooltip onto the screen, scaling and repositioning it if it does not fit. */
	public static void renderTooltipFromComponents(GuiGraphicsExtractor context, int x, int y, int width, int height, int screenWidth, int screenHeight) {
		int[] mousePos = MainUtil.getMousePos();
		TooltipPlacement placement = TooltipPlacement.fit(x, y, width, height, screenWidth, screenHeight,
				mousePos[0], mousePos[1]);
		Rect source = placement.source();
		Rect target = placement.target();
		MainUtil.mapMatrices(context, source.x(), source.y(), source.width(), source.height(),
				target.x(), target.y(), target.width(), target.height());
	}

}
