package com.luneruniverse.minecraft.mod.nbteditor.screens;

import com.luneruniverse.minecraft.mod.nbteditor.util.Drawing;
import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.luneruniverse.minecraft.mod.nbteditor.NBTEditor;
import com.luneruniverse.minecraft.mod.nbteditor.NBTEditorClient;
import com.luneruniverse.minecraft.mod.nbteditor.clientchest.ClientChestHelper;
import com.luneruniverse.minecraft.mod.nbteditor.clientchest.LargeClientChestPageCache;
import com.luneruniverse.minecraft.mod.nbteditor.clientchest.SmallClientChestPageCache;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVEnchantments;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVTooltip;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.ScreenTexts;
import com.luneruniverse.minecraft.mod.nbteditor.screens.configurable.ConfigButton;
import com.luneruniverse.minecraft.mod.nbteditor.screens.configurable.ConfigCategory;
import com.luneruniverse.minecraft.mod.nbteditor.screens.configurable.ConfigItem;
import com.luneruniverse.minecraft.mod.nbteditor.screens.configurable.ConfigPanel;
import com.luneruniverse.minecraft.mod.nbteditor.screens.configurable.ConfigTooltipSupplier;
import com.luneruniverse.minecraft.mod.nbteditor.screens.configurable.ConfigValueBoolean;
import com.luneruniverse.minecraft.mod.nbteditor.screens.configurable.ConfigValueDropdown;
import com.luneruniverse.minecraft.mod.nbteditor.screens.configurable.ConfigValueSlider;
import com.luneruniverse.minecraft.mod.nbteditor.screens.containers.ClientChestScreen;
import com.luneruniverse.minecraft.mod.nbteditor.screens.widgets.CreativeTabWidget;

import com.luneruniverse.minecraft.mod.nbteditor.screens.widgets.Buttons;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;

public class ConfigScreen extends TickableSupportingScreen {
	
	public enum EnchantLevelMax implements ConfigTooltipSupplier {
		NEVER("nbteditor.config.enchant_level_max.never", (level, maxLevel) -> false),
		NOT_MAXED_EXACT("nbteditor.config.enchant_level_max.not_exact", (level, maxLevel) -> level != maxLevel),
		NOT_MAXED("nbteditor.config.enchant_level_max.not_max", (level, maxLevel) -> level < maxLevel),
		ALWAYS("nbteditor.config.enchant_level_max.always", (level, maxLevel) -> true);
		
		private final Component label;
		private final BiFunction<Integer, Integer, Boolean> showMax;
		
		private EnchantLevelMax(String key, BiFunction<Integer, Integer, Boolean> showMax) {
			this.label = Component.translatableEscape(key);
			this.showMax = showMax;
		}
		
		public boolean shouldShowMax(int level, int maxLevel) {
			return showMax.apply(level, maxLevel);
		}
		public EnchantLevelMax next() {
			return values()[(this.ordinal() + 1) % values().length];
		}
		
		@Override
		public String toString() {
			return label.getString();
		}
		@Override
		public MVTooltip getTooltip() {
			List<Component> output = new ArrayList<>();
			for (int lvl = 1; lvl <= 3; lvl++)
				output.add(getEnchantNameWithMax(MVEnchantments.FIRE_ASPECT, lvl, this));
			return new MVTooltip(output);
		}
	}
	
	public enum CheckUpdatesLevel implements ConfigTooltipSupplier {
		MINOR("nbteditor.config.check_updates.minor", 1),
		PATCH("nbteditor.config.check_updates.patch", 2),
		NONE("nbteditor.config.check_updates.none", -1);
		
		private final Component label;
		private final Component desc;
		private final int level;
		
		private CheckUpdatesLevel(String key, int level) {
			this.label = Component.translatableEscape(key);
			this.desc = Component.translatableEscape(key + ".desc");
			this.level = level;
		}
		
		public int getLevel() {
			return level;
		}
		
		@Override
		public String toString() {
			return label.getString();
		}
		@Override
		public MVTooltip getTooltip() {
			return new MVTooltip(desc);
		}
	}
	
	public static record Alias(String original, String alias) {}
	
