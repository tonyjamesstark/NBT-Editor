package com.luneruniverse.minecraft.mod.nbteditor.screens.containers;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.networking.MVClientNetworking;
import com.luneruniverse.minecraft.mod.nbteditor.packets.SetCursorC2SPacket;

import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.client.Minecraft;
import com.luneruniverse.minecraft.mod.nbteditor.util.AccessWidenedApi;
import com.luneruniverse.minecraft.mod.nbteditor.util.PlayerItems;

public class CursorManager {
	
	private AbstractContainerScreen<?> currentRoot;
	private boolean currentRootIsInventory;
	private boolean currentRootHasServerCursor;
	private boolean currentRootClosed;
	private AbstractContainerScreen<?> currentBranch;
	
	public CursorManager() {}
	
	public boolean isBranched() {
		return currentRoot != null && currentRoot != currentBranch;
	}
	public AbstractContainerScreen<?> getCurrentRoot() {
		return currentRoot;
	}
	public boolean isCurrentRootClosed() {
		return currentRootClosed;
	}
	public AbstractContainerScreen<?> getCurrentBranch() {
		return currentBranch;
	}
	
	public void onNoScreenSet() {
		currentRoot = null;
		currentRootClosed = false;
		currentBranch = null;
	}
	
	public void onHandledScreenSet(AbstractContainerScreen<?> screen) {
		if (screen == currentBranch)
			return;
		
		currentRoot = screen;
		currentRootIsInventory = (currentRoot.getMenu() == Minecraft.getInstance().player.inventoryMenu ||
				currentRoot instanceof CreativeModeInventoryScreen);
		currentRootHasServerCursor = !(screen instanceof CreativeModeInventoryScreen);
		currentRootClosed = false;
		currentBranch = screen;
	}
	
	public void onCloseScreenPacket() {
		if (currentRoot == null || currentRootIsInventory)
			return;
		
		currentRootClosed = true;
	}
	
	private void transferCursorTo(AbstractContainerScreen<?> branch) {
		if (currentBranch == branch)
			return;
		
		AbstractContainerMenu handler = branch.getMenu();
		AbstractContainerMenu currentHandler = currentBranch.getMenu();
		
		AccessWidenedApi.setCursorStackSilently(handler, currentHandler.getCarried());
		AccessWidenedApi.setCursorStackSilently(currentHandler, ItemStack.EMPTY);
		
		if (currentRootHasServerCursor) {
			if (branch == currentRoot)
				MVClientNetworking.send(new SetCursorC2SPacket(handler.getCarried().copy()));
			else if (currentBranch == currentRoot)
				MVClientNetworking.send(new SetCursorC2SPacket(ItemStack.EMPTY));
		}
	}
	
	public void showBranch(AbstractContainerScreen<?> branch) {
		if (currentRoot == null) {
			if (Minecraft.getInstance().player.hasInfiniteMaterials()) {
				currentRoot = new CreativeModeInventoryScreen(Minecraft.getInstance().player,
						Minecraft.getInstance().player.connection.enabledFeatures(),
						Minecraft.getInstance().options.operatorItemsTab().get());
				currentRootHasServerCursor = false;
			} else {
				currentRoot = new InventoryScreen(Minecraft.getInstance().player);
				currentRootHasServerCursor = true;
			}
			currentRootIsInventory = true;
			currentRootClosed = false;
			currentBranch = currentRoot;
		}
		if (branch == null)
			branch = currentRoot;
		
		if (currentRootClosed && branch == currentRoot) {
			closeRoot();
			return;
		}
		
		transferCursorTo(branch);
		currentBranch = branch;
		Minecraft.getInstance().player.containerMenu = branch.getMenu();
		branch.skipNextRelease = true;
		Minecraft.getInstance().setScreenAndShow(branch);
	}
	public void showRoot() {
		showBranch(currentRoot);
	}
	
	public void closeRoot() {
		if (currentRoot == null) {
			Minecraft.getInstance().setScreenAndShow(null);
			return;
		}
		
		if (currentRootClosed) {
			if (currentBranch != currentRoot) {
				ItemStack cursor = currentBranch.getMenu().getCarried();
				if (currentRootHasServerCursor) {
					PlayerItems.get(cursor, true);
					cursor = ItemStack.EMPTY;
				}
				AccessWidenedApi.setCursorStackSilently(currentRoot.getMenu(), cursor);
			}
			Minecraft.getInstance().player.clientSideCloseContainer(); // will trigger #onNoScreenSet()
			return;
		}
		
		transferCursorTo(currentRoot);
		Minecraft.getInstance().player.closeContainer(); // will trigger #onNoScreenSet()
	}
	
	public void setCursor(ItemStack item) {
		if (currentRoot == null)
			throw new IllegalStateException("There is no root to set the cursor of");
		
		AccessWidenedApi.setCursorStackSilently(currentBranch.getMenu(), item);
		
		if (currentRootHasServerCursor && currentBranch == currentRoot)
			MVClientNetworking.send(new SetCursorC2SPacket(item.copy()));
	}
	
}
