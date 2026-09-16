package com.luneruniverse.minecraft.mod.nbteditor.screens.widgets;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import org.lwjgl.glfw.GLFW;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVTooltip;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.TextInst;
import com.luneruniverse.minecraft.mod.nbteditor.screens.ConfigScreen;
import com.luneruniverse.minecraft.mod.nbteditor.screens.OverlaySupportingScreen;
import com.luneruniverse.minecraft.mod.nbteditor.util.MainUtil;
import com.luneruniverse.minecraft.mod.nbteditor.util.StyleUtil;
import com.luneruniverse.minecraft.mod.nbteditor.util.TextUtil;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import com.luneruniverse.minecraft.mod.nbteditor.screens.widgets.Buttons;
import com.luneruniverse.minecraft.mod.nbteditor.util.Keys;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;

public class FormattedTextFieldWidget extends GroupWidget {
	
	private static class InternalTextFieldWidget extends MultiLineTextFieldWidget {
		public static InternalTextFieldWidget create(InternalTextFieldWidget prev, int x, int y, int width, int height,
				Component text, boolean newLines, Style base, Consumer<Component> onChange) {
			if (prev == null)
				return new InternalTextFieldWidget(x, y, width, height, text, newLines, base, onChange);
			if (prev.allowsNewLines() != newLines)
				throw new IllegalArgumentException("Cannot convert to/from newLines on FormattedTextFieldWidget");
			if (!StyleUtil.identical(prev.base, base))
				throw new IllegalArgumentException("Cannot change base on FormattedTextFieldWidget");
			prev.setTextChangeListener(onChange);
			prev.ignoreNextSetText = true;
			MultiLineTextFieldWidget.create(prev, x, y, width, height, text.getString(),
					str -> prev.text, newLines, str -> prev.onChange.accept(prev.text));
			prev.setFormattedText(text);
			return prev;
		}
		
		private Consumer<Component> onChange;
		private final Style base;
		private final Style baseReset;
		private final List<Style> styles;
		private Style cursorStyle;
		private Component text;
		private boolean ignoreNextEditStyles;
		private boolean ignoreNextSetText;
		private final List<Component> undo;
		private int undoPos;
		
		protected InternalTextFieldWidget(int x, int y, int width, int height, Component text, boolean newLines, Style base, Consumer<Component> onChange) {
			super(x, y, width, height, text.getString(), newLines, null);
			this.onChange = onChange;
			this.base = base;
			this.baseReset = StyleUtil.minus(StyleUtil.RESET_STYLE, base);
			this.styles = new ArrayList<>();
			this.text = text.copy();
			this.undo = new ArrayList<>();
			this.undo.add(text);
			undoPos = 0;
			genStyles(text, base, 0);
			setFormatter(str -> this.text);
			setChangeListener(str -> this.onChange.accept(this.text));
			onEdit("", 0, 0);
			generateLines();
		}
		
		public InternalTextFieldWidget setTextChangeListener(Consumer<Component> onChange) {
			this.onChange = onChange;
			return this;
		}
		
		private void genStyles(Component text, Style parent, int index) {
			int len = TextUtil.getContent(text).length();
			Style style = text.getStyle().applyTo(parent);
			if (len > 0) {
				setStyle(index, style);
				index += len;
			}
			for (Component child : text.getSiblings()) {
				genStyles(child, style, index);
				index += TextUtil.stripInvalidChars(child.getString(), allowsNewLines()).length();
			}
		}
		
		private void setStyle(int index, Style style) {
			while (styles.size() <= index)
				styles.add(styles.size(), null);
			styles.set(index, style);
		}
		
		private Style getStyle(int index) {
			Style output = base;
			for (int i = 0; i <= index && i < styles.size(); i++) {
				Style style = styles.get(i);
				if (style != null)
					output = style;
			}
			return output;
		}
		
