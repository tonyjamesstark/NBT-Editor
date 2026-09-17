package com.luneruniverse.minecraft.mod.nbteditor.multiversion.networking.mixin.toggled;

import java.util.List;

import com.luneruniverse.minecraft.mod.nbteditor.misc.BasicMixinPlugin;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.Version;

public class NetworkingMixinPlugin extends BasicMixinPlugin {
	
	@Override
	public void addMixins(List<String> output) {
		Version.newSwitch()
				.range("1.20.5", null, () -> {})
				.run();
		Version.newSwitch()
				.range("1.20.5", null, () -> output.add("toggled.CustomPayload1Mixin"))
				.run();
		Version.newSwitch()
				.range("1.20.5", null, () -> output.add("toggled.ClientConnectionMixin_1_20_5"))
				.run();
	}
	
}
