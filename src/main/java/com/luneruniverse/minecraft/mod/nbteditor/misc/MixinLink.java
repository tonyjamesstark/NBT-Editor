package com.luneruniverse.minecraft.mod.nbteditor.misc;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.WeakHashMap;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.luneruniverse.minecraft.mod.nbteditor.mixin.ChatScreenAccessor;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVDrawableHelper;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVTextEvents;
import com.luneruniverse.minecraft.mod.nbteditor.screens.ConfigScreen;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import com.luneruniverse.minecraft.mod.nbteditor.util.NbtIO;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Style;
import net.minecraft.client.Minecraft;

/**
 * The seam between the mixins and the rest of the mod. Non-mixin classes in the mixin package do
 * not work well, so anything a mixin needs to share lives here.
 *
 * <p>What belongs here is state that couples one mixin to another: a flag a mixin sets so a
 * second mixin further down the call stack behaves differently, keyed by thread because that is
 * the only handle the two ends share. {@link #specialNumbers}, {@link #hiddenExceptionHandlers}
 * and {@link #SET_CHANGES} are all that shape, and they have to be reachable from both ends.
 *
 * <p>What does not belong here is mod behaviour that merely happens to be invoked from a mixin.
 * That goes in the package that owns the concept, and the mixin calls it there. Tooltips live in
 * {@code screens.ItemTooltips}, container screen input in
 * {@code screens.containers.ContainerScreenInput}.
 */
public class MixinLink {
	
	public static boolean CLIENT_LOADED = false;
	
	
	private static final Map<String, Runnable> events = new HashMap<>();
	public static Style withRunClickEvent(Style style, Runnable onClick) {
		String id = "\0nbteditor_runnable@" + new Random().nextLong(); // \0 is not valid in file paths on most OSs
		events.put(id, onClick);
		return style.withClickEvent(MVTextEvents.ClickAction.OPEN_FILE.newEvent(id));
	}
	public static boolean tryRunClickEvent(String id) {
		Runnable onClick = events.get(id);
		if (onClick != null) {
			onClick.run();
			return true;
		}
		return false;
	}
	
	
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
	
	
	public static void renderChatLimitWarning(ChatScreen source, GuiGraphicsExtractor context) {
		if (!ConfigScreen.isChatLimitExtended())
			return;
		
		EditBox chatField = ((ChatScreenAccessor) source).getInput();
		if (chatField.getValue().length() > 256) {
			MVDrawableHelper.fill(context, source.width - 202, source.height - 40, source.width - 2, source.height - 14, 0xAAFFAA00);
			Font textRenderer = Minecraft.getInstance().font;
			MVDrawableHelper.drawCenteredTextWithShadow(context, textRenderer, Component.translatableEscape("nbteditor.chat_length_warning_1"), source.width - 102, source.height - 40 + textRenderer.lineHeight / 2, 0xFFAA5500);
			MVDrawableHelper.drawCenteredTextWithShadow(context, textRenderer, Component.translatableEscape("nbteditor.chat_length_warning_2"), source.width - 102, source.height - 28 + textRenderer.lineHeight / 2, 0xFFAA5500);
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
	
	
	/**
	 * Only in 1.20.5 or higher
	 */
	public static final Cache<BookViewScreen.BookAccess, Boolean> WRITTEN_BOOK_CONTENTS = CacheBuilder.newBuilder().weakKeys().build();
	
	
	public static final WeakHashMap<Runnable, Boolean> CATCH_BYPASSING_TASKS = new WeakHashMap<>();
	public static void executeCrashableTask(Runnable task) {
		CATCH_BYPASSING_TASKS.put(task, true);
		Minecraft.getInstance().execute(task);
	}
	
	
	public static final WeakHashMap<Tooltip, Boolean> NEW_TOOLTIPS = new WeakHashMap<>();
	
	
	// MinecraftClient#thread is set after the ClientModInitializers are run
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