		private void applyFormatting(ChatFormatting formatting) {
			int start = getSelStart();
			int end = getSelEnd();
			
			if (start == end) {
				if (cursorStyle == null)
					cursorStyle = getStyle(start == 0 ? 0 : start - 1);
				if (StyleUtil.hasFormatting(cursorStyle, formatting))
					cursorStyle = withoutFormatting(cursorStyle, formatting);
				else
					cursorStyle = withFormatting(cursorStyle, formatting);
				return;
			}
			
			Style startStyle = getStyle(start);
			Style endStyle = getStyle(end);
			
			boolean filled = StyleUtil.hasFormatting(startStyle, formatting);
			setStyle(start, withFormatting(startStyle, formatting));
			for (int i = start + 1; i < end && i < styles.size(); i++) {
				Style style = styles.get(i);
				if (style != null && !StyleUtil.hasFormatting(style, formatting)) {
					styles.set(i, withFormatting(style, formatting));
					filled = false;
				}
			}
			setStyle(end, endStyle);
			
			if (filled) {
				setStyle(start, withoutFormatting(startStyle, formatting));
				for (int i = start + 1; i < end && i < styles.size(); i++) {
					Style style = styles.get(i);
					if (style != null)
						styles.set(i, withoutFormatting(style, formatting));
				}
			}
			
			markUndo();
			onEdit("", start, 0);
			generateLines();
			onChange.accept(text);
		}
		private Style withFormatting(Style style, ChatFormatting formatting) {
			if (formatting == ChatFormatting.RESET)
				return baseReset;
			return style.applyFormat(formatting);
		}
		private Style withoutFormatting(Style style, ChatFormatting formatting) {
			return StyleUtil.minusFormatting(style, StyleUtil.RESET_STYLE.withColor(base.getColor()), formatting);
		}
		
		private void applyStyleChange(UnaryOperator<Style> changer, boolean regenerateLines) {
			int start = getSelStart();
			int end = getSelEnd();
			
			if (start == end) {
				if (cursorStyle == null)
					cursorStyle = getStyle(start == 0 ? 0 : start - 1);
				cursorStyle = changer.apply(cursorStyle);
				return;
			}
			
			Style startStyle = getStyle(start);
			Style endStyle = getStyle(end);
			
			setStyle(start, changer.apply(startStyle));
			for (int i = start + 1; i < end && i < styles.size(); i++) {
				Style style = styles.get(i);
				if (style != null)
					styles.set(i, changer.apply(style));
			}
			setStyle(end, endStyle);
			
			markUndo();
			onEdit("", start, 0);
			if (regenerateLines)
				generateLines();
			onChange.accept(text);
		}
		
		private void applyColor(ChatFormatting color, boolean shadow) {
			if (shadow) {
				int shadowColor = (StyleUtil.scaleRgb(StyleUtil.getColor(color), 0.25) | 0xFF000000);
				applyStyleChange(style -> style.withShadowColor(shadowColor), true);
			} else
				applyFormatting(color);
		}
		
		public void setFormattedText(Component text) {
			styles.clear();
			genStyles(text, base, 0);
			ignoreNextEditStyles = true;
			setText(text.getString());
			ignoreNextEditStyles = false; // If onEdit doesn't get called since text is the same
		}
		public Component getFormattedText() {
			return text;
		}
		@Override
		public void setText(String text) {
			if (ignoreNextSetText) {
				ignoreNextSetText = false;
				return;
			}
			super.setText(text);
		}
		
		private Style getInitialCustomStyle() {
			return (getSelStart() == getSelEnd() ?
					(cursorStyle == null ? getStyle(getSelStart() == 0 ? 0 : getSelStart() - 1) : cursorStyle) :
						getStyle(getSelStart()));
		}
		private void showCustomColor(boolean shadow) {
			Style initialStyle = getInitialCustomStyle();
			int initialColor = (initialStyle.getColor() == null ?
					(base.getColor() == null ? -1 : base.getColor().getValue()) : initialStyle.getColor().getValue());
			if (shadow) {
				int initialShadow = (initialStyle.getShadowColor() == null ?
						(base.getShadowColor() == null ? StyleUtil.scaleRgb(initialColor, 0.25) : base.getShadowColor()) : initialStyle.getShadowColor());
				InputOverlay.show(
						Component.translatableEscape("nbteditor.formatted_text.custom_color.shadow"),
						new ColorSelectorWidget.ColorSelectorInput(initialShadow),
						rgb -> applyStyleChange(style -> style.withShadowColor(rgb | 0xFF000000), true));
			} else {
				InputOverlay.show(
						Component.translatableEscape("nbteditor.formatted_text.custom_color"),
						new ColorSelectorWidget.ColorSelectorInput(initialColor),
						rgb -> applyStyleChange(style -> style.withColor(rgb), true));
			}
		}
		private void showEvents() {
			Style initialStyle = getInitialCustomStyle();
			OverlaySupportingScreen.setOverlayStatic(new EventEditorWidget(
					initialStyle.getClickEvent(), initialStyle.getHoverEvent(),
					(clickEvent, hoverEvent) -> applyStyleChange(
							style -> style.withClickEvent(clickEvent).withHoverEvent(hoverEvent), false)),
					200);
		}
		private void showInsertion() {
			Style initialStyle = getInitialCustomStyle();
			InputOverlay.show(
					Component.translatableEscape("nbteditor.formatted_text.insertion"),
					StringInput.builder()
							.withDefault(initialStyle.getInsertion() == null ? "" : initialStyle.getInsertion())
							.build(),
					insertion -> applyStyleChange(style -> style.withInsertion(insertion.isEmpty() ? null : insertion), false));
		}
		private void showFont() {
			Style initialStyle = getInitialCustomStyle();
			InputOverlay.show(
					Component.translatableEscape("nbteditor.formatted_text.font"),
					StringInput.builder()
							.withDefault(initialStyle.getFont() instanceof FontDescription.Resource f ? f.id().toString() : "")
							.withValidator(font -> font.isEmpty() || Identifier.tryParse(font) != null)
							.withSuggestions((str, cursor) -> {
								SuggestionsBuilder builder = new SuggestionsBuilder(str, 0);
								for (Identifier font : MainUtil.client.fontManager.fontSets.keySet()) {
									String fontStr = font.toString();
									if (fontStr.startsWith(str))
										builder.suggest(fontStr);
								}
								return builder.buildFuture();
							})
							.build(),
					font -> applyStyleChange(style -> style.withFont(font.isEmpty() ? null : new FontDescription.Resource(Identifier.parse(font))), true));
		}
		
