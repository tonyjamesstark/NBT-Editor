package com.luneruniverse.minecraft.mod.nbteditor.screens;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.lwjgl.glfw.GLFW;

import com.luneruniverse.minecraft.mod.nbteditor.NBTEditorClient;
import com.luneruniverse.minecraft.mod.nbteditor.clientchest.ClientChest;
import com.luneruniverse.minecraft.mod.nbteditor.clientchest.ClientChestHelper;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.DataVersionStatus;
import com.luneruniverse.minecraft.mod.nbteditor.util.Drawing;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVTooltip;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.Version;
import com.luneruniverse.minecraft.mod.nbteditor.screens.containers.ClientChestScreen;
import com.luneruniverse.minecraft.mod.nbteditor.screens.util.FancyConfirmScreen;
import com.luneruniverse.minecraft.mod.nbteditor.screens.widgets.NamedTextFieldWidget;

import com.luneruniverse.minecraft.mod.nbteditor.screens.widgets.Buttons;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;

public class ClientChestDataVersionScreen extends TickableSupportingScreen {
	
	private final DataVersionStatus dataVersionStatus;
	private final Component msg;
	private NamedTextFieldWidget dataVersion;
	private Button updatePageBtn;
	
	public ClientChestDataVersionScreen(Optional<Integer> dataVersion) {
		super(Component.nullToEmpty("Client Chest DataVersion"));
		DataVersionStatus dataVersionStatus = DataVersionStatus.of(dataVersion);
		
		this.dataVersionStatus = dataVersionStatus;
		this.msg = Component.translatableEscape(
				"nbteditor.client_chest.data_version." + switch (dataVersionStatus) {
					case UNKNOWN -> "unknown";
					case OUTDATED -> "old";
					case TOO_UPDATED -> "new";
					default -> throw new IllegalArgumentException("Unexpected DataVersionStatus: " + dataVersionStatus);
				},
				ClientChestScreen.PAGE + 1,
				dataVersion.flatMap(Version::getMCVersion).or(() -> dataVersion.map(value -> value.toString())).orElse(""));
	}
	
	@Override
	protected void init() {
		MutableComponent prevKeybind = Component.translatableEscape("nbteditor.keybind.page.up");
		MutableComponent nextKeybind = Component.translatableEscape("nbteditor.keybind.page.down");
		if (ConfigScreen.isInvertedPageKeybinds()) {
			MutableComponent temp = prevKeybind;
			prevKeybind = nextKeybind;
			nextKeybind = temp;
		}
		
		int dontUpdatePageX = width / 2 + (dataVersionStatus == DataVersionStatus.TOO_UPDATED ? -50 : 58);
		
		addRenderableWidget(Buttons.of(dontUpdatePageX, height / 2 - 34, 52, 20,
				Component.translatableEscape("nbteditor.client_chest.data_version.dont_update_page"), btn -> onClose()));
		addRenderableWidget(Buttons.of(dontUpdatePageX + 56, height / 2 - 34, 20, 20, Component.nullToEmpty("<"), btn -> prevPage(),
				ConfigScreen.isKeybindsHidden() ? null : new MVTooltip(Component.literal("")
						.append(prevKeybind).append(Component.translatableEscape("nbteditor.keybind.page.prev")))))
				.active = ClientChestScreen.PAGE > 0;
		addRenderableWidget(Buttons.of(dontUpdatePageX + 80, height / 2 - 34, 20, 20, Component.nullToEmpty(">"), btn -> nextPage(),
				ConfigScreen.isKeybindsHidden() ? null : new MVTooltip(Component.literal("")
						.append(nextKeybind).append(Component.translatableEscape("nbteditor.keybind.page.next")))))
				.active = ClientChestScreen.PAGE < NBTEditorClient.CLIENT_CHEST.getPageCount() - 1;
		
		addRenderableWidget(Buttons.of(dontUpdatePageX, height / 2 - 10, 100, 20,
				Component.translatableEscape("nbteditor.client_chest.reload_page"), btn -> {
			LoadingScreen.show(ClientChestHelper.reloadPage(ClientChestScreen.PAGE), pageData -> ClientChestScreen.show());
		}));
		addRenderableWidget(Buttons.of(dontUpdatePageX, height / 2 + 14, 100, 20,
				Component.translatableEscape("nbteditor.client_chest.clear_page"), btn -> {
			minecraft.setScreenAndShow(new FancyConfirmScreen(value -> {
				if (value) {
					LoadingScreen.show(ClientChestHelper.discardPage(ClientChestScreen.PAGE), success -> ClientChestScreen.show());
					return;
				}
				
				minecraft.setScreenAndShow(this);
			}, Component.translatableEscape("nbteditor.client_chest.clear_page.title"), Component.translatableEscape("nbteditor.client_chest.clear_page.desc"),
					Component.translatableEscape("nbteditor.client_chest.clear_page.yes"), Component.translatableEscape("nbteditor.client_chest.clear_page.no")));
		}));
		
		if (dataVersionStatus == DataVersionStatus.TOO_UPDATED)
			return;
		
		addRenderableWidget(Buttons.of(width / 2 - 158, height / 2 - 10, 100, 20,
				Component.translatableEscape("nbteditor.client_chest.data_version.import_page"), btn -> {
					LoadingScreen.show(
							addSuccessMessage(ClientChestHelper.importPage(ClientChestScreen.PAGE), false),
							success -> ClientChestScreen.show());
				}, new MVTooltip("nbteditor.client_chest.data_version.import_page.desc")))
				.active = (dataVersionStatus == DataVersionStatus.UNKNOWN);
		addRenderableWidget(Buttons.of(width / 2 - 158, height / 2 + 14, 100, 20,
				Component.translatableEscape("nbteditor.client_chest.data_version.import_all_pages"), btn -> {
					LoadingScreen.show(
							addSuccessMessage(ClientChestHelper.importAllPages(), true),
							success -> ClientChestScreen.show());
				}, new MVTooltip("nbteditor.client_chest.data_version.import_all_pages.desc")));
		
		dataVersion = addRenderableWidget(
				new NamedTextFieldWidget(width / 2 - 50, height / 2 - 32, 100, 16, dataVersion)
				.name(Component.translatableEscape("nbteditor.nbt.import.data_version"))
				.tooltip(new MVTooltip("nbteditor.nbt.import.data_version.desc")));
		updatePageBtn = addRenderableWidget(Buttons.of(width / 2 - 50, height / 2 - 10, 100, 20,
				Component.translatableEscape("nbteditor.client_chest.data_version.update_page"), btn -> {
					Optional<Integer> dataVersionValue = Version.getDataVersion(dataVersion.getValue())
							.filter(value -> value < Version.getDataVersion());
					if (dataVersionStatus == DataVersionStatus.UNKNOWN && dataVersionValue.isEmpty())
						return;
					
					updateWithWarning(() -> {
						LoadingScreen.show(
								addSuccessMessage(ClientChestHelper.updatePage(ClientChestScreen.PAGE, dataVersionValue), false),
								success -> ClientChestScreen.show());
					});
				}, new MVTooltip("nbteditor.client_chest.data_version.update_page.desc." + (dataVersionStatus == DataVersionStatus.UNKNOWN ? "unknown" : "old"))));
		addRenderableWidget(Buttons.of(width / 2 - 50, height / 2 + 14, 100, 20,
				Component.translatableEscape("nbteditor.client_chest.data_version.update_all_pages"), btn -> {
					Optional<Integer> dataVersionValue = Version.getDataVersion(dataVersion.getValue())
							.filter(value -> value < Version.getDataVersion());
					
					updateWithWarning(() -> {
						LoadingScreen.show(
								addSuccessMessage(ClientChestHelper.updateAllPages(dataVersionValue), true),
								success -> ClientChestScreen.show());
					});
				}, new MVTooltip("nbteditor.client_chest.data_version.update_all_pages.desc")));
	}
	
