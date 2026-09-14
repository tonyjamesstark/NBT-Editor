package com.luneruniverse.minecraft.mod.nbteditor.mixin.toggled;

import java.util.stream.Stream;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.DynamicRegistryManagerHolder;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.RegistryCache;

import net.minecraft.resources.ResourceKey;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderOwner;
import net.minecraft.tags.TagKey;

@Mixin(Holder.Reference.class)
public abstract class RegistryEntryReferenceMixin<T> {
	
	@Shadow
	public abstract ResourceKey<T> registryKey();
	
	@Inject(method = "value", at = @At("HEAD"), cancellable = true)
	private void value(CallbackInfoReturnable<T> info) {
		@SuppressWarnings("unchecked")
		Holder.Reference<T> source = (Holder.Reference<T>) (Object) this;
		
		if (DynamicRegistryManagerHolder.hasClientManager() && DynamicRegistryManagerHolder.isOwnedByDefaultManager(source)) {
			Holder.Reference<T> convertedRef = RegistryCache.convertManagerWithCache(source);
			if (convertedRef != null)
				info.setReturnValue(convertedRef.value());
		}
	}
	
	@Inject(method = "isIn", at = @At("HEAD"), cancellable = true)
	private void isIn(TagKey<T> tag, CallbackInfoReturnable<Boolean> info) {
		@SuppressWarnings("unchecked")
		Holder.Reference<T> source = (Holder.Reference<T>) (Object) this;
		
		if (DynamicRegistryManagerHolder.hasClientManager() && DynamicRegistryManagerHolder.isOwnedByDefaultManager(source)) {
			Holder.Reference<T> convertedRef = RegistryCache.convertManagerWithCache(source);
			if (convertedRef != null)
				info.setReturnValue(convertedRef.is(tag));
		}
	}
	
	@Inject(method = "ownerEquals", at = @At("RETURN"), cancellable = true)
	private void ownerEquals(HolderOwner<?> owner, CallbackInfoReturnable<Boolean> info) {
		if (!info.getReturnValueZ()) {
			if (DynamicRegistryManagerHolder.isOwnedByDefaultManager((Holder.Reference<?>) (Object) this))
				info.setReturnValue(true);
		}
	}
	
	@Inject(method = "streamTags", at = @At("HEAD"), cancellable = true)
	private void streamTags(CallbackInfoReturnable<Stream<TagKey<T>>> info) {
		@SuppressWarnings("unchecked")
		Holder.Reference<T> source = (Holder.Reference<T>) (Object) this;
		
		if (DynamicRegistryManagerHolder.hasClientManager() && DynamicRegistryManagerHolder.isOwnedByDefaultManager(source)) {
			Holder.Reference<T> convertedRef = RegistryCache.convertManagerWithCache(source);
			if (convertedRef != null)
				info.setReturnValue(convertedRef.tags());
		}
	}
	
	@Override
	public boolean equals(Object obj) {
		if (super.equals(obj))
			return true;
		
		if (obj instanceof Holder.Reference<?> ref &&
				(DynamicRegistryManagerHolder.isOwnedByDefaultManager((Holder.Reference<?>) (Object) this) ||
						DynamicRegistryManagerHolder.isOwnedByDefaultManager(ref))) {
			return registryKey().registry().equals(ref.key().registry()) &&
					registryKey().identifier().equals(ref.key().identifier());
		}
		
		return false;
	}
	
	@Override
	public int hashCode() {
		return 31 * registryKey().registry().hashCode() + registryKey().identifier().hashCode();
	}
	
}
