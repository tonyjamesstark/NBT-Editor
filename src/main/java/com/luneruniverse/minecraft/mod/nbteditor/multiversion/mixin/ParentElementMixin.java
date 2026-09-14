package com.luneruniverse.minecraft.mod.nbteditor.multiversion.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.OldEventBehavior;

import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.events.ContainerEventHandler;

@Mixin(ContainerEventHandler.class)
public interface ParentElementMixin {
	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	private void mouseClicked(MouseButtonEvent click, boolean doubled, CallbackInfoReturnable<Boolean> info) {
		if (!(this instanceof OldEventBehavior))
			return;
		
		ContainerEventHandler source = (ContainerEventHandler) (Object) this;
		
		for (GuiEventListener element : source.children()) {
			if (element.mouseClicked(click, doubled)) {
				source.setFocused(element);
				if (click.button() == 0)
					source.setDragging(true);
				info.setReturnValue(true);
				return;
			}
		}
		info.setReturnValue(false);
	}
}