	@Override
	public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		boolean fullButtons = (dataVersionStatus != DataVersionStatus.TOO_UPDATED);
		
		if (fullButtons) {
			dataVersion.setValid((dataVersionStatus == DataVersionStatus.OUTDATED && dataVersion.getValue().isEmpty()) ||
					Version.getDataVersion(dataVersion.getValue()).filter(value -> value < Version.getDataVersion()).isPresent());
			updatePageBtn.active = (dataVersionStatus == DataVersionStatus.UNKNOWN ? dataVersion.isValid() : dataVersion.getValue().isEmpty());
		}
		
		MVTooltip.setOneTooltip(true, false);
		
		Drawing.renderBackground(this, context);
		super.extractRenderState(context, mouseX, mouseY, delta);
		Drawing.drawCenteredTextWithShadow(context, font,
				msg, width / 2, height / 2 - 44 - font.lineHeight / 2, -1);
		if (fullButtons) {
			Drawing.fill(context, width / 2 - 55, height / 2 - 34, width / 2 - 53, height / 2 + 34, 0xFFAAAAAA);
			Drawing.fill(context, width / 2 + 53, height / 2 - 34, width / 2 + 55, height / 2 + 34, 0xFFAAAAAA);
			Drawing.drawCenteredTextWithShadow(context, font,
					Component.translatableEscape("nbteditor.client_chest.data_version.import", Version.getReleaseTarget()),
					width / 2 - 108, height / 2 - 24 - font.lineHeight / 2, -1);
		}
		Drawing.renderLogo(context);
		
		MVTooltip.renderOneTooltip(context, mouseX, mouseY);
	}
	
	@Override
	public boolean keyPressed(KeyEvent input) {
		int keyCode = input.key(); int scanCode = input.scancode(); int modifiers = input.modifiers();
		if (keyCode == GLFW.GLFW_KEY_PAGE_UP || keyCode == GLFW.GLFW_KEY_PAGE_DOWN) {
			boolean prev = (keyCode == GLFW.GLFW_KEY_PAGE_UP);
			if (ConfigScreen.isInvertedPageKeybinds())
				prev = !prev;
			if (prev)
				prevPage();
			else
				nextPage();
		}
		
		return super.keyPressed(input);
	}
	
	private void prevPage() {
		if (ClientChestScreen.PAGE > 0) {
			ClientChestScreen.PAGE--;
			ClientChestScreen.show();
		}
	}
	private void nextPage() {
		if (ClientChestScreen.PAGE < NBTEditorClient.CLIENT_CHEST.getPageCount() - 1) {
			ClientChestScreen.PAGE++;
			ClientChestScreen.show();
		}
	}
	
	private CompletableFuture<Boolean> addSuccessMessage(CompletableFuture<Boolean> future, boolean all) {
		future.thenAccept(success -> {
			if (success) {
				MutableComponent msg;
				if (all) {
					msg = Component.translatableEscape("nbteditor.client_chest.data_version.update_all_pages_success");
				} else {
					msg = Component.translatableEscape("nbteditor.client_chest.data_version.update_page_success",
							Component.literal(ClientChestScreen.PAGE + 1 + "").withStyle(ChatFormatting.GREEN));
				}
				Minecraft.getInstance().player.sendSystemMessage(ClientChest.attachShowFolder(msg));
			}
		});
		return future;
	}
	
	private void updateWithWarning(Runnable callback) {
		callback.run();
	}
	
}
