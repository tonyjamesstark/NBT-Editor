package com.luneruniverse.minecraft.mod.nbteditor.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.luneruniverse.minecraft.mod.nbteditor.screens.containers.ClientHandledScreen;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.client.Minecraft;
import com.luneruniverse.minecraft.mod.nbteditor.util.PlayerItems;

@Mixin(AbstractContainerMenu.class)
public class ScreenHandlerMixin {
	@Redirect(method = "doClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/Player;dropItem(Lnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/entity/item/ItemEntity;"))
	private ItemEntity dropItem(Player player, ItemStack stack, boolean retainOwnership) {
		if (!(Minecraft.getInstance().gui.screen() instanceof ClientHandledScreen))
			return player.drop(stack, retainOwnership);
		
		PlayerItems.dropCreativeStack(stack);
		return null;
	}
}
