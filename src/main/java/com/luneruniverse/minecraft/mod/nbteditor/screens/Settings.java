package com.luneruniverse.minecraft.mod.nbteditor.screens;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.luneruniverse.minecraft.mod.nbteditor.screens.ConfigScreen.Alias;
import com.luneruniverse.minecraft.mod.nbteditor.screens.ConfigScreen.CheckUpdatesLevel;
import com.luneruniverse.minecraft.mod.nbteditor.screens.ConfigScreen.CreativeTabsPosition;
import com.luneruniverse.minecraft.mod.nbteditor.screens.ConfigScreen.EnchantLevelMax;
import com.luneruniverse.minecraft.mod.nbteditor.screens.ConfigScreen.ItemSizeFormat;

/**
 * Everything settings.json holds, one row per setting: the key it is stored under, its default,
 * and how it is read and written.
 *
 * <p>A row owns its own failure. {@link #load} asks each one for its key in turn, and a row that
 * finds nothing usable takes its default and says so, which leaves the other twenty-four alone.
 * That is the whole point of the table. The settings used to be loaded by twenty-five sequential
 * reads inside one try whose handler saved; a single missing key threw part-way down, so every
 * setting after it silently reverted and the result was written over the user's file. Shortcuts
 * and aliases are typed by hand and both sit late in that order.
 *
 * <p>Seven keys are older than the names the code uses, and two are older than the meaning. They
 * stay as they are so an existing settings.json keeps working, and each row states its own rather
 * than leaving the reader to pair up two lists.
 *
 * <p>Minecraft-free on purpose, apart from the enums it stores, so the load contract is testable
 * without a game. See ADR-0004.
 */
public class Settings {
	
	private static final Logger LOGGER = LogManager.getLogger("nbteditor");
	
	private static final List<Setting<?>> ALL = new ArrayList<>();
	
	public static final Setting<EnchantLevelMax> ENCHANT_LEVEL_MAX =
			ofEnum("maxEnchantLevelDisplay", EnchantLevelMax.NEVER, EnchantLevelMax.class);
	public static final Setting<Boolean> ENCHANT_NUMBER_TYPE_ARABIC = ofBoolean("useArabicEnchantLevels", false);
	public static final Setting<Double> KEY_TEXT_SIZE = ofDouble("keyTextSize", 0.5);
	public static final Setting<Boolean> KEYBINDS_HIDDEN = ofBoolean("hideKeybinds", false);
	/** Not shown in the config screen; {@code ConfigScreen.setLockSlots} is what moves it. */
	public static final Setting<Boolean> LOCK_SLOTS = ofBoolean("lockSlots", false);
	public static final Setting<Boolean> CHAT_LIMIT_EXTENDED = ofBoolean("extendChatLimit", false);
	public static final Setting<Boolean> SINGLE_QUOTES_ALLOWED = ofBoolean("allowSingleQuotes", false);
	public static final Setting<Double> SCROLL_SPEED = ofDouble("scrollSpeed", 5);
	public static final Setting<Boolean> AIR_EDITABLE = ofBoolean("airEditable", false);
	public static final Setting<Boolean> NORMAL_TEXT = ofBoolean("jsonText", false);
	public static final Setting<List<String>> SHORTCUTS = of("shortcuts", ArrayList::new,
			json -> json.getAsJsonArray().asList().stream().map(JsonElement::getAsString)
					.collect(Collectors.toCollection(ArrayList::new)),
			shortcuts -> shortcuts.stream().collect(JsonArray::new, JsonArray::add, JsonArray::addAll));
	/**
	 * Stored as a JSON boolean before the level existed, and a config written then is still out
	 * there; true meant the minor level and false meant none.
	 */
	public static final Setting<CheckUpdatesLevel> CHECK_UPDATES = of("checkUpdates",
			() -> CheckUpdatesLevel.MINOR,
			json -> json.getAsJsonPrimitive().isBoolean()
					? (json.getAsBoolean() ? CheckUpdatesLevel.MINOR : CheckUpdatesLevel.NONE)
					: CheckUpdatesLevel.valueOf(json.getAsString()),
			level -> new JsonPrimitive(level.name()));
	public static final Setting<Boolean> LARGE_CLIENT_CHEST = ofBoolean("largeClientChest", false);
	public static final Setting<Boolean> SCREENSHOT_OPTIONS = ofBoolean("screenshotOptions", true);
	public static final Setting<Boolean> TOOLTIP_OVERFLOW_FIX = ofBoolean("tooltipOverflowFix", true);
	public static final Setting<Boolean> NO_SLOT_RESTRICTIONS = ofBoolean("noArmorRestriction", false);
	public static final Setting<Boolean> HIDE_FORMAT_BUTTONS = ofBoolean("hideFormatButtons", false);
	public static final Setting<Boolean> SPECIAL_NUMBERS = ofBoolean("specialNumbers", true);
	public static final Setting<List<Alias>> ALIASES = of("aliases",
			() -> new ArrayList<>(List.of(
					new Alias("nbteditor", "nbt"),
					new Alias("clientchest", "chest"),
					new Alias("clientchest", "storage"),
					new Alias("factory signature", "sign"))),
			json -> json.getAsJsonArray().asList().stream()
					.map(alias -> new Alias(alias.getAsJsonObject().get("original").getAsString(),
							alias.getAsJsonObject().get("alias").getAsString()))
					.collect(Collectors.toCollection(ArrayList::new)),
			aliases -> aliases.stream().map(alias -> {
				JsonObject obj = new JsonObject();
				obj.addProperty("original", alias.original());
				obj.addProperty("alias", alias.alias());
				return (JsonElement) obj;
			}).collect(JsonArray::new, JsonArray::add, JsonArray::addAll));
	public static final Setting<ItemSizeFormat> ITEM_SIZE_FORMAT =
			ofEnum("itemSize", ItemSizeFormat.HIDDEN, ItemSizeFormat.class);
	/**
	 * Negated on both sides: the stored key predates the swap of what "normal" means, so an
	 * existing config keeps the behaviour its owner chose.
	 */
	public static final Setting<Boolean> INVERTED_PAGE_KEYBINDS = of("invertedPageKeybinds", () -> false,
			json -> !json.getAsBoolean(), inverted -> new JsonPrimitive(!inverted));
	public static final Setting<Boolean> TRIGGER_BLOCK_UPDATES = ofBoolean("triggerBlockUpdates", true);
	public static final Setting<Boolean> WARN_INCOMPATIBLE_PROTOCOL = ofBoolean("warnIncompatibleProtocol", true);
	public static final Setting<Boolean> RECREATE_BLOCKS_AND_ENTITIES = ofBoolean("recreateBlocksAndEntities", false);
	public static final Setting<CreativeTabsPosition> CREATIVE_TABS_POS =
			ofEnum("creativeTabsPos", CreativeTabsPosition.BOTTOM_LEFT, CreativeTabsPosition.class);
	
