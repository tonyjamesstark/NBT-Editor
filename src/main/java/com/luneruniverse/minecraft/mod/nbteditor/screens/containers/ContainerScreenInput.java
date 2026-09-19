package com.luneruniverse.minecraft.mod.nbteditor.screens.containers;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.luneruniverse.minecraft.mod.nbteditor.NBTEditorClient;
import com.luneruniverse.minecraft.mod.nbteditor.commands.get.GetLostItemCommand;
import com.luneruniverse.minecraft.mod.nbteditor.mixin.HandledScreenAccessor;
import com.luneruniverse.minecraft.mod.nbteditor.nbtreferences.itemreferences.ItemReference;
import com.luneruniverse.minecraft.mod.nbteditor.screens.ConfigScreen;
import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.ItemTagReferences;
import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.specific.data.Enchants;

import com.luneruniverse.minecraft.mod.nbteditor.util.AccessWidenedApi;
import com.luneruniverse.minecraft.mod.nbteditor.util.Keys;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.client.Minecraft;

/**
 * What the mod does with clicks and keypresses inside a container screen.
 *
 * <p>Called from the screen mixins, which supply the screen instance they are injected into. Both
 * entry points decide for themselves whether the screen is editable, so the mixins stay thin.
 */
public class ContainerScreenInput {

	/** Slots 0 through 4 of the player inventory screen are the crafting grid and the result. */
	private static final int LAST_NON_STORAGE_INVENTORY_SLOT = 4;

	/** Combines a held enchanted book into the clicked item, when ctrl is down. */
	public static void onMouseClick(AbstractContainerScreen<?> source, Slot slot, int slotId, int button, ContainerInput actionType, CallbackInfo info) {
		if (!source.getMenu().getCarried().isEmpty())
			GetLostItemCommand.addToHistory(source.getMenu().getCarried());

		boolean creativeInv = (source instanceof CreativeModeInventoryScreen);

		if (!creativeInv && !NBTEditorClient.SERVER_CONN.isScreenEditable())
			return;

		if (!Keys.hasControlDown())
			return;

		if (slot instanceof CreativeModeInventoryScreen.SlotWrapper creativeSlot)
			slot = AccessWidenedApi.getWrappedSlot(creativeSlot);

		if (actionType == ContainerInput.PICKUP && slot != null &&
				(slot.container == Minecraft.getInstance().player.getInventory() || !creativeInv) &&
				(!(source instanceof InventoryScreen) || slot.index > LAST_NON_STORAGE_INVENTORY_SLOT)) {
			ItemStack cursor = source.getMenu().getCarried();
			ItemStack item = slot.getItem();
			if (cursor == null || cursor.isEmpty() || item == null || item.isEmpty())
				return;
			if (cursor.getItem() == Items.ENCHANTED_BOOK || item.getItem() == Items.ENCHANTED_BOOK) {
				if (cursor.getItem() != Items.ENCHANTED_BOOK) { // Make sure the cursor is an enchanted book
					ItemStack temp = cursor;
					cursor = item;
					item = temp;
				}

				Enchants enchants = ItemTagReferences.ENCHANTMENTS.get(item);
				enchants.addEnchants(ItemTagReferences.ENCHANTMENTS.get(cursor).getEnchants());
				ItemTagReferences.ENCHANTMENTS.set(item, enchants);

				ItemReference.getContainerItem(source, slot).saveItem(item);
				NBTEditorClient.CURSOR_MANAGER.setCursor(ItemStack.EMPTY);

				info.cancel();
			}
		}
	}

	/** Routes a keypress over a hovered slot to the mod's item keybinds. */
	public static void keyPressed(AbstractContainerScreen<?> source, KeyEvent input, CallbackInfoReturnable<Boolean> info) {
		boolean creativeInv = (source instanceof CreativeModeInventoryScreen);

		Slot hoveredSlot = ((HandledScreenAccessor) source).getHoveredSlot();

		if (hoveredSlot instanceof CreativeModeInventoryScreen.SlotWrapper creativeSlot)
			hoveredSlot = AccessWidenedApi.getWrappedSlot(creativeSlot);

		if (hoveredSlot != null &&
				((creativeInv && hoveredSlot.container == Minecraft.getInstance().player.getInventory()) ||
						(!creativeInv && NBTEditorClient.SERVER_CONN.isScreenEditable())) &&
				(!(source instanceof InventoryScreen) || hoveredSlot.index > LAST_NON_STORAGE_INVENTORY_SLOT) &&
				(ConfigScreen.isAirEditable() || hoveredSlot.getItem() != null && !hoveredSlot.getItem().isEmpty())) {
			if (ClientHandledScreen.handleKeybind(input.key(), hoveredSlot.getItem(),
					ItemReference.getContainerItem(source, hoveredSlot))) {
				info.setReturnValue(true);
			}
		}
	}

}
