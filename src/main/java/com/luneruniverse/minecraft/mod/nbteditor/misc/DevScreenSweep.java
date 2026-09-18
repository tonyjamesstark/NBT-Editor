package com.luneruniverse.minecraft.mod.nbteditor.misc;

import java.util.ArrayList;
import java.util.List;

import com.luneruniverse.minecraft.mod.nbteditor.NBTEditor;
import com.luneruniverse.minecraft.mod.nbteditor.containers.ContainerIO;
import com.luneruniverse.minecraft.mod.nbteditor.containers.ContainerIOs;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.DynamicRegistryManagerHolder;
import com.luneruniverse.minecraft.mod.nbteditor.nbtreferences.itemreferences.HandItemReference;
import com.luneruniverse.minecraft.mod.nbteditor.nbtreferences.itemreferences.ItemReference;
import com.luneruniverse.minecraft.mod.nbteditor.screens.configurable.Configurable;
import com.luneruniverse.minecraft.mod.nbteditor.screens.factories.LocalFactoryScreen;
import com.luneruniverse.minecraft.mod.nbteditor.screens.factories.LocalFactoryScreen.LocalFactoryReference;
import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.ItemTagReferences;
import com.luneruniverse.minecraft.mod.nbteditor.util.NbtIO;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;

/**
 * Clicks every button on the factory menu in turn, so a menu that only breaks once it is on
 * screen breaks on the build host instead of in someone's game. The dev client harness cannot
 * click, and nothing else in the build opens an editor screen at all.
 *
 * <p>It also checks, once, the entity id each minecart's container io writes, because that value
 * is fixed at registration and a wrong one compiles, remaps and sweeps clean. Only the NBT of an
 * edited item shows it, which is how hopper minecarts shipped carrying a furnace_minecart id.
 *
 * <p>Off unless {@code -Dnbte.devscreens} is set. The value is the lore to give the item,
 * {@code |} separating lines; the default carries the non-ASCII text a report on 2026-09-16
 * pointed at. {@code -Dnbte.devscreens.nbt} replaces the whole component patch instead, in the
 * SNBT the NBT editor itself shows, which is how a reported item gets reproduced here verbatim.
 * {@code -Dnbte.devscreens.from=N} resumes at row N, for when a menu takes the client down with
 * it and the rest of the sweep still needs running.
 */
public class DevScreenSweep {

	private static final String DEFAULT_LORE = String.join("|",
			"\u00a7bSection \u00a7dcodes",
			"Latin-1 \u00dcn\u00efc\u00f6d\u00e9, CJK \u6d4b\u8bd5, symbols \u2726\u2714",
			"Astral \ud83d\udde1\ufe0f\ud83c\udf89 and combining a\u0301",
			"Quote \" backslash \\ brace } bracket ] colon :",
			"Literal escape \\u00a7 and \\n",
			"RTL \u05e9\u05dc\u05d5\u05dd zero-width \u200b nbsp \u00a0");
	private static final int TICKS_PER_ROW = 20;

	/** Where LocalFactoryScreen puts its ConfigPanel, and what ConfigGroupingVertical then adds. */
	private static final int PANEL_X = 16;
	private static final int PANEL_Y = 64;
	private static final int ROW_HEIGHT = 20;

	public static void install() {
		String lore = System.getProperty("nbte.devscreens");
		if (lore == null)
			return;
		DevScreenSweep sweep = new DevScreenSweep(lore.isEmpty() ? DEFAULT_LORE : lore,
				System.getProperty("nbte.devscreens.nbt"), Integer.getInteger("nbte.devscreens.from", 0));
		ClientTickEvents.END_CLIENT_TICK.register(sweep::tick);
	}

	private final String lore;
	private final List<String> items;
	private int item;
	private int next;
	private int ticks;
	private boolean done;
	private List<LocalFactoryReference> rows;

	private DevScreenSweep(String lore, String nbt, int from) {
		this.lore = lore;
		this.items = nbt == null ? null : List.of(nbt.split(";;"));
		this.next = from;
	}