	public enum ItemSizeFormat {
		HIDDEN("nbteditor.config.item_size.hidden", -1, false),
		AUTO("nbteditor.config.item_size.auto", 0, false),
		AUTO_COMPRESSED("nbteditor.config.item_size.auto_compressed", 0, true),
		BYTE("nbteditor.config.item_size.byte", 1, false),
		KILOBYTE("nbteditor.config.item_size.kilobyte", 1000, false),
		MEGABYTE("nbteditor.config.item_size.megabyte", 1000000, false),
		GIGABYTE("nbteditor.config.item_size.gigabyte", 1000000000, false),
		BYTE_COMPRESSED("nbteditor.config.item_size.byte_compressed", 1, true),
		KILOBYTE_COMPRESSED("nbteditor.config.item_size.kilobyte_compressed", 1000, true),
		MEGABYTE_COMPRESSED("nbteditor.config.item_size.megabyte_compressed", 1000000, true),
		GIGABYTE_COMPRESSED("nbteditor.config.item_size.gigabyte_compressed", 1000000000, true);
		
		private final Component label;
		private final int magnitude;
		private final boolean compressed;
		
		private ItemSizeFormat(String key, int magnitude, boolean compressed) {
			this.label = Component.translatableEscape(key);
			this.magnitude = magnitude;
			this.compressed = compressed;
		}
		
		public int getMagnitude() {
			return magnitude;
		}
		public boolean isCompressed() {
			return compressed;
		}
		
		@Override
		public String toString() {
			return label.getString();
		}
	}
	
	public enum CreativeTabsPosition {
		BOTTOM_LEFT("nbteditor.config.creative_tabs_pos.bottom_left"),
		BOTTOM_CENTER("nbteditor.config.creative_tabs_pos.bottom_center"),
		BOTTOM_RIGHT("nbteditor.config.creative_tabs_pos.bottom_right"),
		TOP_LEFT("nbteditor.config.creative_tabs_pos.top_left"),
		TOP_CENTER("nbteditor.config.creative_tabs_pos.top_center"),
		TOP_RIGHT("nbteditor.config.creative_tabs_pos.top_right");
		
		private final Component label;
		
		private CreativeTabsPosition(String key) {
			this.label = Component.translatableEscape(key);
		}
		
		public boolean isTop() {
			return this == TOP_LEFT || this == TOP_CENTER || this == TOP_RIGHT;
		}
		
		public Point position(int index, int numTabs, int screenWidth, int screenHeight) {
			int x = switch (this) {
				case BOTTOM_LEFT, TOP_LEFT -> index * (CreativeTabWidget.WIDTH + 2) + 10;
				case BOTTOM_CENTER, TOP_CENTER -> {
					int tabsWidth = numTabs * (CreativeTabWidget.WIDTH + 2) - 2;
					int tabsStart = (screenWidth - tabsWidth) / 2;
					yield tabsStart + index * (CreativeTabWidget.WIDTH + 2);
				}
				case BOTTOM_RIGHT, TOP_RIGHT -> screenWidth - CreativeTabWidget.WIDTH - index * (CreativeTabWidget.WIDTH + 2) - 10;
			};
			
			int y = switch (this) {
				case BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT -> screenHeight - CreativeTabWidget.HEIGHT;
				case TOP_LEFT, TOP_CENTER, TOP_RIGHT -> 0;
			};
			
			return new Point(x, y);
		}
		
		@Override
		public String toString() {
			return label.getString();
		}
	}
	
	public static void loadSettings() {
		JsonObject stored;
		try {
			stored = new Gson().fromJson(
					Files.readString(new File(NBTEditorClient.SETTINGS_FOLDER, "settings.json").toPath()),
					JsonObject.class);
		} catch (NoSuchFileException e) {
			stored = new JsonObject();
		} catch (Exception e) {
			// Defaults stand, and nothing is written: a file that could not be read is not a
			// file to overwrite.
			NBTEditor.LOGGER.error("Error while loading settings", e);
			return;
		}
		if (!Settings.load(stored == null ? new JsonObject() : stored)) {
			NBTEditor.LOGGER.info("Missing some settings from settings.json, fixing ...");
			saveSettings();
		}
	}
	private static void saveSettings() {
		try {
			Files.write(new File(NBTEditorClient.SETTINGS_FOLDER, "settings.json").toPath(),
					new Gson().toJson(Settings.save()).getBytes());
		} catch (IOException e) {
			NBTEditor.LOGGER.error("Error while saving settings", e);
		}
	}
	
