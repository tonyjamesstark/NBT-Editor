package com.luneruniverse.minecraft.mod.nbteditor.mixin;


import java.util.function.Predicate;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.luneruniverse.minecraft.mod.nbteditor.screens.FilterableTextField;
import com.luneruniverse.minecraft.mod.nbteditor.screens.Tickable;
import com.luneruniverse.minecraft.mod.nbteditor.screens.widgets.NamedTextFieldWidget;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.resources.Identifier;

@Mixin(EditBox.class)
public abstract class TextFieldWidgetMixin implements Tickable, FilterableTextField {
	private static final Identifier TEXT_FIELD_INVALID = Identifier.fromNamespaceAndPath("nbteditor", "widget/text_field_invalid");
	@ModifyArg(method = "extractWidgetRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V", ordinal = 0), index = 1)
	private Identifier drawGuiTexture(Identifier texture) {
		EditBox source = (EditBox) (Object) this;
		if (source instanceof NamedTextFieldWidget named && !named.isValid())
			return TEXT_FIELD_INVALID;
		return texture;
	}
	
	@Unique
	private Predicate<String> nbte$filter;
	@Override
	public void nbte$setFilter(Predicate<String> filter) {
		this.nbte$filter = filter;
	}
	// ponytail: a rejected keystroke still moves the cursor, since only the
	// field write is skipped. Reimplement insertText's splice here if that shows.
	@Redirect(method = {"setValue", "insertText"}, at = @At(value = "FIELD",
			target = "Lnet/minecraft/client/gui/components/EditBox;value:Ljava/lang/String;", opcode = Opcodes.PUTFIELD))
	private void nbte$filterValue(EditBox self, String value) {
		if (nbte$filter == null || nbte$filter.test(value))
			self.value = value;
	}
	
	@Override
	public void tick() {
		EditBox source = (EditBox) (Object) this;
	}
}
