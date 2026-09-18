package com.luneruniverse.minecraft.mod.nbteditor.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.WeakHashMap;
import java.util.function.Function;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.DynamicOps;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.DynamicRegistryManagerHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStackTemplate;

/**
 * Click and hover events as a descriptor per action: its fancy-text name, how a typed value is
 * parsed out of a string, and how one is read from and written back to a vanilla event.
 *
 * <p>This was <code>multiversion/MVTextEvents</code>. Nothing about it spans game versions -- the
 * package was the only version-flavoured thing left, and ADR-0001 sends such a class to
 * <code>util/</code> rather than keeping it in a layer being dismantled. It sits beside
 * {@link TextUtil}, which is where the rest of the text serialization already lives.
 */
public class TextEvents {
	
	/**
	 * Handlers for the mod's own clickable text, carried as an {@link ClickAction#OPEN_FILE}
	 * event because a path is the only click value that is free-form enough to hide an id in.
	 * {@code mixin.ScreenMixin} sees the click first and calls {@link #tryRunClickEvent}.
	 *
	 * <p>The keys are weak and the id string inside the event is the only strong reference to
	 * one, so a handler lives exactly as long as the text that can still invoke it. A strong map
	 * here pinned every screen a handler had captured for the rest of the session.
	 */
	private static final Map<String, Runnable> runClickEvents = Collections.synchronizedMap(new WeakHashMap<>());
	
	public static Style withRunClickEvent(Style style, Runnable onClick) {
		String id = "\0nbteditor_runnable@" + new Random().nextLong(); // \0 is not valid in file paths on most OSs
		runClickEvents.put(id, onClick);
		return style.withClickEvent(ClickAction.OPEN_FILE.newEvent(id));
	}
	public static boolean tryRunClickEvent(String id) {
		Runnable onClick = runClickEvents.get(id);
		if (onClick == null)
			return false;
		onClick.run();
		return true;
	}
	
	public static class ClickAction<T> {
		private static final Function<String, Optional<URI>> parseUri = valueStr -> {
			try {
				// Anything but http and https is refused here rather than at the codec, which
				// would drop the whole click event when the edit was saved.
				return Optional.of(Util.parseAndValidateUntrustedUri(valueStr));
			} catch (URISyntaxException e) {
				return Optional.empty();
			}
		};
		private static final Function<String, Optional<String>> parseStr = Optional::of;
		private static final Function<String, Optional<String>> parseCmd = valueStr -> {
			return valueStr.chars().allMatch(c -> TextUtil.isValidChar((char) c)) ? Optional.of(valueStr) : Optional.empty();
		};
		private static final Function<String, Optional<Integer>> parsePage = valueStr -> {
			try {
				int page = Integer.parseInt(valueStr);
				if (page >= 1)
					return Optional.of(page);
			} catch (NumberFormatException e) {}
			return Optional.empty();
		};
		
		public static final ClickAction<URI> OPEN_URL = new ClickAction<>("open_url", parseUri, ClickEvent.OpenUrl::uri, ClickEvent.OpenUrl::new);
		public static final ClickAction<String> OPEN_FILE = new ClickAction<>("open_file", parseStr, ClickEvent.OpenFile::path, ClickEvent.OpenFile::new);
		public static final ClickAction<String> RUN_COMMAND = new ClickAction<>("run_command", parseCmd, ClickEvent.RunCommand::command, ClickEvent.RunCommand::new);
		public static final ClickAction<String> SUGGEST_COMMAND = new ClickAction<>("suggest_command", parseCmd, ClickEvent.SuggestCommand::command, ClickEvent.SuggestCommand::new);
		public static final ClickAction<Integer> CHANGE_PAGE = new ClickAction<>("change_page", parsePage, ClickEvent.ChangePage::page, ClickEvent.ChangePage::new);
		public static final ClickAction<String> COPY_TO_CLIPBOARD = new ClickAction<>("copy_to_clipboard", parseStr, ClickEvent.CopyToClipboard::value, ClickEvent.CopyToClipboard::new);
		public static final ClickAction<?>[] VALUES = new ClickAction<?>[] {OPEN_URL, OPEN_FILE, RUN_COMMAND, SUGGEST_COMMAND, CHANGE_PAGE, COPY_TO_CLIPBOARD};
		
		public static ClickAction<?> fromName(String name) {
			for (ClickAction<?> action : VALUES) {
				if (action.getName().equals(name))
					return action;
			}
			throw new IllegalArgumentException("Invalid ClickAction name: " + name);
		}
		
		/** Null for an action the fancy-text format cannot express (SHOW_DIALOG, CUSTOM). */
		public static ClickAction<?> getAction(ClickEvent event) {
			return switch (event.action()) {
				case OPEN_URL -> OPEN_URL;
				case OPEN_FILE -> OPEN_FILE;
				case RUN_COMMAND -> RUN_COMMAND;
				case SUGGEST_COMMAND -> SUGGEST_COMMAND;
				case CHANGE_PAGE -> CHANGE_PAGE;
				case COPY_TO_CLIPBOARD -> COPY_TO_CLIPBOARD;
				default -> null;
			};
		}
		