	public static EnchantLevelMax getEnchantLevelMax() {
		return Settings.ENCHANT_LEVEL_MAX.get();
	}
	public static boolean isEnchantNumberTypeArabic() {
		return Settings.ENCHANT_NUMBER_TYPE_ARABIC.get();
	}
	public static double getKeyTextSize() {
		return Settings.KEY_TEXT_SIZE.get();
	}
	public static boolean isKeybindsHidden() {
		return Settings.KEYBINDS_HIDDEN.get();
	}
	public static void setLockSlots(boolean lockSlots) {
		Settings.LOCK_SLOTS.set(lockSlots);
		saveSettings();
	}
	public static boolean isLockSlots() {
		return Settings.LOCK_SLOTS.get() || isLockSlotsRequired();
	}
	public static boolean isLockSlotsRequired() {
		return Minecraft.getInstance().gameMode != null && !NBTEditorClient.SERVER_CONN.isEditingAllowed();
	}
	public static boolean isChatLimitExtended() {
		return Settings.CHAT_LIMIT_EXTENDED.get();
	}
	public static boolean isSingleQuotesAllowed() {
		return Settings.SINGLE_QUOTES_ALLOWED.get();
	}
	public static double getScrollSpeed() {
		return Settings.SCROLL_SPEED.get();
	}
	public static boolean isAirEditable() {
		return Settings.AIR_EDITABLE.get();
	}
	public static boolean isNormalText() {
		return Settings.NORMAL_TEXT.get();
	}
	public static List<String> getShortcuts() {
		return Settings.SHORTCUTS.get();
	}
	public static CheckUpdatesLevel getCheckUpdates() {
		return Settings.CHECK_UPDATES.get();
	}
	public static boolean isLargeClientChest() {
		return Settings.LARGE_CLIENT_CHEST.get();
	}
	public static boolean isScreenshotOptions() {
		return Settings.SCREENSHOT_OPTIONS.get();
	}
	public static boolean isTooltipOverflowFix() {
		return Settings.TOOLTIP_OVERFLOW_FIX.get();
	}
	public static boolean isNoSlotRestrictions() {
		return Settings.NO_SLOT_RESTRICTIONS.get();
	}
	public static boolean isHideFormatButtons() {
		return Settings.HIDE_FORMAT_BUTTONS.get();
	}
	public static boolean isSpecialNumbers() {
		return Settings.SPECIAL_NUMBERS.get();
	}
	public static List<Alias> getAliases() {
		return Settings.ALIASES.get();
	}
	public static ItemSizeFormat getItemSizeFormat() {
		return Settings.ITEM_SIZE_FORMAT.get();
	}
	public static boolean isInvertedPageKeybinds() {
		return Settings.INVERTED_PAGE_KEYBINDS.get();
	}
	public static boolean isTriggerBlockUpdates() {
		return Settings.TRIGGER_BLOCK_UPDATES.get();
	}
	public static boolean isWarnIncompatibleProtocol() {
		return Settings.WARN_INCOMPATIBLE_PROTOCOL.get();
	}
	public static boolean isRecreateBlocksAndEntities() {
		return Settings.RECREATE_BLOCKS_AND_ENTITIES.get();
	}
	public static CreativeTabsPosition getCreativeTabsPos() {
		return Settings.CREATIVE_TABS_POS.get();
	}
	
