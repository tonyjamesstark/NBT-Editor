package com.luneruniverse.minecraft.mod.nbteditor.mixin.toggled;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.luneruniverse.minecraft.mod.nbteditor.misc.MixinLink;
import com.luneruniverse.minecraft.mod.nbteditor.screens.ConfigScreen;
import com.luneruniverse.minecraft.mod.nbteditor.util.NbtFormatter;
import com.mojang.serialization.DynamicOps;

import net.minecraft.nbt.SnbtGrammar;

@Mixin(SnbtGrammar.class)
public class SnbtParsingMixin {
	// The unquoted-string rule, where a bare token becomes a string value, beside
	// vanilla's own true/false cases. It is a lambda, so the only name it has is the
	// one javac assigned, and that index moves whenever createParser gains or loses a
	// lambda above it. checkMixinTargets fails the build when it does. An intermediary
	// name is not an option here: the jar ships no refMap, so mixin never resolves one.
	@Redirect(method = "lambda$createParser$13", at = @At(value = "INVOKE", target = "Lcom/mojang/serialization/DynamicOps;createString(Ljava/lang/String;)Ljava/lang/Object;", remap = false))
	private static Object unquotedString_createString(DynamicOps<?> ops, String str) {
		if (ConfigScreen.isSpecialNumbers() && MixinLink.specialNumbers.contains(Thread.currentThread())) {
			Number specialNum = NbtFormatter.SPECIAL_NUMS.get(str);
			if (specialNum != null) {
				if (specialNum instanceof Double d)
					return ops.createDouble(d);
				else if (specialNum instanceof Float f)
					return ops.createFloat(f);
				else
					throw new IllegalStateException("Number of invalid type: " + specialNum.getClass().getName());
			}
		}
		return ops.createString(str);
	}
}
