package com.luneruniverse.minecraft.mod.nbteditor.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.nbt.StringTagVisitor;

@Mixin(StringTagVisitor.class)
public interface StringNbtWriterAccessor {
    @Accessor
    StringBuilder getResult();
}
