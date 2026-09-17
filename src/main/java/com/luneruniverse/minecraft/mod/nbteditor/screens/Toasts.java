package com.luneruniverse.minecraft.mod.nbteditor.screens;


import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;

/** The corner pop-up the mod uses to tell the user something outside a screen. */
public class Toasts {
	
	/**
	 * Shows a system toast.
	 *
	 * <p>Borrows the pack-load-failure id because a toast id only decides which earlier toast a
	 * new one replaces, and the mod has no reason to queue two of its own.
	 */
	public static void show(Component title, Component description) {
		Minecraft.getInstance().gui.toastManager().addToast(
				new SystemToast(SystemToast.SystemToastId.PACK_LOAD_FAILURE, title, description));
	}
	
}
