package com.luneruniverse.minecraft.mod.nbteditor.mixin.toggled;

import java.util.List;

import com.luneruniverse.minecraft.mod.nbteditor.misc.BasicMixinPlugin;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

public class NBTEditorMixinPlugin extends BasicMixinPlugin {
	
	@Override
	public void addMixins(List<String> output) {
		output.add("toggled.ServerPlayNetworkHandlerMixin");
		output.add("toggled.ArmorSlotMixin");
		output.add("toggled.BundleItemMixin_1_20_5");
		output.add("toggled.BundleContentsComponentBuilderMixin");
		output.add("toggled.ClickSlotC2SPacketMixin_1_21_5");
		output.add("toggled.PlayStateFactories1Mixin");
		
		if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER)
			return;
		
		output.add("toggled.DrawContextMixin");
		output.add("toggled.ItemStackMixin");
		output.add("toggled.RegistryEntryReferenceMixin");
		output.add("toggled.Registry1Mixin");
		output.add("toggled.TooltipMixin");
		output.add("toggled.EnchantmentMixin");
		output.add("toggled.ItemModelManagerMixin");
		output.add("toggled.ClientPlayNetworkHandlerMixin");
		output.add("toggled.SnbtParsingMixin");
	}
	
}
