package com.luneruniverse.minecraft.mod.nbteditor.util;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVRegistry;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.util.Set;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderOwner;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTagVisitor;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.LecternMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;

/**
 * The calls that only compile because {@code nbteditor.accesswidener} opens a member Minecraft
 * keeps to itself.
 *
 * <p>Collected here so the seam is visible from both sides. Every method below names a widener
 * line in its javadoc; delete that line and this file stops compiling, which is the point. A
 * Minecraft update that renames or removes one of those members breaks the build here rather
 * than somewhere in the middle of a screen.
 *
 * <p>Nothing else belongs in this class. A method that compiles against the public API is
 * ordinary mod code and goes in the package that owns the concept.
 */
public class AccessWidenedApi {
	
	/**
	 * Builds an enchantment component from a mutable map.
	 *
	 * <p>Widens {@code ItemEnchantments.<init>(Object2IntOpenHashMap)}. The public route is
	 * {@code ItemEnchantments.Mutable}, which copies; the editor rewrites the whole set at once.
	 */
	public static ItemEnchantments newEnchantments(Object2IntOpenHashMap<Holder<Enchantment>> enchantments) {
		return new ItemEnchantments(enchantments);
	}
	
	/**
	 * Puts an item on the cursor without the server noticing a change, by overwriting its idea of
	 * what was already there. A plain {@code setCarried} desyncs, and the next click is rejected.
	 *
	 * <p>Widens {@code AbstractContainerMenu.remoteCarried}.
	 */
	public static void setCursorStackSilently(AbstractContainerMenu handler, ItemStack item) {
		handler.setCarried(item);
		handler.remoteCarried.force(item);
	}
	
	/**
	 * Replaces a text field's contents without running its change listener, and parks the cursor
	 * at one end. Used where the editor is echoing a value back into a field the player is
	 * typing in, and re-entering its own listener would fight them.
	 *
	 * <p>Widens {@code EditBox.value}.
	 */
	public static void setTextFieldValueSilently(EditBox widget, String text, boolean scrollToEnd) {
		widget.value = text;
		int cursor = (scrollToEnd ? text.length() : 0);
		widget.setCursorPosition(cursor);
		widget.setHighlightPos(cursor);
	}
	
	/**
	 * Finds the boat item that spawns a given boat entity.
	 *
	 * <p>Widens {@code BoatItem.entityType}. There is no registry from entity type back to the
	 * item that places it, so this scans the item registry.
	 */
	public static Item getBoatItem(EntityType<?> entityType) {
		for (Item item : MVRegistry.ITEM) {
			if (item instanceof BoatItem boat && entityType == getBoatEntityType(boat))
				return item;
		}
		throw new IllegalStateException("Unknown boat entity type: " + EntityType.getKey(entityType));
	}
	
	/**
	 * The boat entity a boat item places.
	 *
	 * <p>Widens {@code BoatItem.entityType}.
	 */
	public static EntityType<?> getBoatEntityType(BoatItem item) {
		return item.entityType;
	}
	
	/**
	 * The inventory slot a creative-screen slot stands in for. The creative screen wraps every
	 * slot of the screen behind it, and the editor needs the one underneath.
	 *
	 * <p>Widens {@code CreativeModeInventoryScreen$SlotWrapper} and its {@code target}.
	 */
	public static Slot getWrappedSlot(CreativeModeInventoryScreen.SlotWrapper slot) {
		return slot.target;
	}
	
	/**
	 * Adds a widget to a screen the caller does not own, so it renders and takes input with the
	 * screen's own widgets.
	 *
	 * <p>Widens {@code Screen.addRenderableWidget}, which vanilla keeps protected for subclasses.
	 */
	public static <T extends GuiEventListener & Renderable & NarratableEntry> T addWidgetToScreen(Screen screen, T widget) {
		return screen.addRenderableWidget(widget);
	}
	
