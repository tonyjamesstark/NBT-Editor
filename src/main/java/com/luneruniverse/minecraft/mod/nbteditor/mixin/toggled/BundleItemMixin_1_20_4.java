package com.luneruniverse.minecraft.mod.nbteditor.mixin.toggled;

import java.util.WeakHashMap;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.luneruniverse.minecraft.mod.nbteditor.server.ServerMixinLink;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ClickAction;

@Mixin(BundleItem.class)
public class BundleItemMixin_1_20_4 {
	private static final WeakHashMap<Thread, Boolean> NO_SLOT_RESTRICTIONS_THREADS = new WeakHashMap<>();
	@Inject(method = "method_31565(Lnet/minecraft/class_1799;Lnet/minecraft/class_1735;Lnet/minecraft/class_5536;Lnet/minecraft/class_1657;)Z", at = @At("HEAD"), remap = false)
	private void onStackClicked(ItemStack stack, Slot slot, ClickAction clickType, Player player, CallbackInfoReturnable<Boolean> info) {
		NO_SLOT_RESTRICTIONS_THREADS.put(Thread.currentThread(), ServerMixinLink.isNoSlotRestrictions(player, false));
	}
	@Inject(method = "method_31566(Lnet/minecraft/class_1799;Lnet/minecraft/class_1799;Lnet/minecraft/class_1735;Lnet/minecraft/class_5536;Lnet/minecraft/class_1657;Lnet/minecraft/class_5630;)Z", at = @At("HEAD"), remap = false)
	private void onClicked(ItemStack stack, ItemStack otherStack, Slot slot, ClickAction clickType, Player player, SlotAccess cursorStackReference, CallbackInfoReturnable<Boolean> info) {
		NO_SLOT_RESTRICTIONS_THREADS.put(Thread.currentThread(), ServerMixinLink.isNoSlotRestrictions(player, false));
	}
	@Redirect(method = "method_31560(Lnet/minecraft/class_1799;Lnet/minecraft/class_1799;)I", at = @At(value = "INVOKE", target = "Lnet/minecraft/class_1792;method_31568()Z"), remap = false)
	private static boolean addToBundle_canBeNested(Item item) {
		if (NO_SLOT_RESTRICTIONS_THREADS.getOrDefault(Thread.currentThread(), false))
			return true;
		return item.canFitInsideContainerItems();
	}
}
