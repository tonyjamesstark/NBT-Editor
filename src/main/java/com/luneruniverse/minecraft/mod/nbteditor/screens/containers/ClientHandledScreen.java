package com.luneruniverse.minecraft.mod.nbteditor.screens.containers;

import java.util.function.Function;

import org.lwjgl.glfw.GLFW;

import com.luneruniverse.minecraft.mod.nbteditor.NBTEditorClient;
import com.luneruniverse.minecraft.mod.nbteditor.commands.get.GetLostItemCommand;
import com.luneruniverse.minecraft.mod.nbteditor.containers.ContainerIOs;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.IgnoreCloseScreenPacket;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVDrawableHelper;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.OldEventBehavior;
import com.luneruniverse.minecraft.mod.nbteditor.nbtreferences.itemreferences.InventoryItemReference;
import com.luneruniverse.minecraft.mod.nbteditor.nbtreferences.itemreferences.ItemReference;
import com.luneruniverse.minecraft.mod.nbteditor.screens.ConfigScreen;
import com.luneruniverse.minecraft.mod.nbteditor.screens.NBTEditorScreen;
import com.luneruniverse.minecraft.mod.nbteditor.screens.factories.LocalFactoryScreen;
import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.ItemTagReferences;
import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.specific.data.Enchants;
import com.luneruniverse.minecraft.mod.nbteditor.util.MainUtil;

import com.luneruniverse.minecraft.mod.nbteditor.util.Keys;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.client.Minecraft;

public class ClientHandledScreen extends net.minecraft.client.gui.screens.inventory.ContainerScreen implements OldEventBehavior, IgnoreCloseScreenPacket {
	
	private static final Identifier TEXTURE = Identifier.parse("textures/gui/container/generic_54.png");
	
	public static boolean handleKeybind(int keyCode, Slot hoveredSlot, Runnable parent, Function<Slot, ItemReference> containerRef) {
		if (hoveredSlot != null &&
				(ConfigScreen.isAirEditable() || hoveredSlot.getItem() != null && !hoveredSlot.getItem().isEmpty())) {
			ItemReference ref;
			if (hoveredSlot.container == Minecraft.getInstance().player.getInventory()) {
				ref = new InventoryItemReference(hoveredSlot.getContainerSlot());
				if (parent != null)
					((InventoryItemReference) ref).setParent(parent);
			} else
				ref = containerRef.apply(hoveredSlot);
			return handleKeybind(keyCode, hoveredSlot.getItem(), ref);
		}
		return false;
	}
	public static boolean handleKeybind(int keyCode, ItemStack item, ItemReference ref) {
		if (keyCode == GLFW.GLFW_KEY_DELETE) {
			if (item == null || item.isEmpty())
				return false;
			GetLostItemCommand.addToHistory(item);
			ref.saveItem(ItemStack.EMPTY);
			return true;
		}
		if (keyCode != GLFW.GLFW_KEY_SPACE)
			return false;
		
		boolean notAir = item != null && !item.isEmpty();
		if (Keys.hasControlDown()) {
			if (notAir && ContainerIOs.isSupported(item))
				ContainerScreen.show(ref);
		} else if (Keys.hasShiftDown()) {
			if (notAir)
				Minecraft.getInstance().setScreenAndShow(new LocalFactoryScreen<>(ref));
		} else
			Minecraft.getInstance().setScreenAndShow(new NBTEditorScreen<>(ref));
		
		return true;
	}
	
	private ServerInventoryManager serverInv;
	
	protected ClientHandledScreen(int rows, Component title) {
		super(new ClientScreenHandler(rows), Minecraft.getInstance().player.getInventory(), title);
		((ClientScreenHandler) menu).setScreen(this);
		menu.suppressRemoteUpdates();
	}
	
	protected void setSlotTextures(Identifier... textures) {
		((ClientScreenHandler) menu).setSlotTextures(textures);
	}
	
	public ServerInventoryManager getServerInventoryManager() {
		return serverInv;
	}
	
	@Override
	protected void init() {
		super.init();
		serverInv = new ServerInventoryManager();
	}
	
	@Override
	public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		super.extractBackground(context, mouseX, mouseY, delta);
		MVDrawableHelper.drawTexture(context, TEXTURE, leftPos, topPos, 0, 0, imageWidth, menu.getRowCount() * 18 + 17);
		MVDrawableHelper.drawTexture(context, TEXTURE, leftPos, topPos + menu.getRowCount() * 18 + 17, 0, 126, imageWidth, 96);
		
