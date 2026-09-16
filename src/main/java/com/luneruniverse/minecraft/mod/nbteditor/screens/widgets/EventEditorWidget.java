package com.luneruniverse.minecraft.mod.nbteditor.screens.widgets;

import org.lwjgl.glfw.GLFW;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVDrawableHelper;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVTextEvents;
import com.luneruniverse.minecraft.mod.nbteditor.screens.OverlaySupportingScreen;
import com.luneruniverse.minecraft.mod.nbteditor.screens.configurable.ConfigValueDropdown;
import com.luneruniverse.minecraft.mod.nbteditor.screens.widgets.Buttons;
import net.minecraft.network.chat.Component;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.client.Minecraft;

/**
 * The overlay for editing the click and hover events of a run of formatted text, opened from the
 * style menu of a {@link FormattedTextFieldWidget}.
 *
 * <p>The two enums are the editor's own vocabulary, not the game's: each names an action a player
 * can pick from a dropdown, including the NONE that clears the event, and maps it onto the
 * {@link MVTextEvents} action that carries it.
 */
class EventEditorWidget extends GroupWidget implements InitializableOverlay<Screen> {
	public enum ClickAction {
		NONE(null),
		OPEN_URL(MVTextEvents.ClickAction.OPEN_URL),
		RUN_COMMAND(MVTextEvents.ClickAction.RUN_COMMAND),
		SUGGEST_COMMAND(MVTextEvents.ClickAction.SUGGEST_COMMAND),
		CHANGE_PAGE(MVTextEvents.ClickAction.CHANGE_PAGE),
		COPY_TO_CLIPBOARD(MVTextEvents.ClickAction.COPY_TO_CLIPBOARD);
		
		public static ClickAction get(MVTextEvents.ClickAction<?> value) {
			for (ClickAction action : values()) {
				if (action.value == value)
					return action;
			}
			if (value == MVTextEvents.ClickAction.OPEN_FILE)
				return NONE;
			throw new IllegalArgumentException("Invalid ClickAction: " + value);
		}
		
		public final MVTextEvents.ClickAction<?> value;
		private ClickAction(MVTextEvents.ClickAction<?> value) {
			this.value = value;
		}
		
		@Override
		public String toString() {
			if (this == NONE)
				return "none";
			return value.getName();
		}
	}
	public enum HoverAction {
		NONE(null),
		SHOW_TEXT(MVTextEvents.HoverAction.SHOW_TEXT),
		SHOW_ITEM(MVTextEvents.HoverAction.SHOW_ITEM),
		SHOW_ENTITY(MVTextEvents.HoverAction.SHOW_ENTITY);
		
		public static HoverAction get(MVTextEvents.HoverAction<?> value) {
			for (HoverAction action : values()) {
				if (action.value == value)
					return action;
			}
			throw new IllegalArgumentException("Invalid HoverAction: " + value);
		}
		
		public final MVTextEvents.HoverAction<?> value;
		private HoverAction(MVTextEvents.HoverAction<?> value) {
			this.value = value;
		}
		
		@Override
		public String toString() {
			if (this == NONE)
				return "none";
			return value.getName();
		}
	}
	public interface EventPairCallback {
		public void onEventChange(ClickEvent clickEvent, HoverEvent hoverEvent);
	}
	
	private int x;
	private int y;
	private final ConfigValueDropdown<ClickAction> clickActionDropdown;
	private final TranslatedGroupWidget clickActionField;
	private final NamedTextFieldWidget clickValueField;
	private final ConfigValueDropdown<HoverAction> hoverActionDropdown;
	private final TranslatedGroupWidget hoverActionField;
	private final NamedTextFieldWidget hoverValueField;
	private final Button ok;
	private final Button cancel;
	
