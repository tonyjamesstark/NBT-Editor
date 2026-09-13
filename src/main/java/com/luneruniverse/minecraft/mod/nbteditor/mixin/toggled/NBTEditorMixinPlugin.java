package com.luneruniverse.minecraft.mod.nbteditor.mixin.toggled;

import java.util.List;

import com.luneruniverse.minecraft.mod.nbteditor.misc.BasicMixinPlugin;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.Version;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

public class NBTEditorMixinPlugin extends BasicMixinPlugin {
	
	@Override
	public void addMixins(List<String> output) {
		Version.newSwitch()
				.range("1.19.3", null, () -> output.add("toggled.ServerPlayNetworkHandlerMixin"))
				.run();
		Version.newSwitch()
				.range("1.21.0", null, () -> output.add("toggled.ArmorSlotMixin"))
				.run();
		Version.newSwitch()
				.range("1.21.5", null, () -> {}) // Covered by ArmorSlotMixin
				.run();
		Version.newSwitch()
				.range("1.20.5", null, () -> {
					output.add("toggled.BundleItemMixin_1_20_5");
					output.add("toggled.BundleContentsComponentBuilderMixin");
				})
				.run();
		Version.newSwitch()
				.range("1.21.2", null, () -> {})
				.run();
		Version.newSwitch()
				.range("1.21.5", null, () -> output.add("toggled.ClickSlotC2SPacketMixin_1_21_5"))
				.run();
		Version.newSwitch()
				.range("1.21.5", null, () -> output.add("toggled.PlayStateFactories1Mixin"))
				.run();
		
		if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER)
			return;
		
		Version.newSwitch()
				.range("1.20.0", null, () -> output.add("toggled.DrawContextMixin"))
				.run();
		Version.newSwitch()
				.range("1.20.5", null, () -> output.add("toggled.ItemStackMixin"))
				.run();
		Version.newSwitch()
				.range("1.20.5", null, () -> {
					output.add("toggled.RegistryEntryReferenceMixin");
					output.add("toggled.Registry1Mixin");
				})
				.run();
		Version.newSwitch()
				.range("1.21.0", null, () -> output.add("toggled.TooltipMixin"))
				.run();
		Version.newSwitch()
				.range("1.21.0", null, () -> output.add("toggled.EnchantmentMixin"))
				.run();
		Version.newSwitch()
				.run();
		Version.newSwitch()
				.range("1.21.4", null, () -> {
					output.add("toggled.ItemModelManagerMixin");
					output.add("toggled.ItemRenderStateLayerRenderStateMixin");
				})
				.run();
		Version.newSwitch()
				.range("1.21.2", null, () -> output.add("toggled.ClientPlayNetworkHandlerMixin"))
				.run();
		Version.newSwitch()
				.range("1.21.5", null, () -> output.add("toggled.SnbtParsingMixin"))
				.run();
	}
	
}