	private static MutableComponent getEnchantName(Enchantment enchant, int level) {
		MutableComponent output = MVEnchantments.getEnchantmentName(enchant).copy();
        if (level != 1 || enchant.getMaxLevel() != 1 || getEnchantLevelMax() == EnchantLevelMax.ALWAYS) {
            output.append(" ");
            if (isEnchantNumberTypeArabic())
            	output.append("" + level);
            else
            	output.append(Component.translatableEscape("enchantment.level." + level));
        }
        return output;
	}
	public static Component getEnchantNameWithMax(Enchantment enchant, int level, EnchantLevelMax display) {
		MutableComponent text = getEnchantName(enchant, level);
		if (display.shouldShowMax(level, enchant.getMaxLevel())) {
			text = text.append("/").append(
					ConfigScreen.isEnchantNumberTypeArabic() ?
							Component.nullToEmpty("" + enchant.getMaxLevel()) :
							Component.translatableEscape("enchantment.level." + enchant.getMaxLevel()));
		}
		return text;
	}
	public static Component getEnchantNameWithMax(Enchantment enchant, int level) {
		return getEnchantNameWithMax(enchant, level, getEnchantLevelMax());
	}
	
	
	public static final List<Consumer<ConfigCategory>> ADDED_OPTIONS = new ArrayList<>();
	
	
	private final Screen parent;
	private final ConfigCategory config;
	private ConfigPanel panel;
	