		@Override
		protected void renderHighlightsBelow(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
			Style initialStyle = getStyle(0);
			int start = (initialStyle.getClickEvent() != null || initialStyle.getHoverEvent() != null || initialStyle.getInsertion() != null ? 0 : -1);
			for (int i = 0; i < styles.size(); i++) {
				Style style = styles.get(i);
				if (style == null)
					continue;
				if (style.getClickEvent() != null || style.getHoverEvent() != null || style.getInsertion() != null) {
					if (start == -1)
						start = i;
				} else if (start != -1) {
					renderHighlight(context, start, i, 0x55FFAA00);
					start = -1;
				}
			}
			if (start != -1)
				renderHighlight(context, start, getText().length(), 0x55FFAA00);
		}
		
		@Override
		public boolean keyPressed(KeyEvent input) {
			int keyCode = input.key(); int scanCode = input.scancode(); int modifiers = input.modifiers();
			if (super.keyPressed(input))
				return true;
			
			if (Keys.hasControlDown() && !Keys.hasShiftDown()) {
				ChatFormatting formatting = switch (keyCode) {
					case GLFW.GLFW_KEY_B -> ChatFormatting.BOLD;
					case GLFW.GLFW_KEY_I -> ChatFormatting.ITALIC;
					case GLFW.GLFW_KEY_U -> ChatFormatting.UNDERLINE;
					case GLFW.GLFW_KEY_D -> ChatFormatting.STRIKETHROUGH;
					case GLFW.GLFW_KEY_K -> ChatFormatting.OBFUSCATED;
					case GLFW.GLFW_KEY_BACKSLASH -> ChatFormatting.RESET;
					default -> null;
				};
				if (formatting != null) {
					applyFormatting(formatting);
					return true;
				}
			}
			
			if (Keys.hasControlDown() && Keys.hasShiftDown()) {
				switch (keyCode) {
					case GLFW.GLFW_KEY_C -> showCustomColor(hasShadowKeyDown());
					case GLFW.GLFW_KEY_E -> showEvents();
					case GLFW.GLFW_KEY_I -> showInsertion();
					case GLFW.GLFW_KEY_F -> showFont();
				}
			}
			
			return false;
		}
		
		@Override
		protected void onCursorMove(int cursor, int selStart, int selEnd) {
			if (getCursor() == cursor && getSelStart() == selStart && getSelEnd() == selEnd)
				return;
			cursorStyle = null;
		}
		
