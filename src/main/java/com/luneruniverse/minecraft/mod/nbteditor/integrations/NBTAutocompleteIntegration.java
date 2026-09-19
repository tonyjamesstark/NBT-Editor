package com.luneruniverse.minecraft.mod.nbteditor.integrations;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.luneruniverse.minecraft.mod.nbteditor.localnbt.LocalBlock;
import com.luneruniverse.minecraft.mod.nbteditor.localnbt.LocalEntity;
import com.luneruniverse.minecraft.mod.nbteditor.localnbt.LocalItem;
import com.luneruniverse.minecraft.mod.nbteditor.localnbt.LocalNBT;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.DynamicRegistryManagerHolder;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.nbt.manager.NBTManagers;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mt1006.nbt_ac.autocomplete.NbtSuggestionManager;

import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.Identifier;
import com.luneruniverse.minecraft.mod.nbteditor.util.AccessWidenedApi;
import com.luneruniverse.minecraft.mod.nbteditor.util.TextUtil;

public class NBTAutocompleteIntegration extends Integration {
	
	public static final Optional<NBTAutocompleteIntegration> INSTANCE = Integration.getOptional(NBTAutocompleteIntegration::new);
	
	private static StringRange shiftRange(StringRange range, int shift) {
		return new StringRange(range.getStart() + shift, range.getEnd() + shift);
	}
	private static Suggestion shiftSuggestion(Suggestion suggestion, int shift) {
		return replaceSuggestion(suggestion,
				new Suggestion(shiftRange(suggestion.getRange(), shift), suggestion.getText(), suggestion.getTooltip()));
	}
	/** A Suggestion is the key its subtext is filed under, so a rewritten one has to carry it over. */
	private static Suggestion replaceSuggestion(Suggestion oldSuggestion, Suggestion newSuggestion) {
		NbtSuggestionManager.subtextMap.put(newSuggestion, NbtSuggestionManager.subtextMap.remove(oldSuggestion));
		return newSuggestion;
	}
	
	private NBTAutocompleteIntegration() {}
	
	@Override
	public String getModId() {
		return "nbt_ac";
	}
	
	private CompletableFuture<Suggestions> getSuggestions(String type, Identifier id, Tag nbt, List<String> path, String key, String value, int cursor, Collection<String> otherTags) {
		if (value != null && otherTags != null)
			throw new IllegalArgumentException("Both value and otherTags can't be non-null at the same time!");
		if (key == null && value == null)
			throw new IllegalArgumentException("Both key and value can't be null at the same time!");
		
		boolean components = type.equals("item");
		
		boolean nextTagAllowed;
		if (value == null) {
			key = key.substring(0, cursor);
			nextTagAllowed = false;
		} else {
			value = value.substring(0, cursor);
			// A suggestion that starts a sibling tag is only usable while something is still open
			// to the left of the cursor. Counting depth rather than comparing against the length
			// of the whole value keeps a bracket inside a string from opening anything.
			nextTagAllowed = nestLevel(value) > 0;
		}
		
		if (key != null && (key.contains("{") || key.contains("[")))
			return new SuggestionsBuilder("", 0).buildFuture();
		
		StringBuilder pathBuilder = new StringBuilder();
		boolean firstKey = true;
		if (nbt != null) {
			for (String piece : path) {
				if (nbt instanceof CompoundTag compound) {
					if (firstKey && components) {
						pathBuilder.append('[');
						pathBuilder.append(piece);
						pathBuilder.append('=');
					} else {
						pathBuilder.append('{');
						pathBuilder.append(escapeKey(piece));
						pathBuilder.append(':');
					}
					nbt = compound.get(piece);
				} else if (nbt instanceof ListTag list) {
					pathBuilder.append('[');
					nbt = list.get(Integer.parseInt(piece));
				} else
					return new SuggestionsBuilder("", 0).buildFuture();
				firstKey = false;
			}
		}
		int fieldStart = pathBuilder.length();
		if (key != null) {
			if (nbt instanceof CompoundTag) {
				if (firstKey && components)
					pathBuilder.append('[');
				else
					pathBuilder.append('{');
			} else if (nbt instanceof ListTag)
				pathBuilder.append('[');
			else
				return new SuggestionsBuilder("", 0).buildFuture();
			fieldStart = pathBuilder.length();
			
			if (nbt instanceof CompoundTag) {
				if (firstKey && components)
					pathBuilder.append(key);
				else {
					String escapedKey = escapeKey(key);
					// The closing quote is only left off while the key itself is still being typed.
					if (key.equals(escapedKey))
						pathBuilder.append(key);
					else if (value == null)
						pathBuilder.append(escapedKey.substring(0, escapedKey.length() - 1));
					else
						pathBuilder.append(escapedKey);
				}
			}
			
			if (value != null) {
				if (nbt instanceof CompoundTag) {
					if (firstKey && components)
						pathBuilder.append('=');
					else
						pathBuilder.append(':');
					fieldStart = pathBuilder.length();
				}
				pathBuilder.append(value);
			}
		} else {
			if (firstKey && components) {
				pathBuilder.append("[container=[{item:{id:\"" + id + "\",components:");
				fieldStart = pathBuilder.length();
			}
			pathBuilder.append(value);
		}
		String pathStr = pathBuilder.toString();
		
		String suggestionId = type + "/" + id;
		final int fieldStartFinal = fieldStart;
		final String valueFinal = value;
		final boolean firstKeyFinal = firstKey;
		return loadFromName(suggestionId, pathStr, components).thenApply(suggestions -> {
			List<Suggestion> shiftedSuggestions = suggestions.getList().stream()
					.filter(suggestion ->
							!(suggestion.getText().isEmpty()) &&
							!(valueFinal == null && suggestion.getText().contains(":")) &&
							!(valueFinal == null && firstKeyFinal && components && suggestion.getText().contains("{")) &&
							!(!nextTagAllowed && (suggestion.getText().contains(",") ||
									suggestion.getText().contains("}") || suggestion.getText().contains("]"))) &&
							!(otherTags != null && otherTags.contains(suggestion.getText())))
					.map(suggestion -> {
						suggestion = shiftSuggestion(suggestion, -fieldStartFinal);
						if (firstKeyFinal && components) {
							String component = suggestion.getText();
							boolean extraEquals = component.endsWith("=");
							if (extraEquals)
								component = component.substring(0, component.length() - 1);
							if (alreadyPresent(otherTags, component))
								return null;
							if (extraEquals) {
								suggestion = replaceSuggestion(suggestion,
										new Suggestion(suggestion.getRange(), component, suggestion.getTooltip()));
							}
						}
						return suggestion;
					})
					.filter(suggestion -> suggestion != null)
					.collect(Collectors.toList());
			return new Suggestions(shiftRange(suggestions.getRange(), -fieldStartFinal), shiftedSuggestions);
		});
	}
	/**
	 * Whether the item already carries {@code component}, under any of its spellings.
	 *
	 * <p>A component name has a namespace that may be left off or left bare, and a leading
	 * {@code !} for one being removed, so the tag already there and the suggestion for it rarely
	 * look alike. Comparing both qualified, and without the {@code !}, is what makes them.
	 */
	private static boolean alreadyPresent(Collection<String> otherTags, String component) {
		if (otherTags == null)
			return false;
		String qualified = TextUtil.addNamespace(component);
		return otherTags.stream()
				.map(tag -> TextUtil.addNamespace(tag.startsWith("!") ? tag.substring(1) : tag))
				.anyMatch(qualified::equals);
	}
	