	public ConfigScreen(Screen parent) {
		super(Component.translatableEscape("nbteditor.config"));
		this.parent = parent;
		this.config = new ConfigCategory(Component.translatableEscape("nbteditor.config"));
		
		ConfigCategory mc = new ConfigCategory(Component.translatableEscape("nbteditor.config.category.mc"));
		ConfigCategory guis = new ConfigCategory(Component.translatableEscape("nbteditor.config.category.guis"));
		ConfigCategory functional = new ConfigCategory(Component.translatableEscape("nbteditor.config.category.functional"));
		this.config.setConfigurable("mc", mc);
		this.config.setConfigurable("guis", guis);
		this.config.setConfigurable("functional", functional);
		
		
		// ---------- MC ----------
		
		mc.setConfigurable("extendChatLimit", new ConfigItem<>(Component.translatableEscape("nbteditor.config.chat_limit"),
				new ConfigValueBoolean(Settings.CHAT_LIMIT_EXTENDED.get(), false, 100, Component.translatableEscape("nbteditor.config.chat_limit.extended"), Component.translatableEscape("nbteditor.config.chat_limit.normal"))
				.addValueListener(value -> Settings.CHAT_LIMIT_EXTENDED.set(value.getValidValue())))
				.setTooltip("nbteditor.config.chat_limit.desc"));
		
		mc.setConfigurable("tooltipOverflowFix", new ConfigItem<>(Component.translatableEscape("nbteditor.config.tooltip_overflow_fix"),
				new ConfigValueBoolean(Settings.TOOLTIP_OVERFLOW_FIX.get(), true, 100, Component.translatableEscape("nbteditor.config.tooltip_overflow_fix.enabled"), Component.translatableEscape("nbteditor.config.tooltip_overflow_fix.disabled"))
				.addValueListener(value -> Settings.TOOLTIP_OVERFLOW_FIX.set(value.getValidValue())))
				.setTooltip("nbteditor.config.tooltip_overflow_fix.desc"));
		
		mc.setConfigurable("maxEnchantLevelDisplay", new ConfigItem<>(Component.translatableEscape("nbteditor.config.enchant_level_max"),
				ConfigValueDropdown.forEnum(Settings.ENCHANT_LEVEL_MAX.get(), EnchantLevelMax.NEVER, EnchantLevelMax.class)
				.addValueListener(value -> Settings.ENCHANT_LEVEL_MAX.set(value.getValidValue())))
				.setTooltip("nbteditor.config.enchant_level_max.desc"));
		
		mc.setConfigurable("useArabicEnchantLevels", new ConfigItem<>(Component.translatableEscape("nbteditor.config.enchant_number_type"),
				new ConfigValueBoolean(Settings.ENCHANT_NUMBER_TYPE_ARABIC.get(), false, 100, Component.translatableEscape("nbteditor.config.enchant_number_type.arabic"),
				Component.translatableEscape("nbteditor.config.enchant_number_type.roman"), new MVTooltip(Component.translatableEscape("nbteditor.config.enchant_number_type.desc2")))
				.addValueListener(value -> Settings.ENCHANT_NUMBER_TYPE_ARABIC.set(value.getValidValue())))
				.setTooltip("nbteditor.config.enchant_number_type.desc"));
		
		mc.setConfigurable("noSlotRestrictions", new ConfigItem<>(Component.translatableEscape("nbteditor.config.no_slot_restrictions"),
				new ConfigValueBoolean(Settings.NO_SLOT_RESTRICTIONS.get(), false, 100, Component.translatableEscape("nbteditor.config.no_slot_restrictions.enabled"), Component.translatableEscape("nbteditor.config.no_slot_restrictions.disabled"))
				.addValueListener(value -> Settings.NO_SLOT_RESTRICTIONS.set(value.getValidValue())))
				.setTooltip("nbteditor.config.no_slot_restrictions.desc"));
		
		mc.setConfigurable("screenshotOptions", new ConfigItem<>(Component.translatableEscape("nbteditor.config.screenshot_options"),
				new ConfigValueBoolean(Settings.SCREENSHOT_OPTIONS.get(), true, 100, Component.translatableEscape("nbteditor.config.screenshot_options.enabled"), Component.translatableEscape("nbteditor.config.screenshot_options.disabled"))
				.addValueListener(value -> Settings.SCREENSHOT_OPTIONS.set(value.getValidValue())))
				.setTooltip(new MVTooltip(Component.translatableEscape("nbteditor.config.screenshot_options.desc", Component.translatableEscape("nbteditor.file_options.show"), Component.translatableEscape("nbteditor.file_options.delete")))));
		
		// ---------- GUIs ----------
		
		guis.setConfigurable("creativeTabsPos", new ConfigItem<>(Component.translatableEscape("nbteditor.config.creative_tabs_pos"),
				ConfigValueDropdown.forEnum(Settings.CREATIVE_TABS_POS.get(), CreativeTabsPosition.BOTTOM_LEFT, CreativeTabsPosition.class)
				.addValueListener(value -> Settings.CREATIVE_TABS_POS.set(value.getValidValue())))
				.setTooltip("nbteditor.config.creative_tabs_pos.desc"));
		
		guis.setConfigurable("scrollSpeed", new ConfigItem<>(Component.translatableEscape("nbteditor.config.scroll_speed"),
				ConfigValueSlider.forDouble(100, Settings.SCROLL_SPEED.get(), 5, 0.5, 10, 0.05, value -> Component.literal(String.format("%.2f", value)))
				.addValueListener(value -> Settings.SCROLL_SPEED.set(value.getValidValue())))
				.setTooltip("nbteditor.config.scroll_speed.desc"));
		
		guis.setConfigurable("hideFormatButtons", new ConfigItem<>(Component.translatableEscape("nbteditor.config.hide_format_buttons"),
				new ConfigValueBoolean(Settings.HIDE_FORMAT_BUTTONS.get(), false, 100, Component.translatableEscape("nbteditor.config.hide_format_buttons.enabled"), Component.translatableEscape("nbteditor.config.hide_format_buttons.disabled"))
				.addValueListener(value -> Settings.HIDE_FORMAT_BUTTONS.set(value.getValidValue())))
				.setTooltip("nbteditor.config.hide_format_buttons.desc"));
		
		guis.setConfigurable("hideKeybinds", new ConfigItem<>(Component.translatableEscape("nbteditor.config.keybinds"),
				new ConfigValueBoolean(Settings.KEYBINDS_HIDDEN.get(), false, 100, Component.translatableEscape("nbteditor.config.keybinds.hidden"), Component.translatableEscape("nbteditor.config.keybinds.shown"),
				new MVTooltip("nbteditor.keybind.edit", "nbteditor.keybind.factory", "nbteditor.keybind.container", "nbteditor.keybind.enchant", "nbteditor.keybind.delete"))
				.addValueListener(value -> Settings.KEYBINDS_HIDDEN.set(value.getValidValue())))
				.setTooltip("nbteditor.config.keybinds.desc"));
		
		guis.setConfigurable("invertedPageKeybinds", new ConfigItem<>(Component.translatableEscape("nbteditor.config.page_keybinds"),
				new ConfigValueBoolean(Settings.INVERTED_PAGE_KEYBINDS.get(), false, 100, Component.translatableEscape("nbteditor.config.page_keybinds.inverted"), Component.translatableEscape("nbteditor.config.page_keybinds.normal"))
				.addValueListener(value -> Settings.INVERTED_PAGE_KEYBINDS.set(value.getValidValue())))
				.setTooltip("nbteditor.config.page_keybinds.desc"));
		
		guis.setConfigurable("itemSize", new ConfigItem<>(Component.translatableEscape("nbteditor.config.item_size"),
				ConfigValueDropdown.forEnum(Settings.ITEM_SIZE_FORMAT.get(), ItemSizeFormat.HIDDEN, ItemSizeFormat.class)
				.addValueListener(value -> Settings.ITEM_SIZE_FORMAT.set(value.getValidValue())))
				.setTooltip("nbteditor.config.item_size.desc"));
		
		guis.setConfigurable("keyTextSize", new ConfigItem<>(Component.translatableEscape("nbteditor.config.key_text_size"),
				ConfigValueSlider.forDouble(100, Settings.KEY_TEXT_SIZE.get(), 0.5, 0.5, 1, 0.05, value -> Component.literal(String.format("%.2f", value)))
				.addValueListener(value -> Settings.KEY_TEXT_SIZE.set(value.getValidValue())))
				.setTooltip("nbteditor.config.key_text_size.desc"));
		
		guis.setConfigurable("checkUpdates", new ConfigItem<>(Component.translatableEscape("nbteditor.config.check_updates"),
				ConfigValueDropdown.forEnum(Settings.CHECK_UPDATES.get(), CheckUpdatesLevel.MINOR, CheckUpdatesLevel.class)
				.addValueListener(value -> Settings.CHECK_UPDATES.set(value.getValidValue())))
				.setTooltip("nbteditor.config.check_updates.desc"));
		
		guis.setConfigurable("warnIncompatibleProtocol", new ConfigItem<>(Component.translatableEscape("nbteditor.config.warn_incompatible_protocol"),
				new ConfigValueBoolean(Settings.WARN_INCOMPATIBLE_PROTOCOL.get(), true, 100, Component.translatableEscape("nbteditor.config.warn_incompatible_protocol.enabled"), Component.translatableEscape("nbteditor.config.warn_incompatible_protocol.disabled"))
				.addValueListener(value -> Settings.WARN_INCOMPATIBLE_PROTOCOL.set(value.getValidValue())))
				.setTooltip("nbteditor.config.warn_incompatible_protocol.desc"));
		
		// ---------- FUNCTIONAL ----------
		
		functional.setConfigurable("aliases", new ConfigButton(100, Component.translatableEscape("nbteditor.config.aliases"),
				btn -> minecraft.setScreenAndShow(new AliasesScreen(this)), new MVTooltip("nbteditor.config.aliases.desc")));
		
		functional.setConfigurable("shortcuts", new ConfigButton(100, Component.translatableEscape("nbteditor.config.shortcuts"),
				btn -> minecraft.setScreenAndShow(new ShortcutsScreen(this)), new MVTooltip("nbteditor.config.shortcuts.desc")));
		
		functional.setConfigurable("recreateBlocksAndEntities", new ConfigItem<>(Component.translatableEscape("nbteditor.config.recreate_blocks_and_entities"),
				new ConfigValueBoolean(Settings.RECREATE_BLOCKS_AND_ENTITIES.get(), false, 100, Component.translatableEscape("nbteditor.config.recreate_blocks_and_entities.enabled"), Component.translatableEscape("nbteditor.config.recreate_blocks_and_entities.disabled"))
				.addValueListener(value -> Settings.RECREATE_BLOCKS_AND_ENTITIES.set(value.getValidValue())))
				.setTooltip("nbteditor.config.recreate_blocks_and_entities.desc"));
		
		functional.setConfigurable("largeClientChest", new ConfigItem<>(Component.translatableEscape("nbteditor.config.client_chest_size"),
				new ConfigValueBoolean(Settings.LARGE_CLIENT_CHEST.get(), false, 100, Component.translatableEscape("nbteditor.config.client_chest_size.large"), Component.translatableEscape("nbteditor.config.client_chest_size.small"))
				.addValueListener(value -> Settings.LARGE_CLIENT_CHEST.set(value.getValidValue())))
				.setTooltip("nbteditor.config.client_chest_size.desc"));
		
		functional.setConfigurable("airEditable", new ConfigItem<>(Component.translatableEscape("nbteditor.config.air_editable"),
				new ConfigValueBoolean(Settings.AIR_EDITABLE.get(), false, 100, Component.translatableEscape("nbteditor.config.air_editable.yes"), Component.translatableEscape("nbteditor.config.air_editable.no"))
				.addValueListener(value -> Settings.AIR_EDITABLE.set(value.getValidValue())))
				.setTooltip("nbteditor.config.air_editable.desc"));
		
		functional.setConfigurable("specialNumbers", new ConfigItem<>(Component.translatableEscape("nbteditor.config.special_numbers"),
				new ConfigValueBoolean(Settings.SPECIAL_NUMBERS.get(), true, 100, Component.translatableEscape("nbteditor.config.special_numbers.enabled"), Component.translatableEscape("nbteditor.config.special_numbers.disabled"))
				.addValueListener(value -> Settings.SPECIAL_NUMBERS.set(value.getValidValue())))
				.setTooltip("nbteditor.config.special_numbers.desc"));
		
		functional.setConfigurable("triggerBlockUpdates", new ConfigItem<>(Component.translatableEscape("nbteditor.config.trigger_block_updates"),
				new ConfigValueBoolean(Settings.TRIGGER_BLOCK_UPDATES.get(), true, 100, Component.translatableEscape("nbteditor.config.trigger_block_updates.yes"), Component.translatableEscape("nbteditor.config.trigger_block_updates.no"))
				.addValueListener(value -> Settings.TRIGGER_BLOCK_UPDATES.set(value.getValidValue())))
				.setTooltip("nbteditor.config.trigger_block_updates.desc"));
		
		functional.setConfigurable("normalText", new ConfigItem<>(Component.translatableEscape("nbteditor.config.normal_text"),
				new ConfigValueBoolean(Settings.NORMAL_TEXT.get(), false, 100, Component.translatableEscape("nbteditor.config.normal_text.yes"), Component.translatableEscape("nbteditor.config.normal_text.no"))
				.addValueListener(value -> Settings.NORMAL_TEXT.set(value.getValidValue())))
				.setTooltip("nbteditor.config.normal_text.desc"));
		
		functional.setConfigurable("allowSingleQuotes", new ConfigItem<>(Component.translatableEscape("nbteditor.config.single_quotes"),
				new ConfigValueBoolean(Settings.SINGLE_QUOTES_ALLOWED.get(), false, 100, Component.translatableEscape("nbteditor.config.single_quotes.allowed"),
				Component.translatableEscape("nbteditor.config.single_quotes.not_allowed"), new MVTooltip("nbteditor.config.single_quotes.example"))
				.addValueListener(value -> Settings.SINGLE_QUOTES_ALLOWED.set(value.getValidValue())))
				.setTooltip("nbteditor.config.single_quotes.desc"));
		
		ADDED_OPTIONS.forEach(option -> option.accept(config));
	}
	
