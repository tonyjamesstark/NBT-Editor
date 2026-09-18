package com.luneruniverse.minecraft.mod.nbteditor.misc;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import com.luneruniverse.minecraft.mod.nbteditor.util.NbtIO;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.Tag;
import net.minecraft.client.Minecraft;

/**
 * The seam between the mixins and the rest of the mod. Non-mixin classes in the mixin package do
 * not work well, so anything a mixin needs to share lives here.
 *
 * <p>What belongs here is state that couples one mixin to another: a flag a mixin sets so a
 * second mixin further down the call stack behaves differently, keyed by thread because that is
 * the only handle the two ends share. {@link #specialNumbers}, {@link #hiddenExceptionHandlers},
 * {@link #ITEM_BEING_RENDERED} and {@link #SET_CHANGES} are all that shape, and they have to be
 * reachable from both ends.
 *
 * <p>What does not belong here is mod behaviour that merely happens to be invoked from a mixin.
 * That goes in the package that owns the concept, and the mixin calls it there. Tooltips live in
 * {@code screens.ItemTooltips}, container screen input in
 * {@code screens.containers.ContainerScreenInput}, the chat length banner in
 * {@code screens.ChatLimitWarning}, and clickable text the mod builds itself in
 * {@code util.TextEvents}.
 */
public class MixinLink {
	
	public static boolean CLIENT_LOADED = false;
	
	
	public static File screenshotTarget;
	
	
	public static final Set<Thread> hiddenExceptionHandlers = Collections.synchronizedSet(new HashSet<>());
	@SuppressWarnings("serial")
	public static class HiddenException extends RuntimeException {
		public HiddenException(String msg, Throwable cause) {
			super(msg, cause);
		}
	}
	public interface DangerousRunnable {
		public void run() throws Throwable;
	}
	public static void throwHiddenException(DangerousRunnable toRun) throws Throwable {
		hiddenExceptionHandlers.add(Thread.currentThread());
		try {
			toRun.run();
		} catch (HiddenException e) {
			throw e.getCause();
		} finally {
			hiddenExceptionHandlers.remove(Thread.currentThread());
		}
	}
	
	
	public static final Set<Thread> specialNumbers = Collections.synchronizedSet(new HashSet<>());
	public static Tag parseSpecialElement(StringReader reader) throws CommandSyntaxException {
		specialNumbers.add(Thread.currentThread());
		try {
			return NbtIO.parseSnbt(reader);
		} finally {
			specialNumbers.remove(Thread.currentThread());
		}
	}
	
	
	public static final Cache<BookViewScreen.BookAccess, Boolean> WRITTEN_BOOK_CONTENTS = CacheBuilder.newBuilder().weakKeys().build();
	
	
	public static final WeakHashMap<Runnable, Boolean> CATCH_BYPASSING_TASKS = new WeakHashMap<>();
	public static void executeCrashableTask(Runnable task) {
		CATCH_BYPASSING_TASKS.put(task, true);
		Minecraft.getInstance().execute(task);
	}
	
	
	public static final WeakHashMap<Tooltip, Boolean> NEW_TOOLTIPS = new WeakHashMap<>();
	
	
	/**
	 * The client's main thread, recorded early enough to be usable during mod init.
	 *
	 * <p>{@code Minecraft.isSameThread()} is not a substitute. {@code Minecraft.gameThread} is
	 * assigned partway through {@code Minecraft.<init>}, after Fabric has dispatched the mod
	 * initialisers, so {@code isSameThread()} returns false on the real main thread for the whole
	 * of {@code onInitialize} and {@code onInitializeClient}. Measured on 26.2 by printing both
	 * from every entrypoint in a dev client: the mixin sets this immediately after
	 * {@code ReentrantBlockableEventLoop.<init>}, where {@code Minecraft.getInstance()} is still
	 * null, and {@code isSameThread()} first reads true at registry load. Swapping it out would
	 * silently disable the guard in {@link
	 * com.luneruniverse.minecraft.mod.nbteditor.multiversion.DynamicRegistryManagerHolder}, which
	 * is the one caller and runs during init.
	 */
	public static volatile Thread MAIN_THREAD;
	public static boolean isOnMainThread() {
		return Thread.currentThread() == MAIN_THREAD;
	}
	
	
	public static final Map<Thread, ItemStack> ITEM_BEING_RENDERED = Collections.synchronizedMap(new WeakHashMap<>());
	
	
	public static final Set<Thread> SET_CHANGES = Collections.synchronizedSet(new HashSet<>());
	public static void setChanges(ItemStack item, DataComponentPatch changes) {
		try {
			SET_CHANGES.add(Thread.currentThread());
			item.applyComponentsAndValidate(changes);
		} finally {
			SET_CHANGES.remove(Thread.currentThread());
		}
	}
	
}