		private final String name;
		private final Function<String, Optional<T>> parser;
		private final Function<ClickEvent, T> getter;
		private final Function<T, ClickEvent> constructor;
		
		@SuppressWarnings("unchecked")
		private <E extends ClickEvent> ClickAction(String name, Function<String, Optional<T>> parser, Function<E, T> getter, Function<T, ClickEvent> constructor) {
			this.name = name;
			this.parser = parser;
			this.getter = (Function<ClickEvent, T>) getter;
			this.constructor = constructor;
		}
		
		public String getName() {
			return name;
		}
		
		public Optional<T> parseValue(String valueStr) {
			return parser.apply(valueStr);
		}
		
		public String getStringifiedValue(ClickEvent event) {
			return getter.apply(event).toString();
		}
		public Optional<T> getValue(ClickEvent event) {
			return parseValue(getStringifiedValue(event));
		}
		
		public ClickEvent newEvent(T value) {
			return constructor.apply(value);
		}
		public Optional<ClickEvent> newEventParse(String valueStr) {
			return parseValue(valueStr).map(this::newEvent);
		}
	}
	
	public static class HoverAction<T> {
		public static final HoverAction<Component> SHOW_TEXT = new HoverAction<>("show_text", HoverEvent.ShowText::value, HoverEvent.ShowText::new);
		public static final HoverAction<ItemStackTemplate> SHOW_ITEM = new HoverAction<>("show_item", HoverEvent.ShowItem::item, HoverEvent.ShowItem::new);
		public static final HoverAction<HoverEvent.EntityTooltipInfo> SHOW_ENTITY = new HoverAction<>("show_entity", HoverEvent.ShowEntity::entity, HoverEvent.ShowEntity::new);
		public static final HoverAction<?>[] VALUES = new HoverAction<?>[] {SHOW_TEXT, SHOW_ITEM, SHOW_ENTITY};
		
		public static HoverAction<?> fromName(String name) {
			for (HoverAction<?> action : VALUES) {
				if (action.getName().equals(name))
					return action;
			}
			throw new IllegalArgumentException("Invalid HoverAction name: " + name);
		}
		
		/**
		 * A show_item hover holds an item, an item holds components, and an enchantment component
		 * holds registry entries, which a codec refuses to write through ops that cannot reach the
		 * registry that owns them.
		 */
		private static DynamicOps<Tag> nbtOps() {
			return DynamicRegistryManagerHolder.get().createSerializationContext(NbtOps.INSTANCE);
		}
		
		public static HoverAction<?> getAction(HoverEvent event) {
			return switch (event.action()) {
				case SHOW_TEXT -> SHOW_TEXT;
				case SHOW_ITEM -> SHOW_ITEM;
				case SHOW_ENTITY -> SHOW_ENTITY;
			};
		}
		
		private final String name;
		private final Function<HoverEvent, T> getter;
		private final Function<T, HoverEvent> constructor;
		
		@SuppressWarnings("unchecked")
		private <E extends HoverEvent> HoverAction(String name, Function<E, T> getter, Function<T, HoverEvent> constructor) {
			this.name = name;
			this.getter = (Function<HoverEvent, T>) getter;
			this.constructor = constructor;
		}
		
		public String getName() {
			return name;
		}
		
		public Optional<T> parseValue(String valueStr) {
			return newEventParse(valueStr).map(this::getValue);
		}
		
		@SuppressWarnings("unchecked")
		public T getValue(HoverEvent event) {
			return getter.apply(event);
		}
		public String getStringifiedValue(HoverEvent event) {
			CompoundTag nbt = (CompoundTag) HoverEvent.CODEC.encodeStart(nbtOps(), event).result().orElseThrow();
			if (this == SHOW_TEXT)
				return nbt.get("value").toString();
			nbt.remove("action");
			return nbt.toString();
		}
		
		public HoverEvent newEvent(T value) {
			return constructor.apply(value);
		}
		public Optional<HoverEvent> newEventParse(String valueStr) {
			Tag valueNbt;
			try {
				valueNbt = TagParser.create(NbtOps.INSTANCE).parseFully(valueStr);
			} catch (CommandSyntaxException e) {
				return Optional.empty();
			}

			CompoundTag nbt = new CompoundTag();
			nbt.putString("action", name);
			if (this == SHOW_TEXT)
				nbt.put("value", valueNbt);
			else if (valueNbt instanceof CompoundTag valueNbtCompound)
				nbt.merge(valueNbtCompound);
			else
				return Optional.empty();

			return HoverEvent.CODEC.parse(nbtOps(), nbt).result();
		}
	}
	
}