		@Override
		protected void onEdit(String insertedText, int pos, int overwrittenLen) {
			while (undoPos > 0) {
				undo.remove(0);
				undoPos--;
			}
			
			if (ignoreNextEditStyles)
				ignoreNextEditStyles = false;
			else if (overwrittenLen == 0) {
				Style style = getStyle(pos);
				setStyle(pos, style);
				for (int i = 0; i < insertedText.length(); i++)
					styles.add(pos, null);
				if (cursorStyle != null)
					setStyle(pos, cursorStyle);
				else if (pos == 0)
					setStyle(0, style);
			} else {
				Style startStyle = getStyle(pos);
				Style endStyle = null;
				for (int i = 0; i < overwrittenLen && pos < styles.size(); i++) {
					Style style = styles.remove(pos);
					if (style != null)
						endStyle = style;
				}
				if (endStyle != null && (pos >= styles.size() || styles.get(pos) == null))
					setStyle(pos, endStyle);
				if (pos < styles.size()) {
					for (int i = 0; i < insertedText.length(); i++)
						styles.add(pos, null);
				}
				if (!insertedText.isEmpty())
					setStyle(pos, startStyle);
			}
			
			String afterEdit = new StringBuilder(getText()).replace(pos, pos + overwrittenLen, insertedText).toString();
			MutableComponent text = Component.literal("");
			String part = "";
			Style style = !styles.isEmpty() && styles.get(0) != null ? styles.get(0) : base;
			for (int i = 0; i < afterEdit.length(); i++) {
				Style newStyle = (i == 0 || i >= styles.size() ? null : styles.get(i));
				if (newStyle != null) {
					if (newStyle.equals(style))
						styles.set(i, null);
					else {
						text.append(Component.literal(part).setStyle(style));
						part = "";
						style = newStyle;
					}
				}
				part += afterEdit.charAt(i);
			}
			if (!part.isEmpty())
				text.append(Component.literal(part).setStyle(style));
			undo.add(0, text);
			this.text = text;
			
			while (styles.size() > afterEdit.length())
				styles.remove(afterEdit.length());
		}
		
		@Override
		protected void onUndo(String newText) {
			if (undoPos < undo.size() - 1) {
				text = undo.get(++undoPos);
				styles.clear();
				genStyles(text, Style.EMPTY, 0);
			}
		}
		
		@Override
		protected void onRedo(String newText) {
			if (undoPos > 0) {
				text = undo.get(--undoPos);
				styles.clear();
				genStyles(text, Style.EMPTY, 0);
			}
		}
		
		@Override
		protected void onUndoDiscard() {
			while (undoPos > 0) {
				undo.remove(0);
				undoPos--;
			}
			undo.remove(0);
		}
		
		@Override
		protected String onCopy(String text, int pos, int len) {
			return TextInst.toString(TextUtil.substring(this.text, pos, pos + len));
		}
		
		@Override
		protected String onPaste(String text, int pos, int overwrittenLen) {
			try {
				Component textValue = pasteFilter(TextUtil.fromStringSafely(text, true));
				String textValueStr = textValue.getString();
				int textLen = textValueStr.length();
				
				Style endStyle = getStyle(pos);
				for (int i = 0; i < overwrittenLen && pos < styles.size(); i++) {
					Style style = styles.remove(pos);
					if (style != null)
						endStyle = style;
				}
				if (endStyle != null && (pos >= styles.size() || styles.get(pos) == null))
					setStyle(pos, endStyle);
				if (pos < styles.size()) {
					for (int i = 0; i < textLen; i++)
						styles.add(pos, null);
				}
				
				genStyles(textValue, Style.EMPTY, pos);
				ignoreNextEditStyles = true;
				
				return textValueStr;
			} catch (Exception e) {
				return text;
			}
		}
		private Component pasteFilter(Component toPaste) {
			toPaste = TextUtil.stripInvalidChars(toPaste, allowsNewLines());
			int numNewLines = getNumNewLines(getText());
			int toPasteNewLines = getNumNewLines(toPaste);
			while (numNewLines + toPasteNewLines + 1 > maxLines) {
				int i = TextUtil.lastIndexOf(toPaste, '\n');
				if (i == -1)
					break;
				toPaste = TextUtil.deleteCharAt(toPaste, i);
				toPasteNewLines--;
			}
			return toPaste;
		}
		private int getNumNewLines(Component text) {
			AtomicInteger output = new AtomicInteger(0);
			text.visit(str -> {
				int numNewLines = getNumNewLines(str);
				if (numNewLines != 0)
					output.setPlain(output.getPlain() + numNewLines);
				return Optional.empty();
			});
			return output.getPlain();
		}
	}
	