	/** How many brackets {@code snbt} leaves open, ignoring any that fall inside a string. */
	private static int nestLevel(String snbt) {
		int nestLevel = 0;
		Character quote = null;
		boolean escaped = false;
		for (char c : snbt.toCharArray()) {
			if (escaped)
				escaped = false;
			else if (quote != null && c == '\\')
				escaped = true;
			else if (quote != null)
				quote = (c == quote ? null : quote);
			else if (c == '"' || c == '\'')
				quote = c;
			else if (c == '[' || c == '{')
				nestLevel++;
			else if (c == ']' || c == '}')
				nestLevel--;
		}
		return nestLevel;
	}
	private String escapeKey(String key) {
		if (key.isEmpty() || AccessWidenedApi.isSimpleName(key))
			return key;
		return StringTag.quoteAndEscape(key);
	}
	private CompletableFuture<Suggestions> loadFromName(String name, String tag, boolean components) {
		if (components) {
			name = name.substring("item/".length());
			int shift = name.length();
			SuggestionsBuilder builder = new SuggestionsBuilder(name + tag, 0);
			return new ItemParser(DynamicRegistryManagerHolder.get()).fillSuggestions(builder).thenApply(suggestions -> {
				return new Suggestions(shiftRange(suggestions.getRange(), -shift), suggestions.getList().stream()
						.map(suggestion -> shiftSuggestion(suggestion, -shift)).collect(Collectors.toList()));
			});
		}
		return NbtSuggestionManager.loadFromName(name, tag, new SuggestionsBuilder(tag, 0), false);
	}
	
	public CompletableFuture<Suggestions> getSuggestions(LocalNBT nbt, List<String> path, String key, String value, int cursor, Collection<String> otherTags) {
		if (nbt instanceof LocalItem)
			return getSuggestions("item", nbt.getId(), nbt.getNBT(), path, key, value, cursor, otherTags);
		if (nbt instanceof LocalBlock)
			return getSuggestions("block", nbt.getId(), nbt.getNBT(), path, key, value, cursor, otherTags);
		if (nbt instanceof LocalEntity)
			return getSuggestions("entity", nbt.getId(), nbt.getNBT(), path, key, value, cursor, otherTags);
		return new SuggestionsBuilder("", 0).buildFuture();
	}
	public CompletableFuture<Suggestions> getSuggestions(LocalNBT nbt, List<String> path, String key, String value, int cursor) {
		return getSuggestions(nbt, path, key, value, cursor, null);
	}
	
}
