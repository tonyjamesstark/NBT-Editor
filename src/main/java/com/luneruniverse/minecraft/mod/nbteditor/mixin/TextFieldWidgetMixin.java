package com.luneruniverse.minecraft.mod.nbteditor.mixin;


import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.IdentifierInst;
import com.luneruniverse.minecraft.mod.nbteditor.screens.Tickable;
import com.luneruniverse.minecraft.mod.nbteditor.screens.widgets.NamedTextFieldWidget;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.resources.Identifier;

@Mixin(EditBox.class)
public abstract class TextFieldWidgetMixin implements Tickable {
	private static final Identifier TEXT_FIELD_INVALID = IdentifierInst.of("nbteditor", "widget/text_field_invalid");
	@ModifyArg(method = "renderWidget", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Ljava/util/function/Function;Lnet/minecraft/util/Identifier;IIII)V", ordinal = 0))
	@Group(name = "renderButton", min = 1)
	private Identifier drawGuiTexture2(Identifier texture) {
		EditBox source = (EditBox) (Object) this;
		if (source instanceof NamedTextFieldWidget named && !named.isValid())
			return TEXT_FIELD_INVALID;
		return texture;
	}
	@ModifyArg(method = "method_48579", at = @At(value = "INVOKE", target = "Lnet/minecraft/class_332;method_52706(Lnet/minecraft/class_2960;IIII)V", ordinal = 0), remap = false)
	@Group(name = "renderButton", min = 1)
	private Identifier drawGuiTexture1(Identifier texture) {
		EditBox source = (EditBox) (Object) this;
		if (source instanceof NamedTextFieldWidget named && !named.isValid())
			return TEXT_FIELD_INVALID;
		return texture;
	}
	@ModifyArg(method = "method_48579", at = @At(value = "INVOKE", target = "Lnet/minecraft/class_332;method_25294(IIIII)V", ordinal = 0), index = 4, remap = false)
	@Group(name = "renderButton", min = 1)
	private int fillDrawContext(int color) {
		EditBox source = (EditBox) (Object) this;
		if (source instanceof NamedTextFieldWidget named && !named.isValid())
			return 0xFFDF4949;
		return color;
	}
	@ModifyArg(method = {"method_48579", "method_25359"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/class_342;method_25294(Lnet/minecraft/class_4587;IIIII)V", ordinal = 0), index = 5, remap = false)
	@Group(name = "renderButton", min = 1)
	private int fillMatrixStack(int color) {
		EditBox source = (EditBox) (Object) this;
		if (source instanceof NamedTextFieldWidget named && !named.isValid())
			return 0xFFDF4949;
		return color;
	}
	
	@Override
	public void tick() {
		EditBox source = (EditBox) (Object) this;
	}
}
