package com.luneruniverse.minecraft.mod.nbteditor.fancytext;

import java.util.List;
import java.util.UUID;

import com.luneruniverse.minecraft.mod.nbteditor.util.AccessWidenedApi;
import com.luneruniverse.minecraft.mod.nbteditor.util.TextEvents;
import com.luneruniverse.minecraft.mod.nbteditor.nbtreferences.itemreferences.ItemReference;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.IdentifierException;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.client.Minecraft;

public record FancyTextStyleOptionNode(StyleOption option, String value, List<FancyTextNode> contents) implements FancyTextNode {
	
	@Override
	public Style modifyStyle(Style style) {
		return switch (option) {
			case OPEN_URL, RUN_COMMAND, SUGGEST_COMMAND, CHANGE_PAGE, COPY_TO_CLIPBOARD -> TextEvents.ClickAction.fromName(
					option.name().toLowerCase()).newEventParse(value == null ? "" : value).map(style::withClickEvent).orElse(style);
			case SHOW_TEXT -> style.withHoverEvent(
					TextEvents.HoverAction.SHOW_TEXT.newEvent(value == null ? Component.nullToEmpty("") : FancyText.parse(value)));
			case SHOW_ITEM -> {
				ItemStack item;
				try {
					item = Minecraft.getInstance().player.getInventory().getItem(Integer.parseInt(value));
				} catch (NumberFormatException | IndexOutOfBoundsException e) {
					try {
						item = ItemReference.getHeldItem().getItem();
					} catch (CommandSyntaxException e2) {
						item = ItemStack.EMPTY;
					}
				}
				// ItemStackTemplate's constructor rejects air and a zero count outright, so an
				// empty result here would throw rather than show an empty tooltip.
				if (item.isEmpty())
					yield style;
				yield style.withHoverEvent(TextEvents.HoverAction.SHOW_ITEM.newEvent(ItemStackTemplate.fromStack(item)));
			}
			case SHOW_ENTITY -> {
				Entity entity;
				try {
					if (value == null)
						throw new IllegalArgumentException();
					String uuid = value;
					if (!uuid.contains("-"))
						uuid = uuid.replaceFirst("(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)", "$1-$2-$3-$4-$5");
					entity = AccessWidenedApi.getEntityByUuid(Minecraft.getInstance().level, UUID.fromString(uuid));
					if (entity == null)
						throw new IllegalArgumentException();
				} catch (IllegalArgumentException e) {
					if (Minecraft.getInstance().crosshairPickEntity != null)
						entity = Minecraft.getInstance().crosshairPickEntity;
					else
						entity = Minecraft.getInstance().player;
				}
				yield style.withHoverEvent(TextEvents.HoverAction.SHOW_ENTITY.newEvent(
						new HoverEvent.EntityTooltipInfo(entity.getType(), entity.getUUID(), entity.getName())));
			}
			case INSERTION -> style.withInsertion(value);
			case FONT -> {
				if (value == null)
					yield style.withFont(FontDescription.DEFAULT);
				try {
					yield style.withFont(new FontDescription.Resource(Identifier.parse(value)));
				} catch (IdentifierException e) {
					yield style.withFont(FontDescription.DEFAULT);
				}
			}
		};
	}
	
	@Override
	public int getNumberOfTextNodes() {
		return contents.stream().mapToInt(FancyTextNode::getNumberOfTextNodes).sum();
	}
	
}