	public static FormattedTextFieldWidget create(FormattedTextFieldWidget prev, int x, int y, int width, int height,
			Component text, boolean newLines, Style base, Consumer<Component> onChange) {
		if (prev == null)
			return new FormattedTextFieldWidget(x, y, width, height, text, newLines, base, onChange);
		prev.x = x;
		prev.y = y;
		prev.width = width;
		prev.height = height;
		prev.field = InternalTextFieldWidget.create(prev.field, x, y + 24, width, height - 24, text, newLines, base, onChange);
		prev.clearWidgets();
		prev.init();
		prev.addElement(prev.field);
		prev.addTickable(prev.field);
		prev.setText(text);
		prev.setChangeListener(onChange);
		if (prev.isMultiFocused())
			prev.setMultiFocused(false);
		return prev;
	}
	public static FormattedTextFieldWidget create(FormattedTextFieldWidget prev, int x, int y, int width, int height,
			List<Component> lines, Style base, Consumer<List<Component>> onChange) {
		return create(prev, x, y, width, height, TextUtil.joinLines(lines), true, base, text -> onChange.accept(TextUtil.splitText(text)));
	}
	
	private static boolean hasShadowKeyDown() {
		return StyleUtil.SHADOW_COLOR_EXISTS && Keys.hasAltDown();
	}
	
	private int x;
	private int y;
	private int width;
	private int height;
	private InternalTextFieldWidget field;
	private ButtonDropdownWidget colors;
	private Button font;
	private int lastFont;
	private long lastFontChange;
	
