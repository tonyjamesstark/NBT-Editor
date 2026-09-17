package com.luneruniverse.minecraft.mod.nbteditor.mixin.toggled;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.llamalad7.mixinextras.sugar.Local;
import com.luneruniverse.minecraft.mod.nbteditor.server.ServerMixinLink;

import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BundleItem;

@Mixin(BundleItem.class)
public class BundleItemMixin_1_20_5 {
	@ModifyVariable(method = {"overrideStackedOnOther", "overrideOtherStackedOnMe"}, at = @At("STORE"), require = 2)
	private BundleContents.Mutable newBundleContentsComponentBuilder(BundleContents.Mutable builder, @Local Player player) {
		if (ServerMixinLink.isNoSlotRestrictions(player, false))
			ServerMixinLink.NO_SLOT_RESTRICTIONS_BUNDLES.put(builder, true);
		return builder;
	}
}