	@Override
	protected void init() {
		ConfigPanel newPanel = addRenderableWidget(new ConfigPanel(16, 16, width - 32, height - 32, config));
		if (panel != null)
			newPanel.setScroll(panel.getScroll());
		panel = newPanel;
		
		this.addRenderableWidget(Buttons.of(this.width - 134, this.height - 36, 100, 20, ScreenTexts.DONE, btn -> close()));
	}
	
	public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		Drawing.renderBackground(this, context);
		super.extractRenderState(context, mouseX, mouseY, delta);
	}
	
	public void close() {
		minecraft.setScreenAndShow(this.parent);
	}
	
	@Override
	public void removed() {
		saveSettings();
		if (isLargeClientChest() != (NBTEditorClient.CLIENT_CHEST.getCache() instanceof LargeClientChestPageCache)) {
			NBTEditorClient.CLIENT_CHEST.setCache(isLargeClientChest() ? new LargeClientChestPageCache(5) : new SmallClientChestPageCache(100))
					.thenAccept(v -> ClientChestHelper.loadDefaultPages(NBTEditorClient.CLIENT_CHEST.getLoadLevel(0)));
			ClientChestScreen.PAGE = Math.min(ClientChestScreen.PAGE, NBTEditorClient.CLIENT_CHEST.getPageCount() - 1);
		}
	}
	
}
