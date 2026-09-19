package com.luneruniverse.minecraft.mod.nbteditor.dev;

import java.util.UUID;

import com.luneruniverse.minecraft.mod.nbteditor.NBTEditor;
import com.luneruniverse.minecraft.mod.nbteditor.fancytext.FancyText;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.decoration.ArmorStand;

/**
 * Times resolving a {@code [show_entity]} UUID against the number of entities the client has
 * loaded, which is what roadmap 2.1 asks for before anything changes.
 *
 * <p>The cost is the point: the resolve runs on the fancy-text parse path, which brigadier
 * re-runs per keystroke, and it is proportional to loaded entities, so a flat dev world says
 * nothing about a busy server. This loads the entities itself, client side, rather than waiting
 * for one.
 *
 * <p>Off unless {@code -Dnbte.devbench} is set. The value is the entity counts to walk, comma
 * separated; the default spans a dev world and a crowded one.
 */
public class DevEntityScanBench {

	private static final String DEFAULT_COUNTS = "0,100,500,2000,8000";
	private static final int WARMUP = 200;
	private static final int RUNS = 2000;
	/** A software-rendered client is a noisy place to time anything, and the minimum of a few
	 * repetitions is far steadier than any one of them. */
	private static final int REPEATS = 3;
	/**
	 * How much slower the busiest world is allowed to be than the fastest run of the set.
	 * Resolving a UUID
	 * through the level's own index does not depend on the entity count at all, so the honest
	 * bound is 1 plus noise. The scan this replaced was 24 times slower across the same span, so
	 * anything that reintroduces one lands well outside this.
	 */
	private static final double MAX_SLOWDOWN = 4;

	static void install() {
		String counts = System.getProperty("nbte.devbench");
		if (counts == null)
			return;
		DevEntityScanBench bench = new DevEntityScanBench(counts.isEmpty() ? DEFAULT_COUNTS : counts);
		ClientTickEvents.END_CLIENT_TICK.register(bench::tick);
	}

	private final int[] counts;
	private final double[] results;
	private int next;
	private int loaded;
	private int settle = 40;

	private DevEntityScanBench(String counts) {
		String[] parts = counts.split(",");
		this.counts = new int[parts.length];
		this.results = new double[parts.length];
		for (int i = 0; i < parts.length; i++)
			this.counts[i] = Integer.parseInt(parts[i].trim());
	}

	private void tick(Minecraft client) {
		if (client.player == null || client.level == null || next > counts.length)
			return;
		if (settle-- > 0)
			return;
		if (next == counts.length) {
			report();
			next++;
			return;
		}
		int target = counts[next];
		try {
			while (loaded < target)
				spawn(client, ++loaded);
			results[next++] = measure(client, loaded);
		} catch (Throwable e) {
			NBTEditor.LOGGER.error("BENCH fail at " + target + " entities", e);
			NBTEditor.LOGGER.info("BENCH done");
			next = counts.length + 1;
		}
	}

	/** A client-side armour stand, which is what the renderer's entity list is made of. */
	private static void spawn(Minecraft client, int id) {
		Entity entity = new ArmorStand(EntityTypes.ARMOR_STAND, client.level);
		entity.setId(1_000_000 + id);
		entity.setUUID(UUID.randomUUID());
		entity.snapTo(client.player.getX(), client.player.getY(), client.player.getZ());
		client.level.addEntity(entity);
	}

	/**
	 * A miss is the honest number: a hit stops at whatever position the entity happens to hold,
	 * and the fallback path a wrong UUID takes walks every entity there is.
	 */
	/**
	 * The resolve is supposed to cost the same whatever the world holds. Saying so out loud is
	 * what turns the baseline into a check: an O(entities) scan cannot pass this.
	 */
	private void report() {
		// Against the fastest run rather than the first: the first pays for class loading and is
		// routinely the slowest of the set, which says nothing about how the resolve scales.
		double fastest = Double.MAX_VALUE;
		for (double result : results)
			fastest = Math.min(fastest, result);
		double slowdown = results[results.length - 1] / fastest;
		if (slowdown > MAX_SLOWDOWN)
			NBTEditor.LOGGER.error("BENCH fail the resolve is {}x slower on a busy world, so it scans",
					String.format("%.1f", slowdown));
		else
			NBTEditor.LOGGER.info("BENCH flat within {}x across {} to {} entities",
					String.format("%.1f", slowdown), counts[0], counts[counts.length - 1]);
		NBTEditor.LOGGER.info("BENCH done");
	}
	
	private static double measure(Minecraft client, int entities) {
		String text = "[show_entity]{" + UUID.randomUUID() + "}(hover)";
		if (!hasHoverEvent(FancyText.parse(text))) {
			// A syntax slip here would time a parse that never reaches the resolve at all, and
			// report it as the scan being free.
			throw new IllegalStateException("the bench text parses without a hover event: " + text);
		}
		for (int i = 0; i < WARMUP; i++)
			FancyText.parse(text);
		long each = Long.MAX_VALUE;
		for (int repeat = 0; repeat < REPEATS; repeat++) {
			long start = System.nanoTime();
			for (int i = 0; i < RUNS; i++)
				FancyText.parse(text);
			each = Math.min(each, (System.nanoTime() - start) / RUNS);
		}
		NBTEditor.LOGGER.info("BENCH entities={} rendering={} parse={}us",
				entities, count(client), each / 1000.0);
		return each / 1000.0;
	}

	private static boolean hasHoverEvent(Component text) {
		if (text.getStyle().getHoverEvent() != null)
			return true;
		for (Component sibling : text.getSiblings()) {
			if (hasHoverEvent(sibling))
				return true;
		}
		return false;
	}
	
	private static int count(Minecraft client) {
		int total = 0;
		for (@SuppressWarnings("unused") Entity entity : client.level.entitiesForRendering())
			total++;
		return total;
	}

}
