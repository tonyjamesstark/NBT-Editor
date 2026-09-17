package com.luneruniverse.minecraft.mod.nbteditor.screens.containers;

import org.joml.Vector2d;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.networking.MVClientNetworking;
import com.luneruniverse.minecraft.mod.nbteditor.packets.SetCursorC2SPacket;

import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
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
	
	/**
	 * Handing the mouse back to the world warps the pointer to the middle of the window. When the
	 * close is only there to make room for another screen, that warp reads as the cursor jumping
	 * away mid-interaction, so remember where it was and let {@code MouseHandlerMixin} put it back.
	 */
	public void closeRootToNewScreen() {
		MouseHandler mouse = Minecraft.getInstance().mouseHandler;
		restoreMouseX = mouse.xpos();
		restoreMouseY = mouse.ypos();
		// The next release is the one this close causes. The deadline is only so an unrelated
		// release much later, after the screen never arrived, is left alone.
		restoreMouseUntil = System.currentTimeMillis() + RESTORE_MOUSE_WINDOW_MS;
		closeRoot();
	}
	
	private static final long RESTORE_MOUSE_WINDOW_MS = 500;
	private static double restoreMouseX;
	private static double restoreMouseY;
	private static long restoreMouseUntil;
	
	/** The pointer position the next mouse release should keep, or null to let it centre. */
	public static Vector2d takeMousePositionToRestore() {
		if (System.currentTimeMillis() > restoreMouseUntil)
			return null;
		restoreMouseUntil = 0;
		return new Vector2d(restoreMouseX, restoreMouseY);
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
