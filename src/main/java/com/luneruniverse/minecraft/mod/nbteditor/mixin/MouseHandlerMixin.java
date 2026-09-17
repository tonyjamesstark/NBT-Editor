package com.luneruniverse.minecraft.mod.nbteditor.mixin;

import org.joml.Vector2d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import com.luneruniverse.minecraft.mod.nbteditor.screens.containers.CursorManager;

import net.minecraft.client.MouseHandler;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
	
	@Shadow
	private double xpos;
	@Shadow
	private double ypos;
	
	// releaseMouse always puts the pointer in the middle of the window, which is right when the
	// world is giving up the mouse and wrong when one of our screens is only closing so another
	// can open. The fields are set just above this call, so both they and the args need the
	// position putting back.
	@ModifyArgs(method = "releaseMouse", at = @At(value = "INVOKE",
			target = "Lcom/mojang/blaze3d/platform/InputConstants;grabOrReleaseMouse(Lcom/mojang/blaze3d/platform/Window;IDD)V"))
	private void releaseMouse_keepPointerWhereItWas(Args args) {
		Vector2d restore = CursorManager.takeMousePositionToRestore();
		if (restore == null)
			return;
		
		xpos = restore.x;
		ypos = restore.y;
		args.set(2, xpos);
		args.set(3, ypos);
	}
	
}
