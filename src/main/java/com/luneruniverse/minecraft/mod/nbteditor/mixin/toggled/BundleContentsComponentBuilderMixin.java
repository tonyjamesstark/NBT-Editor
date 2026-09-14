package com.luneruniverse.minecraft.mod.nbteditor.mixin.toggled;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.luneruniverse.minecraft.mod.nbteditor.server.ServerMixinLink;

import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

@Mixin(BundleContents.Mutable.class)
public class BundleContentsComponentBuilderMixin {
	@Redirect(method = {"add(Lnet/minecraft/item/ItemStack;)I", "add(Lnet/minecraft/screen/slot/Slot;Lnet/minecraft/entity/player/PlayerEntity;)I"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/component/type/BundleContents;canBeBundled(Lnet/minecraft/item/ItemStack;)Z"), require = 2)
	@Group(name = "add", min = 1)
	private boolean add_canBeBundled(ItemStack item) {
		if (!item.isEmpty() && ServerMixinLink.NO_SLOT_RESTRICTIONS_BUNDLES
				.getOrDefault((BundleContents.Mutable) (Object) this, false)) {
			return true;
		}
		return BundleContents.canItemBeInBundle(item);
	}
	@Redirect(method = "method_57432(Lnet/minecraft/class_1799;)I", at = @At(value = "INVOKE", target = "Lnet/minecraft/class_1792;method_31568()Z"), remap = false)
	@Group(name = "add", min = 1)
	private boolean add_canBeNested(Item item) {
		if (ServerMixinLink.NO_SLOT_RESTRICTIONS_BUNDLES.getOrDefault((BundleContents.Mutable) (Object) this, false))
			return true;
		return item.canFitInsideContainerItems();
	}
}
