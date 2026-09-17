package com.luneruniverse.minecraft.mod.nbteditor.multiversion.networking.mixin.toggled;

import java.util.List;

import com.luneruniverse.minecraft.mod.nbteditor.misc.BasicMixinPlugin;

public class NetworkingMixinPlugin extends BasicMixinPlugin {
	
	@Override
	public void addMixins(List<String> output) {
		output.add("toggled.CustomPayload1Mixin");
		output.add("toggled.ClientConnectionMixin_1_20_5");
	}
	
}
