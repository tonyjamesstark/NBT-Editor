package com.luneruniverse.minecraft.mod.nbteditor.screens;

import java.util.function.Function;

import org.lwjgl.glfw.GLFW;

import com.luneruniverse.minecraft.mod.nbteditor.commands.get.GetLostItemCommand;
import com.luneruniverse.minecraft.mod.nbteditor.localnbt.LocalNBT;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVDrawableHelper;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVMisc;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVTooltip;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.TextInst;
import com.luneruniverse.minecraft.mod.nbteditor.nbtreferences.NBTReference;
import com.luneruniverse.minecraft.mod.nbteditor.nbtreferences.itemreferences.ItemReference;
import com.luneruniverse.minecraft.mod.nbteditor.screens.factories.LocalFactoryScreen;
import com.luneruniverse.minecraft.mod.nbteditor.screens.util.FancyConfirmScreen;
import com.luneruniverse.minecraft.mod.nbteditor.screens.widgets.AlertWidget;
import com.luneruniverse.minecraft.mod.nbteditor.screens.widgets.NamedTextFieldWidget;
import com.luneruniverse.minecraft.mod.nbteditor.util.MainUtil;

import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public abstract class LocalEditorScreen<L extends LocalNBT> extends OverlaySupportingScreen {
	
	protected static record FactoryLink<L extends LocalNBT>(String langName, Function<NBTReference<L>, Screen> factory) {
		public FactoryLink(String langName, Function<NBTReference<L>, Screen> factory) {
			this.langName = langName;
			this.factory = factory;
		}
	}
	
	protected final NBTReference<L> ref;
	protected L localNBT;
	protected L savedLocalNBT;
	private boolean saved;
	
	protected NamedTextFieldWidget name;
	private Button saveBtn;
	
	protected LocalEditorScreen(Component title, NBTReference<L> ref) {
		super(title);
		this.ref = ref;
		this.savedLocalNBT = LocalNBT.copy(ref.getLocalNBT());
		this.localNBT = LocalNBT.copy(savedLocalNBT);
		this.saved = true;
	}
	
	protected boolean isNameEditable() {
		return false;
	}
	
	protected boolean isSaveRequried() {
		return true;
	}
	
	protected FactoryLink<L> getFactoryLink() {
		return new FactoryLink<>("nbteditor.factory", LocalFactoryScreen::new);
	}
	
	
	@Override
	protected final void init() {
		super.init();
		
		name = new NamedTextFieldWidget(16 + (32 + 8) * 2, 16 + 8, 100, 16) {
			@Override
			public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
				double mouseX = click.x(); double mouseY = click.y(); int button = click.button();
				if (isNameEditable())
					return super.mouseClicked(click, doubled);
				else
					return false;
			}
		}.name(TextInst.translatable("nbteditor.editor.name"));
		name.setMaxLength(Integer.MAX_VALUE);
		name.setValue(localNBT.getName().getString());
		name.setEditable(isNameEditable());
		addRenderableWidget(name);
		
		if (isSaveRequried()) {
			saveBtn = addRenderableWidget(MVMisc.newButton(16 + (32 + 8) * 2 + 100 + 8, 16 + 6, 100, 20, TextInst.translatable("nbteditor.editor.save"), btn -> {
				save();
			}));
			saveBtn.active = !saved;
		}
		
		FactoryLink<L> link = getFactoryLink();
		if (link != null) {
			addRenderableWidget(MVMisc.newTexturedButton(width - 36, 22, 20, 20, 20,
					LocalFactoryScreen.FACTORY_ICON,
					btn -> closeSafely(() -> minecraft.setScreenAndShow(link.factory().apply(ItemReference.toItemStackRef(ref)))),
					new MVTooltip(link.langName())));
		}
		
		initEditor();
	}
	protected void initEditor() {}
	
	@Override
	public final void renderMain(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		MVDrawableHelper.renderBackground(this, context);
		preRenderEditor(context, mouseX, mouseY, delta);
		super.renderMain(context, mouseX, mouseY, delta);
		renderEditor(context, mouseX, mouseY, delta);
		MainUtil.renderLogo(context);
		renderPreview(context, delta);
	}
	private void renderPreview(GuiGraphicsExtractor context, float tickDelta) {
		int scaleX = 2;
		int scaleY = 2;
		int x = (16 + 32 + 8) / scaleX;
		int y = 16 / scaleY;
		
		context.pose().pushMatrix();
		context.pose().scale((float) (scaleX), (float) (scaleY));
		localNBT.renderIcon(context, x, y, tickDelta);
		context.pose().popMatrix();
	}
	protected void preRenderEditor(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {}
	protected void renderEditor(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {}
	
	protected void renderTip(GuiGraphicsExtractor context, String langHint) {
		if (!ConfigScreen.isKeybindsHidden()) {
			int x = 16 + (32 + 8) * 2 + (100 + 8) * 2;
			MainUtil.drawWrappingString(context, font, TextInst.translatable(langHint).getString(),
					16 + (32 + 8) * 2 + (100 + 8) * 2, 16 + 6 + 10, width - x - 8 - 20 - 8, -1, false, true);
		}
	}
	
	@Override
	public boolean keyPressed(KeyEvent input) {
		int keyCode = input.key(); int scanCode = input.scancode(); int modifiers = input.modifiers();
		if (getOverlay() != null)
			return super.keyPressed(input);
		if (super.keyPressed(input))
			return true;
		
		if (MVMisc.hasControlDown() && !MVMisc.hasShiftDown() && !MVMisc.hasAltDown() && keyCode == GLFW.GLFW_KEY_S) {
			save();
			return true;
		}
		
		return name.keyPressed(input);
	}
	
	protected void setSaved(boolean saved) {
		this.saved = saved;
		if (saveBtn != null)
			saveBtn.active = !saved;
	}
	public boolean isSaved() {
		return saved;
	}
	protected boolean save() {
		if (ref.exists()) {
			savedLocalNBT = LocalNBT.copy(localNBT);
			saveBtn.setMessage(TextInst.translatable("nbteditor.editor.saving"));
			setSaved(true);
			ref.saveLocalNBT(savedLocalNBT, () -> {
				saveBtn.setMessage(TextInst.translatable("nbteditor.editor.save"));
			});
		} else {
			localNBT.toItem(false).ifPresentOrElse(item -> {
				savedLocalNBT = LocalNBT.copy(localNBT);
				GetLostItemCommand.loseItem(item);
				setSaved(true);
				saveBtn.setMessage(TextInst.translatable("nbteditor.editor.save"));
			}, () -> setOverlay(new AlertWidget(() -> setOverlay(null), TextInst.translatable("nbteditor.editor.ref_broken")), 500));
		}
		return true;
	}
	protected void checkSave() {
		localNBT.getOrCreateNBT(); // Make sure both items have NBT defined, so no NBT and empty NBT comes out equal
		savedLocalNBT.getOrCreateNBT();
		setSaved(localNBT.equals(savedLocalNBT));
	}
	
	@Override
	public void onClose() {
		closeSafely(ref::showParent);
	}
	
	protected void closeSafely(Runnable onClose) {
		if (saved)
			onClose.run();
		else {
			minecraft.setScreenAndShow(new FancyConfirmScreen(value -> {
				if (!value || save())
					onClose.run();
			}, TextInst.translatable("nbteditor.editor.unsaved.title"), TextInst.translatable("nbteditor.editor.unsaved.desc"),
					TextInst.translatable("nbteditor.editor.unsaved.yes"), TextInst.translatable("nbteditor.editor.unsaved.no")));
		}
	}
	
}
