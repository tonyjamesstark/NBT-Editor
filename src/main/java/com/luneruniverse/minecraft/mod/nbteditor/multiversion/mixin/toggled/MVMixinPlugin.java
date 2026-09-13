package com.luneruniverse.minecraft.mod.nbteditor.multiversion.mixin.toggled;

import java.util.List;

import com.luneruniverse.minecraft.mod.nbteditor.misc.BasicMixinPlugin;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

public class MVMixinPlugin extends BasicMixinPlugin {
	
	@Override
	public void addMixins(List<String> output) {
		output.add("toggled.ItemStackMixin");
		
		if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER)
			return;
		
		output.add("toggled.ScreenMixin");
		output.add("toggled.BookScreenContentsMixin");
	}
	
}
