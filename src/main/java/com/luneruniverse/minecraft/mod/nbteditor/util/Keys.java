package com.luneruniverse.minecraft.mod.nbteditor.util;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;

import net.minecraft.client.input.InputQuirks;

/**
 * Which modifier keys are held right now, and whether a key code is one of the editing shortcuts.
 *
 * <p>1.21.9 moved the modifier queries off {@code Screen} and onto the input record handed to an
 * event. The mod asks outside an event - while rendering, or from a keybind handler that only has
 * the key code - so these poll the window directly, which is what {@code Screen}'s statics did.
 */
public class Keys {
	
	private static boolean isEitherPressed(int left, int right) {
		Window window = MainUtil.client.getWindow();
		return InputConstants.isKeyDown(window, left) || InputConstants.isKeyDown(window, right);
	}
	
	public static boolean hasShiftDown() {
		return isEitherPressed(GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT);
	}
	
	/** On macOS the command key stands in for control, matching the rest of the game. */
	public static boolean hasControlDown() {
		if (InputQuirks.REPLACE_CTRL_KEY_WITH_CMD_KEY)
			return isEitherPressed(GLFW.GLFW_KEY_LEFT_SUPER, GLFW.GLFW_KEY_RIGHT_SUPER);
		return isEitherPressed(GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL);
	}
	
	public static boolean hasAltDown() {
		return isEitherPressed(GLFW.GLFW_KEY_LEFT_ALT, GLFW.GLFW_KEY_RIGHT_ALT);
	}
	
	/** Control and nothing else: shift or alt held means the user meant something different. */
	private static boolean isShortcut(int keyCode, int shortcutKey) {
		return keyCode == shortcutKey && hasControlDown() && !hasShiftDown() && !hasAltDown();
	}
	
	public static boolean isSelectAll(int keyCode) {
		return isShortcut(keyCode, GLFW.GLFW_KEY_A);
	}
	
	public static boolean isCopy(int keyCode) {
		return isShortcut(keyCode, GLFW.GLFW_KEY_C);
	}
	
	public static boolean isPaste(int keyCode) {
		return isShortcut(keyCode, GLFW.GLFW_KEY_V);
	}
	
	public static boolean isCut(int keyCode) {
		return isShortcut(keyCode, GLFW.GLFW_KEY_X);
	}
	
}
