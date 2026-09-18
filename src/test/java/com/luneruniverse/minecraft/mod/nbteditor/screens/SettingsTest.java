package com.luneruniverse.minecraft.mod.nbteditor.screens;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.luneruniverse.minecraft.mod.nbteditor.screens.ConfigScreen.Alias;
import com.luneruniverse.minecraft.mod.nbteditor.screens.ConfigScreen.CheckUpdatesLevel;
import com.luneruniverse.minecraft.mod.nbteditor.screens.ConfigScreen.CreativeTabsPosition;
import com.luneruniverse.minecraft.mod.nbteditor.screens.ConfigScreen.EnchantLevelMax;
import com.luneruniverse.minecraft.mod.nbteditor.screens.Settings.Setting;

class SettingsTest {
	
	@BeforeEach
	void resetToDefaults() {
		Settings.load(new JsonObject());
	}
	
	/**
	 * The defect the table exists to make unrepresentable. "jsonText" sits tenth in a config
	 * written by an older build, and shortcuts, aliases, the update level and the tab position are
	 * all stored after it.
	 */
	@Test
	void aMissingKeyResetsItselfAndNothingElse() {
		Settings.SHORTCUTS.get().add("/say mine");
		Settings.ALIASES.get().add(new Alias("nbteditor", "mine"));
		Settings.CHECK_UPDATES.set(CheckUpdatesLevel.NONE);
		Settings.CREATIVE_TABS_POS.set(CreativeTabsPosition.TOP_RIGHT);
		Settings.NORMAL_TEXT.set(true);
		JsonObject stored = Settings.save();
		stored.remove("jsonText");
		
		assertFalse(Settings.load(stored));
		
		assertEquals(List.of("/say mine"), Settings.SHORTCUTS.get());
		assertTrue(Settings.ALIASES.get().contains(new Alias("nbteditor", "mine")));
		assertEquals(CheckUpdatesLevel.NONE, Settings.CHECK_UPDATES.get());
		assertEquals(CreativeTabsPosition.TOP_RIGHT, Settings.CREATIVE_TABS_POS.get());
		assertFalse(Settings.NORMAL_TEXT.get());
	}
	
	@Test
	void aWrongTypeResetsItselfAndNothingElse() {
		Settings.SCROLL_SPEED.set(9.0);
		Settings.KEY_TEXT_SIZE.set(0.75);
		Settings.SHORTCUTS.get().add("/say mine");
		JsonObject stored = Settings.save();
		stored.addProperty("scrollSpeed", "fast");
		
		assertFalse(Settings.load(stored));
		
		assertEquals(5.0, Settings.SCROLL_SPEED.get());
		assertEquals(0.75, Settings.KEY_TEXT_SIZE.get());
		assertEquals(List.of("/say mine"), Settings.SHORTCUTS.get());
	}
	
	@Test
	void anUnknownEnumNameResetsItselfAndNothingElse() {
		Settings.ENCHANT_LEVEL_MAX.set(EnchantLevelMax.ALWAYS);
		Settings.AIR_EDITABLE.set(true);
		JsonObject stored = Settings.save();
		stored.addProperty("maxEnchantLevelDisplay", "SOMETIMES");
		
		assertFalse(Settings.load(stored));
		
		assertEquals(EnchantLevelMax.NEVER, Settings.ENCHANT_LEVEL_MAX.get());
		assertTrue(Settings.AIR_EDITABLE.get());
	}
	
	@Test
	void aCompleteFileNeedsNoWriteBack() {
		assertTrue(Settings.load(Settings.save()));
	}
	
	@Test
	void everySettingSurvivesARoundTrip() {
		Settings.ENCHANT_LEVEL_MAX.set(EnchantLevelMax.ALWAYS);
		Settings.SCROLL_SPEED.set(7.5);
		Settings.SPECIAL_NUMBERS.set(false);
		Settings.SHORTCUTS.get().add("/say mine");
		Settings.ALIASES.get().clear();
		Settings.ALIASES.get().add(new Alias("clientchest", "mine"));
		
		assertTrue(Settings.load(Settings.save()));
		
		assertEquals(EnchantLevelMax.ALWAYS, Settings.ENCHANT_LEVEL_MAX.get());
		assertEquals(7.5, Settings.SCROLL_SPEED.get());
		assertFalse(Settings.SPECIAL_NUMBERS.get());
		assertEquals(List.of("/say mine"), Settings.SHORTCUTS.get());
		assertEquals(List.of(new Alias("clientchest", "mine")), Settings.ALIASES.get());
	}
	
	@Test
	void theUpdateLevelStillReadsTheBooleanItUsedToBe() {
		JsonObject stored = Settings.save();
		
		stored.addProperty("checkUpdates", true);
		assertTrue(Settings.load(stored));
		assertEquals(CheckUpdatesLevel.MINOR, Settings.CHECK_UPDATES.get());
		
		stored.addProperty("checkUpdates", false);
		assertTrue(Settings.load(stored));
		assertEquals(CheckUpdatesLevel.NONE, Settings.CHECK_UPDATES.get());
	}
	
	@Test
	void thePageKeybindsNegationSurvivesARoundTrip() {
		Settings.INVERTED_PAGE_KEYBINDS.set(true);
		JsonObject stored = Settings.save();
		
		assertFalse(stored.get("invertedPageKeybinds").getAsBoolean());
		assertTrue(Settings.load(stored));
		assertTrue(Settings.INVERTED_PAGE_KEYBINDS.get());
	}
	
	@Test
	void noTwoSettingsShareAKey() {
		List<String> keys = Settings.all().stream().map(Setting::key).collect(Collectors.toList());
		assertEquals(keys.size(), keys.stream().distinct().count());
	}
	
	/** The lists are handed to the screens to edit in place, so a shared default would carry over. */
	@Test
	void aListDefaultIsFreshEveryLoad() {
		Settings.SHORTCUTS.get().add("/say leaked");
		Settings.ALIASES.get().clear();
		
		Settings.load(new JsonObject());
		
		assertEquals(List.of(), Settings.SHORTCUTS.get());
		assertEquals(4, Settings.ALIASES.get().size());
	}
	
}
