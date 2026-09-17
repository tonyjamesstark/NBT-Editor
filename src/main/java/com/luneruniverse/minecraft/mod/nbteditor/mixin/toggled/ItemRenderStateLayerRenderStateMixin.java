package com.luneruniverse.minecraft.mod.nbteditor.mixin.toggled;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.luneruniverse.minecraft.mod.nbteditor.misc.MixinLink;
import com.luneruniverse.minecraft.mod.nbteditor.screens.ConfigScreen;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState.FoilType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

@Mixin(ItemStackRenderState.LayerRenderState.class)
public class ItemRenderStateLayerRenderStateMixin {
	
	@Shadow
	private ItemStackRenderState.FoilType foilType;
	
	@ModifyArg(method = "submit", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/special/SpecialModelRenderer;submit(Ljava/lang/Object;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/submit/MultiBufferSource;IIZ)V"))
	private MultiBufferSource submit(MultiBufferSource provider) {
		ItemStack item = MixinLink.ITEM_BEING_RENDERED.remove(Thread.currentThread());
		if (item == null)
			return provider;
		
		if (!(item.getItem() instanceof BlockItem) ||
				(!MixinLink.ENCHANT_GLINT_FIX.contains(item) && !ConfigScreen.isEnchantGlintFix()))
			return provider;
		return layer -> ItemRenderer.getFoilBuffer(provider, layer, true, foilType != FoilType.NONE);
	}
	
}