	public EventEditorWidget(ClickEvent clickEvent, HoverEvent hoverEvent, EventPairCallback onDone) {
		MVTextEvents.ClickAction<?> clickAction = (clickEvent == null ? null : MVTextEvents.ClickAction.getAction(clickEvent));
		String clickValue = (clickAction == null ? "" : clickAction.getStringifiedValue(clickEvent));
		MVTextEvents.HoverAction<?> hoverAction = (hoverEvent == null ? null : MVTextEvents.HoverAction.getAction(hoverEvent));
		String hoverValue = (hoverEvent == null ? "" : hoverAction.getStringifiedValue(hoverEvent));
		
		clickActionDropdown = ConfigValueDropdown.forEnum(ClickAction.get(clickAction), ClickAction.NONE, ClickAction.class);
		clickActionDropdown.setWidth(150);
		clickActionDropdown.addValueListener(value -> updateOk());
		clickActionField = addElement(TranslatedGroupWidget.forWidget(clickActionDropdown, 0, 0, 0));
		clickValueField = addWidget(new NamedTextFieldWidget(0, 0, 150, 16))
				.name(Component.translatableEscape("nbteditor.formatted_text.click_event_value"));
		clickValueField.setMaxLength(Integer.MAX_VALUE);
		clickValueField.setValue(clickValue);
		clickValueField.setResponder(str -> updateOk());
		
		hoverActionDropdown = ConfigValueDropdown.forEnum(HoverAction.get(hoverAction), HoverAction.NONE, HoverAction.class);
		hoverActionDropdown.setWidth(150);
		hoverActionDropdown.addValueListener(value -> updateOk());
		hoverActionField = addElement(TranslatedGroupWidget.forWidget(hoverActionDropdown, 0, 0, 0));
		hoverValueField = addWidget(new NamedTextFieldWidget(0, 0, 150, 16))
				.name(Component.translatableEscape("nbteditor.formatted_text.hover_event_value"));
		hoverValueField.setMaxLength(Integer.MAX_VALUE);
		hoverValueField.setValue(hoverValue);
		hoverValueField.setResponder(str -> updateOk());
		
		ok = addWidget(Buttons.of(0, 0, 150, 20, Component.translatableEscape("nbteditor.ok"), btn -> {
			onDone.onEventChange(
					clickActionDropdown.getValidValue() == ClickAction.NONE ? null :
						clickActionDropdown.getValidValue().value.newEventParse(clickValueField.getValue()).get(),
					hoverActionDropdown.getValidValue() == HoverAction.NONE ? null :
						hoverActionDropdown.getValidValue().value.newEventParse(hoverValueField.getValue()).get());
			OverlaySupportingScreen.setOverlayStatic(null);
		}));
		cancel = addWidget(Buttons.of(0, 0, 150, 20, Component.translatableEscape("nbteditor.cancel"), btn -> {
			OverlaySupportingScreen.setOverlayStatic(null);
		}));
		
		addDrawable(hoverActionField);
		addDrawable(clickActionField);
		
		updateOk();
	}
	
	@Override
	public void init(Screen parent, int width, int height) {
		x = width / 2;
		y = height / 2;
		
		clickActionField.setTranslation(x - 152, y - 34, 0);
		clickValueField.x = x + 2;
		clickValueField.y = y - 32;
		
		hoverActionField.setTranslation(x - 152, y - 12, 0);
		hoverValueField.x = x + 2;
		hoverValueField.y = y - 10;
		
		ok.x = x - 152;
		ok.y = y + 12;
		
		cancel.x = x + 2;
		cancel.y = y + 12;
	}
	
	private void updateOk() {
		ok.active = (clickActionDropdown.getValidValue() == ClickAction.NONE ||
				clickActionDropdown.getValidValue().value.newEventParse(clickValueField.getValue()).isPresent()) &&
				(hoverActionDropdown.getValidValue() == HoverAction.NONE ||
				hoverActionDropdown.getValidValue().value.newEventParse(hoverValueField.getValue()).isPresent());
	}
	
	@Override
	public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		MVDrawableHelper.renderBackground(Minecraft.getInstance().gui.screen(), context);
		MVDrawableHelper.drawCenteredTextWithShadow(context, Minecraft.getInstance().font,
				Component.translatableEscape("nbteditor.formatted_text.events"),
				x, y - 38 - Minecraft.getInstance().font.lineHeight, -1);
		super.extractRenderState(context, mouseX, mouseY, delta);
	}
	
	@Override
	public boolean keyPressed(KeyEvent input) {
		int keyCode = input.key(); int scanCode = input.scancode(); int modifiers = input.modifiers();
		if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
			OverlaySupportingScreen.setOverlayStatic(null);
			return true;
		}
		if (keyCode == GLFW.GLFW_KEY_ENTER) {
			if (ok.active)
				ok.onPress(input);
			return true;
		}
		
		return super.keyPressed(input);
	}
}
