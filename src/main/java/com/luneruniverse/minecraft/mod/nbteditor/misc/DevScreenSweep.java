package com.luneruniverse.minecraft.mod.nbteditor.misc;

import java.util.ArrayList;
import java.util.List;

import com.luneruniverse.minecraft.mod.nbteditor.NBTEditor;
import com.luneruniverse.minecraft.mod.nbteditor.nbtreferences.itemreferences.HandItemReference;
import com.luneruniverse.minecraft.mod.nbteditor.nbtreferences.itemreferences.ItemReference;
import com.luneruniverse.minecraft.mod.nbteditor.screens.factories.LocalFactoryScreen;
import com.luneruniverse.minecraft.mod.nbteditor.screens.factories.LocalFactoryScreen.LocalFactoryReference;
import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.ItemTagReferences;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Opens every factory screen in turn against a synthetic item, so a screen that only breaks once
 * it is on-screen breaks on the build host instead of in someone's game. The dev client harness
 * cannot click, and nothing else in the build opens a screen at all.
 *
 * Off unless {@code -Dnbte.devscreens} is set. The value is the lore to give the item, {@code |}
 * separating lines; the default carries the non-ASCII text a report on 2026-09-16 pointed at.
 * {@code -Dnbte.devscreens.from=N} resumes at factory N, for when a screen takes the client down
 * with it and the rest of the sweep still needs running.
 */
public class DevScreenSweep {

	private static final String DEFAULT_LORE =
			"§bPre-existing §dunicode: Ünïcödé ✦ 测试|Second line ✔";
	private static final int TICKS_PER_SCREEN = 20;

	public static void install() {
		String lore = System.getProperty("nbte.devscreens");
		if (lore == null)
			return;
		int from = Integer.getInteger("nbte.devscreens.from", 0);
		DevScreenSweep sweep = new DevScreenSweep(lore.isEmpty() ? DEFAULT_LORE : lore, from);
		ClientTickEvents.END_CLIENT_TICK.register(sweep::tick);
	}

	private final String lore;
	private int next;
	private int ticks;
	private boolean done;
	private List<LocalFactoryReference> factories;

	private DevScreenSweep(String lore, int from) {
		this.lore = lore;
		this.next = from;
	}

	private void tick(Minecraft client) {
		if (client.player == null || done)
			return;
		if (factories != null && next >= factories.size()) {
			done = true;
			NBTEditor.LOGGER.info("SWEEP done");
			return;
		}
		if (ticks++ % TICKS_PER_SCREEN != 0)
			return;

		if (factories == null) {
			ItemStack item = new ItemStack(Items.DIAMOND_SWORD);
			List<Component> lines = new ArrayList<>();
			for (String line : lore.split("\\|", -1))
				lines.add(Component.literal(line));
			ItemTagReferences.LORE.set(item, lines);
			client.player.setItemInHand(InteractionHand.MAIN_HAND, item);
			factories = LocalFactoryScreen.BASIC_FACTORIES;
			NBTEditor.LOGGER.info("SWEEP item {} lore={}", item, ItemTagReferences.LORE.get(item));
			return;
		}

		ItemReference ref = new HandItemReference(InteractionHand.MAIN_HAND);
		LocalFactoryReference factory = factories.get(next);
		String name = next + " " + factory.buttonText().getString();
		next++;
		if (!factory.supported().test(ref)) {
			NBTEditor.LOGGER.info("SWEEP skip {}", name);
			return;
		}
		NBTEditor.LOGGER.info("SWEEP open {}", name);
		try {
			factory.factory().accept(ref);
			NBTEditor.LOGGER.info("SWEEP ok {} children={}", name, client.gui.screen().children().size());
		} catch (Throwable e) {
			NBTEditor.LOGGER.error("SWEEP fail " + name, e);
			client.setScreenAndShow(null);
		}
	}

}
