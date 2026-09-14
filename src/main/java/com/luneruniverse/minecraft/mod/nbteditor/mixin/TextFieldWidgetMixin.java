package com.luneruniverse.minecraft.mod.nbteditor.mixin;


import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.IdentifierInst;
import com.luneruniverse.minecraft.mod.nbteditor.screens.Tickable;
import com.luneruniverse.minecraft.mod.nbteditor.screens.widgets.NamedTextFieldWidget;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.resources.Identifier;

@Mixin(EditBox.class)
public abstract class TextFieldWidgetMixin implements Tickable {
	private static final Identifier TEXT_FIELD_INVALID = IdentifierInst.of("nbteditor", "widget/text_field_invalid");
	@ModifyArg(method = "renderWidget", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V", ordinal = 0), index = 1)
	private Identifier drawGuiTexture(Identifier texture) {
		EditBox source = (EditBox) (Object) this;
		if (source instanceof NamedTextFieldWidget named && !named.isValid())
			return TEXT_FIELD_INVALID;
		return texture;
	}
	
	@Override
	public void tick() {
		EditBox source = (EditBox) (Object) this;
	}
}