	private void tick(Minecraft client) {
		if (client.player == null || done)
			return;
		if (rows != null && next >= rows.size()) {
			if (items == null || ++item >= items.size()) {
				done = true;
				NBTEditor.LOGGER.info("SWEEP done");
				return;
			}
			rows = null;
			next = 0;
		}
		if (ticks++ % TICKS_PER_ROW != 0)
			return;

		if (rows == null) {
			// Without this the mod reads the default registry set, and every holder the server
			// owns is foreign to it. It is why the factory menu went dead on enchanted items.
			if (DynamicRegistryManagerHolder.hasClientManager())
				NBTEditor.LOGGER.info("SWEEP registry client manager set");
			else
				NBTEditor.LOGGER.error("SWEEP fail the client registry manager was never set");
			if (item == 0)
				checkEntityIds();
			client.player.setItemInHand(InteractionHand.MAIN_HAND, buildItem(client));
			ItemReference ref = new HandItemReference(InteractionHand.MAIN_HAND);
			rows = new ArrayList<>();
			for (LocalFactoryReference factory : LocalFactoryScreen.BASIC_FACTORIES) {
				if (factory.supported().test(ref))
					rows.add(factory);
			}
			NBTEditor.LOGGER.info("SWEEP item {} of {} nbt={}", item,
					items == null ? 1 : items.size(), client.player.getMainHandItem().nbte$getNbt());
			return;
		}

		int row = next++;
		String name = "item " + item + " row " + row + " " + rows.get(row).buttonText().getString();
		LocalFactoryScreen<?> menu = new LocalFactoryScreen<>(new HandItemReference(InteractionHand.MAIN_HAND));
		client.setScreenAndShow(menu);
		NBTEditor.LOGGER.info("SWEEP click {}", name);
		try {
			boolean handled = menu.mouseClicked(new MouseButtonEvent(
					PANEL_X + Configurable.PADDING * 2 + 5,
					PANEL_Y + row * (ROW_HEIGHT + Configurable.PADDING) + 10,
					new MouseButtonInfo(0, 0)), false);
			NBTEditor.LOGGER.info("SWEEP {} handled={} opened={}",
					name, handled, client.gui.screen().getClass().getSimpleName());
			if (!handled || client.gui.screen() == menu)
				NBTEditor.LOGGER.error("SWEEP fail {} the click opened nothing", name);
		} catch (Throwable e) {
			NBTEditor.LOGGER.error("SWEEP fail " + name, e);
		}
		client.setScreenAndShow(null);
	}

	private static void checkEntityIds() {
		checkEntityId(Items.HOPPER_MINECART, "minecraft:hopper_minecart");
		checkEntityId(Items.CHEST_MINECART, "minecraft:chest_minecart");
	}

	/** Edits the item the way the container screen does, then reads back the id that reached NBT. */
	private static void checkEntityId(Item container, String expected) {
		ItemStack stack = new ItemStack(container);
		try {
			ContainerIO<ItemStack> io = ContainerIOs.get(stack);
			ItemStack[] contents = new ItemStack[io.getMaxSlots(stack)];
			contents[0] = new ItemStack(Items.DIAMOND);
			io.write(stack, contents);
			CompoundTag entityData = ItemTagReferences.ENTITY_DATA.get(stack);
			String id = entityData == null ? null : entityData.nbte$getStringOrDefault("id");
			if (expected.equals(id))
				NBTEditor.LOGGER.info("SWEEP check {} writes id={}", expected, id);
			else
				NBTEditor.LOGGER.error("SWEEP fail {} writes id={}", expected, id);
		} catch (Throwable e) {
			NBTEditor.LOGGER.error("SWEEP fail " + expected + " could not be edited", e);
		}
	}

	private ItemStack buildItem(Minecraft client) {
		ItemStack stack = new ItemStack(Items.DIAMOND_SWORD);
		if (items != null) {
			String snbt = items.get(item);
			try {
				stack.nbte$setNbt((CompoundTag) NbtIO.parseSnbt(snbt));
				return stack;
			} catch (Exception e) {
				throw new IllegalArgumentException("-Dnbte.devscreens.nbt is not SNBT: " + snbt, e);
			}
		}
		stack.enchant(client.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
				.getOrThrow(Enchantments.UNBREAKING), 3);
		List<Component> lines = new ArrayList<>();
		for (String line : lore.split("\\|", -1))
			lines.add(Component.literal(line));
		ItemTagReferences.LORE.set(stack, lines);
		return stack;
	}

}