	protected FormattedTextFieldWidget(int x, int y, int width, int height, Component text, boolean newLines, Style base, Consumer<Component> onChange) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		this.field = InternalTextFieldWidget.create(null, x, y + 24, width, height - 24, text, newLines, base, onChange);
		init();
		addElement(field);
		addTickable(field);
	}
	private void init() {
		if (width < 16 * 20 + (ConfigScreen.isHideFormatButtons() ? 0 : 20 + 4 + 5 * 20 + (4 + 20) * 2)) {
			colors = addElement(new ButtonDropdownWidget(x, y, 20, 20, Component.literal("⬛").withStyle(ChatFormatting.AQUA), 20, 20));
			for (ChatFormatting formatting : ChatFormatting.values()) {
				if (!StyleUtil.isColor(formatting))
					break;
				colors.addButton(Component.literal("⬛").withStyle(formatting), btn -> {
					field.applyColor(formatting, hasShadowKeyDown());
					colors.setOpen(false);
				}, createColorButtonTooltip(formatting));
			}
			colors.build();
		} else {
			colors = null;
			int i = 0;
			for (ChatFormatting formatting : ChatFormatting.values()) {
				if (!StyleUtil.isColor(formatting))
					break;
				addWidget(Buttons.of(x + i * 20, y, 20, 20, Component.literal("⬛").withStyle(formatting),
						btn -> field.applyColor(formatting, hasShadowKeyDown()), createColorButtonTooltip(formatting)));
				i++;
			}
		}
		
		if (!ConfigScreen.isHideFormatButtons()) {
			int afterColorsX = x + (colors == null ? 16 * 20 : 20);
			
			int i = 0;
			for (ChatFormatting formatting : new ChatFormatting[] {ChatFormatting.BOLD, ChatFormatting.ITALIC, ChatFormatting.UNDERLINE,
					ChatFormatting.STRIKETHROUGH, ChatFormatting.OBFUSCATED, ChatFormatting.RESET}) {
				Component btnText;
				MVTooltip btnTooltip;
				if (formatting == ChatFormatting.RESET) {
					btnText = Component.nullToEmpty("");
					if (ConfigScreen.isKeybindsHidden())
						btnTooltip = new MVTooltip(Component.nullToEmpty(StyleUtil.getName(formatting)));
					else {
						btnTooltip = new MVTooltip(
								Component.nullToEmpty(StyleUtil.getName(formatting)),
								Component.translatableEscape("nbteditor.keybind.formatted_text.reset"));
					}
				} else {
					btnText = Component.literal(formatting.name().substring(0, 1)).withStyle(formatting);
					btnTooltip = new MVTooltip(Component.nullToEmpty(StyleUtil.getName(formatting)));
				}
				addWidget(Buttons.of(
						afterColorsX + 24 + i * 20 + (formatting == ChatFormatting.RESET ? 4 + 20 * 3 + 4 : 0), y, 20, 20,
						btnText, btn -> field.applyFormatting(formatting), btnTooltip));
				i++;
			}
			
			addWidget(Buttons.of(afterColorsX, y, 20, 20,
					Component.literal("⬛").setStyle(Style.EMPTY.withColor(0x9999C0).applyFormat(ChatFormatting.ITALIC)),
					btn -> field.showCustomColor(hasShadowKeyDown()),
					createFormatButtonTooltip("custom_color", true)));
			
			addWidget(Buttons.of(afterColorsX + 24 + 5 * 20 + 4, y, 20, 20,
					Component.literal("E"),
					btn -> field.showEvents(),
					createFormatButtonTooltip("events", false)));
			addWidget(Buttons.of(afterColorsX + 24 + 5 * 20 + 4 + 20, y, 20, 20,
					Component.literal("I"),
					btn -> field.showInsertion(),
					createFormatButtonTooltip("insertion", false)));
			font = addWidget(Buttons.of(afterColorsX + 24 + 5 * 20 + 4 + 20 * 2, y, 20, 20,
					Component.literal("F"), // Gets replaced before rendering
					btn -> field.showFont(),
					createFormatButtonTooltip("font", false)));
		}
	}
	private MVTooltip createColorButtonTooltip(ChatFormatting color) {
		Component name = Component.nullToEmpty(StyleUtil.getName(color));
		if (ConfigScreen.isKeybindsHidden() || !StyleUtil.SHADOW_COLOR_EXISTS)
			return new MVTooltip(name);
		return new MVTooltip(name, Component.translatableEscape("nbteditor.keybind.formatted_text.shadow"));
	}
	private MVTooltip createFormatButtonTooltip(String name, boolean color) {
		String nameKey = "nbteditor.formatted_text." + name;
		if (ConfigScreen.isKeybindsHidden())
			return new MVTooltip(nameKey);
		String keybindKey = "nbteditor.keybind.formatted_text." + name;
		if (color && StyleUtil.SHADOW_COLOR_EXISTS)
			return new MVTooltip(nameKey, keybindKey, "nbteditor.keybind.formatted_text.shadow");
		return new MVTooltip(nameKey, keybindKey);
	}
	
	public FormattedTextFieldWidget setChangeListener(Consumer<Component> onChange) {
		field.setTextChangeListener(onChange);
		return this;
	}
	public FormattedTextFieldWidget setMaxLines(int maxLines) {
		field.setMaxLines(maxLines);
		return this;
	}
	public FormattedTextFieldWidget setBackgroundColor(int bgColor) {
		field.setBackgroundColor(bgColor);
		return this;
	}
	public FormattedTextFieldWidget setCursorColor(int cursorColor) {
		field.setCursorColor(cursorColor);
		return this;
	}
	public FormattedTextFieldWidget setSelectionColor(int selColor) {
		field.setSelectionColor(selColor);
		return this;
	}
	public FormattedTextFieldWidget setShadow(boolean shadow) {
		field.setShadow(shadow);
		return this;
	}
	public FormattedTextFieldWidget setOverscroll(boolean overscroll) {
		field.setOverscroll(overscroll);
		return this;
	}
	
	public FormattedTextFieldWidget suggest(Screen screen, BiFunction<String, Integer, CompletableFuture<Suggestions>> suggestions) {
		field.suggest(screen, suggestions);
		return this;
	}
	
	public int getX() {
		return x;
	}
	public int getY() {
		return y;
	}
	public int getWidth() {
		return width;
	}
	public int getHeight() {
		return height;
	}
	
	public void setText(Component text) {
		field.setFormattedText(text);
	}
	public void setText(List<Component> lines) {
		setText(TextUtil.joinLines(lines));
	}
	public Component getText() {
		return field.getFormattedText();
	}
	
	public List<Component> getTextLines() {
		return TextUtil.splitText(getText());
	}
	
	@Override
	public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		setFocused(isMultiFocused() ? field : null);
		field.extractRenderState(context, mouseX, mouseY, delta);
		
		if (colors != null) {
			context.pose().pushMatrix();
			context.pose().translate((float) (0.0), (float) (0.0));
			colors.extractRenderState(context, mouseX, mouseY, delta);
			context.pose().popMatrix();
		}
		
		if (font != null) {
			long time = System.currentTimeMillis();
			if (lastFontChange < time - 200) {
				lastFontChange = time;
				lastFont += Math.floor(Math.random() * 2) + 1;
				font.setMessage(Component.literal(lastFont % 3 + "")
						.withStyle(style -> style.withFont(new FontDescription.Resource(Identifier.fromNamespaceAndPath("nbteditor", "fancy_f")))));
			}
		}
		
		super.extractRenderState(context, mouseX, mouseY, delta);
	}
	
	@Override
	public boolean isMouseOver(double mouseX, double mouseY) {
		return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
	}
	
}
