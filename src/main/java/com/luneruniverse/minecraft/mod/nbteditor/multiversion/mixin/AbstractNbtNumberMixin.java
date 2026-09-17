package com.luneruniverse.minecraft.mod.nbteditor.multiversion.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.nbt.MVAbstractNbtNumberParent;

import net.minecraft.nbt.NumericTag;

@Mixin(NumericTag.class)
public interface AbstractNbtNumberMixin extends MVAbstractNbtNumberParent {
	
	public default byte nbte$byteValue() {
		return ((NumericTag) (Object) this).byteValue();
	}
	
	public default short nbte$shortValue() {
		return ((NumericTag) (Object) this).shortValue();
	}
	
	public default int nbte$intValue() {
		return ((NumericTag) (Object) this).intValue();
	}
	
	public default long nbte$longValue() {
		return ((NumericTag) (Object) this).longValue();
	}
	
	public default float nbte$floatValue() {
		return ((NumericTag) (Object) this).floatValue();
	}
	
	public default double nbte$doubleValue() {
		return ((NumericTag) (Object) this).doubleValue();
	}
	
	public default Number nbte$numberValue() {
		return ((NumericTag) (Object) this).box();
	}
	
}
