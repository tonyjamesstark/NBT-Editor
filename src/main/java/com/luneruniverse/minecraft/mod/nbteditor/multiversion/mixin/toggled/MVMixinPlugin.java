package com.luneruniverse.minecraft.mod.nbteditor.multiversion.mixin.toggled;

import java.util.List;

import com.luneruniverse.minecraft.mod.nbteditor.misc.BasicMixinPlugin;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.Version;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

public class MVMixinPlugin extends BasicMixinPlugin {
	
	@Override
	public void addMixins(List<String> output) {
		Version.newSwitch()
				.range("1.20.3", null, () -> {})
				.run();
		Version.newSwitch()
				.range("1.20.5", null, () -> output.add("toggled.ItemStackMixin"))
				.run();
		
		if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER)
			return;
		
		Version.newSwitch()
				.range("1.19.3", null, () -> output.add("toggled.ScreenMixin"))
				.run();
		Version.newSwitch()
				.range("1.20.2", null, () -> {})
				.run();
		Version.newSwitch()
				.range("1.20.5", null, () -> output.add("toggled.BookScreenContentsMixin"))
				.run();
	}
	
}