	/**
	 * Takes each setting's value from <code>settings</code>, or its default when that key is
	 * missing or unreadable.
	 * @return whether every setting came from the file, meaning there is nothing to write back.
	 */
	public static boolean load(JsonObject settings) {
		boolean complete = true;
		for (Setting<?> setting : ALL)
			complete &= setting.load(settings);
		return complete;
	}
	
	public static JsonObject save() {
		JsonObject settings = new JsonObject();
		for (Setting<?> setting : ALL)
			setting.save(settings);
		return settings;
	}
	
	public static List<Setting<?>> all() {
		return List.copyOf(ALL);
	}
	
	private static Setting<Boolean> ofBoolean(String key, boolean defaultValue) {
		return of(key, () -> defaultValue, JsonElement::getAsBoolean, JsonPrimitive::new);
	}
	private static Setting<Double> ofDouble(String key, double defaultValue) {
		return of(key, () -> defaultValue, JsonElement::getAsDouble, JsonPrimitive::new);
	}
	private static <E extends Enum<E>> Setting<E> ofEnum(String key, E defaultValue, Class<E> type) {
		return of(key, () -> defaultValue, json -> Enum.valueOf(type, json.getAsString()),
				value -> new JsonPrimitive(value.name()));
	}
	private static <T> Setting<T> of(String key, Supplier<T> defaultValue,
			Function<JsonElement, T> read, Function<T, JsonElement> write) {
		Setting<T> setting = new Setting<>(key, defaultValue, read, write);
		ALL.add(setting);
		return setting;
	}
	
	public static final class Setting<T> {
		
		private final String key;
		private final Supplier<T> defaultValue;
		private final Function<JsonElement, T> read;
		private final Function<T, JsonElement> write;
		private T value;
		
		private Setting(String key, Supplier<T> defaultValue,
				Function<JsonElement, T> read, Function<T, JsonElement> write) {
			this.key = key;
			this.defaultValue = defaultValue;
			this.read = read;
			this.write = write;
			this.value = defaultValue.get();
		}
		
		public String key() {
			return key;
		}
		public T get() {
			return value;
		}
		public void set(T value) {
			this.value = value;
		}
		
		private boolean load(JsonObject settings) {
			JsonElement stored = settings.get(key);
			if (stored != null) {
				try {
					value = read.apply(stored);
					return true;
				} catch (RuntimeException e) {
					LOGGER.warn("Ignoring unreadable setting {} in settings.json", key, e);
				}
			}
			// A fresh default every time. Shortcuts and aliases hand out the live list for the
			// screens to edit, so one shared instance would carry a previous session's edits.
			value = defaultValue.get();
			return false;
		}
		
		private void save(JsonObject settings) {
			settings.add(key, write.apply(value));
		}
		
	}
	
}