		if (showLogo())
			MainUtil.renderLogo(context);
	}
	protected boolean showLogo() {
		return true;
	}
	
	@Override
	protected void extractLabels(GuiGraphicsExtractor context, int mouseX, int mouseY) {
		getLockedSlotsInfo().renderLockedHighlights(context, menu, true, false, true);
		
		MVDrawableHelper.drawTextWithoutShadow(context, font, getRenderedTitle(), titleLabelX, titleLabelY, 4210752);
		MVDrawableHelper.drawTextWithoutShadow(context, font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 4210752);
	}
	protected Component getRenderedTitle() {
		return title;
	}
	
	public void setInitialFocus(GuiEventListener element) {
		super.setInitialFocus(element);
		setFocused(element);
	}
	@Override
	protected void setInitialFocus() {}
	
	public boolean shouldPause() {
		return false;
	}
	
	@Override
	public final void tick() {
		super.tick();
	}
	@Override
	protected void containerTick() {}
	
	public void close() {
		NBTEditorClient.CURSOR_MANAGER.closeRoot();
	}
	@Override
	public void removed() {
		serverInv = null;
		// Don't always drop cursor in older versions
	}
	
	
	@Override
	protected void slotClicked(Slot slot, int slotId, int button, ContainerInput actionType) {
		if (slot != null) {
			LockedSlotsInfo lockedSlotsInfo = getLockedSlotsInfo();
			if (lockedSlotsInfo.isBlocked(slot, button, actionType, false)) {
				if (lockedSlotsInfo.isCopyLockedItem() && slot.container != minecraft.player.getInventory()) {
					switch (actionType) {
						case PICKUP, PICKUP_ALL -> {
							ItemStack item = slot.getItem();
							if (item.isEmpty())
								break;
							if (!menu.getCarried().isEmpty() &&
									!ItemStack.isSameItemSameComponents(item, menu.getCarried())) {
								GetLostItemCommand.loseItem(menu.getCarried());
								menu.setCarried(ItemStack.EMPTY);
							}
							ItemStack cursor = menu.getCarried();
							if (!cursor.isEmpty()) {
								cursor.setCount(Math.min(cursor.getMaxStackSize(), cursor.getCount() + item.getCount()));
								menu.setCarried(cursor);
							} else
								menu.setCarried(item.copy());
							serverInv.updateServer();
						}
						case CLONE -> {
							ItemStack item = slot.getItem();
							if (item.isEmpty())
								break;
							if (!menu.getCarried().isEmpty())
								break;
							item = item.copy();
							item.setCount(item.getMaxStackSize());
							menu.setCarried(item);
							serverInv.updateServer();
						}
						case QUICK_MOVE -> {
							ItemStack prevItem = slot.getItem().copy();
							ClientScreenHandlerSlot.unlockDuring(() -> menu.clicked(slot.index, button, actionType, Minecraft.getInstance().player));
							slot.set(prevItem);
							serverInv.updateServer();
						}
						case THROW -> {
							ItemStack item = slot.getItem();
							if (button == 0) {
								item = item.copy();
								item.setCount(1);
							}
							MainUtil.dropCreativeStack(item);
						}
						case SWAP -> {}
						case QUICK_CRAFT -> throw new IllegalArgumentException("Invalid ContainerInput: " + actionType);
					}
				}
				return;
			}
		}
		
		if (!(this instanceof CursorHistoryScreen))
			GetLostItemCommand.addToHistory(menu.getCarried());
		
		if (!(slot != null && allowEnchantmentCombine() && Keys.hasControlDown() && tryCombineEnchantments(slot, actionType)))
			menu.clicked(slot == null ? slotId : slot.index, button, actionType, Minecraft.getInstance().player);
		
		if (!(this instanceof CursorHistoryScreen))
			GetLostItemCommand.addToHistory(menu.getCarried());
		
		serverInv.updateServer();
		onChange();
	}
	
	private boolean tryCombineEnchantments(Slot slot, ContainerInput actionType) {
		if (actionType == ContainerInput.PICKUP && slot != null) {
			ItemStack cursor = menu.getCarried();
			ItemStack item = slot.getItem();
			if (cursor == null || cursor.isEmpty() || item == null || item.isEmpty())
				return false;
			if (cursor.getItem() == Items.ENCHANTED_BOOK || item.getItem() == Items.ENCHANTED_BOOK) {
				if (cursor.getItem() != Items.ENCHANTED_BOOK) { // Make sure the cursor is an enchanted book
					ItemStack temp = cursor;
					cursor = item;
					item = temp;
				}
				
				Enchants enchants = ItemTagReferences.ENCHANTMENTS.get(item);
				enchants.addEnchants(ItemTagReferences.ENCHANTMENTS.get(cursor).getEnchants());
				ItemTagReferences.ENCHANTMENTS.set(item, enchants);
				
				slot.set(item);
				menu.setCarried(ItemStack.EMPTY);
				return true;
			}
		}
		
		return false;
	}
	public boolean allowEnchantmentCombine() {
		return false;
	}
	
	public LockedSlotsInfo getLockedSlotsInfo() {
		return LockedSlotsInfo.NONE;
	}
	public void onChange() {
		
	}
	
}
