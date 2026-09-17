package com.luneruniverse.minecraft.mod.nbteditor.mixin.toggled;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.luneruniverse.minecraft.mod.nbteditor.server.ServerMixinLink;

import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.ItemStack;

@Mixin(BundleContents.Mutable.class)
public class BundleContentsComponentBuilderMixin {
	@Redirect(method = {"tryInsert(Lnet/minecraft/world/item/ItemStack;)I", "tryTransfer(Lnet/minecraft/world/inventory/Slot;Lnet/minecraft/world/entity/player/Player;)I"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/component/BundleContents;canItemBeInBundle(Lnet/minecraft/world/item/ItemStack;)Z"), require = 2)
	private boolean add_canBeBundled(ItemStack item) {
		if (!item.isEmpty() && ServerMixinLink.NO_SLOT_RESTRICTIONS_BUNDLES
				.getOrDefault((BundleContents.Mutable) (Object) this, false)) {
			return true;
		}
		return BundleContents.canItemBeInBundle(item);
	}
}
