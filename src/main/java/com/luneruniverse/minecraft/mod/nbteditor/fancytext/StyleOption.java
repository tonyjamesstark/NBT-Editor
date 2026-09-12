package com.luneruniverse.minecraft.mod.nbteditor.fancytext;

public enum StyleOption {
	OPEN_URL,
	RUN_COMMAND,
	SUGGEST_COMMAND,
	CHANGE_PAGE,
	COPY_TO_CLIPBOARD,
	SHOW_TEXT,
	SHOW_ITEM,
	SHOW_ENTITY,
	INSERTION,
	FONT;
	
	/**
	 * @return The matching option, or null if the name isn't a style option
	 */
	public static StyleOption get(String name) {
		for (StyleOption option : values()) {
			if (option.name().equalsIgnoreCase(name))
				return option;
		}
		return null;
	}
	
}
