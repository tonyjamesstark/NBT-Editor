package com.luneruniverse.minecraft.mod.nbteditor.mixin.toggled;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.luneruniverse.minecraft.mod.nbteditor.misc.MixinLink;

import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

@Mixin(ItemModelResolver.class)
public class ItemModelManagerMixin {
	
	@Inject(method = "appendItemLayers(Lnet/minecraft/client/renderer/item/ItemStackRenderState;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/ItemOwner;I)V", at = @At("HEAD"))
	private void appendItemLayers(ItemStackRenderState renderState, ItemStack stack, ItemDisplayContext displayContext, Level world, LivingEntity entity, final int seed, CallbackInfo info) {
		MixinLink.ITEM_BEING_RENDERED.put(Thread.currentThread(), stack);
	}
	
}
