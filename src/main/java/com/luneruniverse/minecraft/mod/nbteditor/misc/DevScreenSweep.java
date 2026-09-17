package com.luneruniverse.minecraft.mod.nbteditor.misc;

import java.util.ArrayList;
import java.util.List;

import com.luneruniverse.minecraft.mod.nbteditor.NBTEditor;
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
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Clicks every button on the factory menu in turn, so a menu that only breaks once it is on
 * screen breaks on the build host instead of in someone's game. The dev client harness cannot
 * click, and nothing else in the build opens an editor screen at all.
 *
 * <p>Off unless {@code -Dnbte.devscreens} is set. The value is the lore to give the item,
 * {@code |} separating lines; the default carries the non-ASCII text a report on 2026-09-16
 * pointed at. {@code -Dnbte.devscreens.nbt} replaces the whole component patch instead, in the
 * SNBT the NBT editor itself shows, which is how a reported item gets reproduced here verbatim.
 * {@code -Dnbte.devscreens.from=N} resumes at row N, for when a menu takes the client down with
 * it and the rest of the sweep still needs running.
 */
public class DevScreenSweep {

	private static final String DEFAULT_LORE =
			"§bPre-existing §dunicode: Ünïcödé ✦ 测试|Second line ✔";
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
	private final String nbt;
	private int next;
	private int ticks;
	private boolean done;
	private List<LocalFactoryReference> rows;

	private DevScreenSweep(String lore, String nbt, int from) {
		this.lore = lore;
		this.nbt = nbt;
		this.next = from;
	}

	private void tick(Minecraft client) {
		if (client.player == null || done)
			return;
		if (rows != null && next >= rows.size()) {
			done = true;
			NBTEditor.LOGGER.info("SWEEP done");
			return;
		}
		if (ticks++ % TICKS_PER_ROW != 0)
			return;

		if (rows == null) {
			client.player.setItemInHand(InteractionHand.MAIN_HAND, buildItem());
			ItemReference ref = new HandItemReference(InteractionHand.MAIN_HAND);
			rows = new ArrayList<>();
			for (LocalFactoryReference factory : LocalFactoryScreen.BASIC_FACTORIES) {
				if (factory.supported().test(ref))
					rows.add(factory);
			}
			NBTEditor.LOGGER.info("SWEEP item {} nbt={}", client.player.getMainHandItem(),
					client.player.getMainHandItem().nbte$getNbt());
			return;
		}

		int row = next++;
		String name = row + " " + rows.get(row).buttonText().getString();
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

	private ItemStack buildItem() {
		ItemStack item = new ItemStack(Items.DIAMOND_SWORD);
		if (nbt != null) {
			try {
				item.nbte$setNbt((net.minecraft.nbt.CompoundTag) NbtIO.parseSnbt(nbt));
				return item;
			} catch (Exception e) {
				throw new IllegalArgumentException("-Dnbte.devscreens.nbt is not SNBT: " + nbt, e);
			}
		}
		List<Component> lines = new ArrayList<>();
		for (String line : lore.split("\\|", -1))
			lines.add(Component.literal(line));
		ItemTagReferences.LORE.set(item, lines);
		return item;
	}

}
