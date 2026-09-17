package com.luneruniverse.minecraft.mod.nbteditor.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.luneruniverse.minecraft.mod.nbteditor.misc.ParallelResourceReload;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.DynamicRegistryManagerHolder;

import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.server.packs.resources.ReloadInstance;

@Mixin(LoadingOverlay.class)
public class SplashOverlayMixin {
	@ModifyVariable(method = "<init>", at = @At("HEAD"), ordinal = 0)
	private static ReloadInstance init_monitor(ReloadInstance monitor) {
		return new ParallelResourceReload(monitor, DynamicRegistryManagerHolder.loadDefaultManager());
	}
}