	/**
	 * One of a style's five boolean formatting flags, which is {@code null} when the style says
	 * nothing about it. The public {@code isBold} and friends collapse that third state to
	 * {@code false}, and the editor has to tell "off" from "unset" to diff two styles.
	 *
	 * <p>Widens {@code Style.bold}, {@code italic}, {@code underlined}, {@code strikethrough} and
	 * {@code obfuscated}.
	 */
	public static Boolean getStyleFlag(Style style, ChatFormatting flag) {
		return switch (flag) {
			case BOLD -> style.bold;
			case ITALIC -> style.italic;
			case UNDERLINE -> style.underlined;
			case STRIKETHROUGH -> style.strikethrough;
			case OBFUSCATED -> style.obfuscated;
			default -> throw new IllegalArgumentException("Not a style flag: " + flag);
		};
	}
	
	/**
	 * The {@code &x} character a formatting code is written with.
	 *
	 * <p>Widens {@code ChatFormatting.code}.
	 */
	public static char getFormattingCode(ChatFormatting formatting) {
		return formatting.code;
	}
	
	/**
	 * A text colour as the string vanilla's own serializer would write, which is a name for the
	 * sixteen named colours and {@code #rrggbb} otherwise.
	 *
	 * <p>Widens {@code TextColor.formatValue}.
	 */
	public static String formatColorValue(TextColor color) {
		return color.formatValue();
	}
	
	/**
	 * The nbt a block argument carries, which is {@code null} when the command named no nbt.
	 *
	 * <p>Widens {@code BlockInput.tag}.
	 */
	public static CompoundTag getBlockArgumentNbt(BlockInput block) {
		return block.tag;
	}
	
	/**
	 * The container behind an open lectern screen, which is the lectern block entity's own
	 * single-slot inventory.
	 *
	 * <p>Widens {@code LecternMenu.lectern}.
	 */
	public static Container getLecternContainer(LecternMenu menu) {
		return menu.lectern;
	}
	
	/**
	 * The registry a registry entry belongs to, used to ask whether it can be serialized into a
	 * given one.
	 *
	 * <p>Widens {@code Holder$Reference.owner}.
	 */
	public static <T> HolderOwner<T> getHolderOwner(Holder.Reference<T> entry) {
		return entry.owner;
	}
	
	/**
	 * Every font the resource packs loaded, for the font picker's suggestions. The public
	 * {@code FontManager} exposes one font at a time and never the set of them.
	 *
	 * <p>Widens {@code Minecraft.fontManager} and {@code FontManager.fontSets}.
	 */
	public static Set<Identifier> getLoadedFontIds() {
		return Minecraft.getInstance().fontManager.fontSets.keySet();
	}
	
	/**
	 * The entity in a level with a given UUID, or null when nothing loaded carries it.
	 *
	 * <p>Widens {@code Level.getEntities}. The public API reaches an entity by network id or by
	 * bounding box, never by UUID; the index that answers one in a single step is behind this
	 * protected getter. The alternative is walking every loaded entity, which fancy text did on a
	 * path brigadier re-runs per keystroke, at roughly 86ns per loaded entity.
	 */
	public static Entity getEntityByUuid(Level level, UUID uuid) {
		return level.getEntities().get(uuid);
	}
	
	/**
	 * Tells a container screen to swallow the next mouse release. The editor swaps the open
	 * screen mid-click, and without this the release lands on the new screen as a fresh click.
	 *
	 * <p>Widens {@code AbstractContainerScreen.skipNextRelease}.
	 */
	public static void skipNextRelease(AbstractContainerScreen<?> screen) {
		screen.skipNextRelease = true;
	}
	
	/**
	 * Whether an NBT key can be written unquoted.
	 *
	 * <p>Widens {@code StringTagVisitor.UNQUOTED_KEY_MATCH}, which is the pattern the vanilla
	 * SNBT writer itself consults. {@code true} and {@code false} match the pattern but would
	 * read back as booleans, so they are excluded.
	 */
	public static boolean isSimpleName(String name) {
		return (!name.equalsIgnoreCase("true") && !name.equalsIgnoreCase("false") &&
				StringTagVisitor.UNQUOTED_KEY_MATCH.matcher(name).matches());
	}
	
}
