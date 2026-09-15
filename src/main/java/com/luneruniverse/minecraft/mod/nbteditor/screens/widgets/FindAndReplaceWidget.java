package com.luneruniverse.minecraft.mod.nbteditor.screens.widgets;

import org.lwjgl.glfw.GLFW;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVDrawableHelper;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVTooltip;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.TextInst;
import com.luneruniverse.minecraft.mod.nbteditor.screens.OverlaySupportingScreen;
import com.luneruniverse.minecraft.mod.nbteditor.util.Keys;
import com.luneruniverse.minecraft.mod.nbteditor.util.MainUtil;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.KeyEvent;

/**
 * The find-and-replace overlay of a {@link MultiLineTextFieldWidget}, opened with ctrl+F.
 *
 * <p>Holds only the dialog. The searching and editing it drives belong to the text field and stay
 * there; what is kept here is the query, the replacement and the regex toggle, which are static so
 * that they survive closing the overlay and reopening it on another field.
 */
class FindAndReplaceWidget extends TranslatedGroupWidget {
	private static String findValue = "";
	private static String replaceValue = "";
	private static boolean regex = false;
	
	private final NamedTextFieldWidget find;
	private final NamedTextFieldWidget replace;
	private final Button regexBtn;
	private final MultiLineTextFieldWidget field;
	private boolean dragging;
	
	public FindAndReplaceWidget(MultiLineTextFieldWidget field) {
		super(MainUtil.client.getWindow().getGuiScaledWidth() / 2 - 100,
				MainUtil.client.getWindow().getGuiScaledHeight() / 2 - 30, 200);
		this.field = field;
		find = addWidget(new NamedTextFieldWidget(0, 0, 176, 16)
				.name(TextInst.translatable("nbteditor.multi_line_text.find")));
		replace = addWidget(new NamedTextFieldWidget(0, 20, 200, 16)
				.name(TextInst.translatable("nbteditor.multi_line_text.replace")));
		regexBtn = addWidget(Buttons.of(180, -2, 20, 20,
				TextInst.translatable("nbteditor.multi_line_text.regex." + (regex ? "on" : "off")), btn -> {
			regex = !regex;
			btn.setMessage(TextInst.translatable("nbteditor.multi_line_text.regex." + (regex ? "on" : "off")));
		}, new MVTooltip("nbteditor.multi_line_text.regex")));
		addWidget(Buttons.of(0, 40, 40, 20, TextInst.translatable("nbteditor.multi_line_text.find"), btn -> {
			field.findNext(findValue, regex, Keys.hasShiftDown(), true);
		}));
		addWidget(Buttons.of(44, 40, 64, 20, TextInst.translatable("nbteditor.multi_line_text.replace"), btn -> {
			if (field.findNext(findValue, regex, Keys.hasShiftDown(), true))
				field.replaceSelection(replaceValue, regex);
		}));
		addWidget(Buttons.of(112, 40, 64, 20, TextInst.translatable("nbteditor.multi_line_text.replace_all"), btn -> {
			field.replaceAll(findValue, replaceValue, regex);
		}));
		addWidget(Buttons.of(180, 40, 20, 20, TextInst.translatable("nbteditor.multi_line_text.x"), btn -> {
			OverlaySupportingScreen.setOverlayStatic(null);
		}));
		
		if (field.getSelStart() != field.getSelEnd())
			findValue = field.getSelectedText();
		find.setMaxLength(Integer.MAX_VALUE);
		find.setValue(findValue);
		find.setResponder(str -> findValue = str);
		setFocused(find);
		
		replace.setMaxLength(Integer.MAX_VALUE);
		replace.setValue(replaceValue);
		replace.setResponder(str -> replaceValue = str);
	}
	
	@Override
	public void renderPre(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		MVDrawableHelper.fill(context, -16, -16, 216, 76, 0xC8101010);
	}
	
	@Override
	protected boolean mouseClickedPre(double mouseX, double mouseY, int button) {
		if (isMouseOver(mouseX, mouseY) && !(mouseX >= 0 && mouseX <= 200 && mouseY >= 0 && mouseY <= 60))
			dragging = true;
		return false;
	}
	@Override
	protected boolean mouseReleasedPre(double mouseX, double mouseY, int button) {
		dragging = false;
		return false;
	}
	@Override
	public boolean mouseDraggedPre(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		if (dragging)
			addTranslation(deltaX, deltaY, 0);
		return false;
	}
	
	@Override
	public boolean keyPressed(KeyEvent input) {
		int keyCode = input.key(); int scanCode = input.scancode(); int modifiers = input.modifiers();
		if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
			OverlaySupportingScreen.setOverlayStatic(null);
			return true;
		}
		if (keyCode == GLFW.GLFW_KEY_ENTER) {
			field.findNext(findValue, regex, Keys.hasShiftDown(), true);
			return true;
		}
		if (keyCode == GLFW.GLFW_KEY_TAB) {
			if (getFocused() == find)
				setFocused(replace);
			else
				setFocused(find);
			return true;
		}
		if (keyCode == GLFW.GLFW_KEY_R && Keys.hasControlDown() && !Keys.hasShiftDown() && !Keys.hasAltDown()) {
			regex = !regex;
			regexBtn.setMessage(TextInst.translatable("nbteditor.multi_line_text.regex." + (regex ? "on" : "off")));
			return true;
		}
		
		return super.keyPressed(input);
	}
	
	@Override
	public boolean isMouseOver(double mouseX, double mouseY) {
		return mouseX >= -16 && mouseX <= 216 && mouseY >= -16 && mouseY <= 76;
	}
}
