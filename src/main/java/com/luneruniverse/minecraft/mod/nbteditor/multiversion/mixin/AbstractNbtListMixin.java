package com.luneruniverse.minecraft.mod.nbteditor.multiversion.mixin;

import java.util.Optional;
import java.util.stream.Stream;

import org.spongepowered.asm.mixin.Mixin;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.nbt.MVAbstractNbtListParent;

import net.minecraft.nbt.CollectionTag;
import net.minecraft.nbt.Tag;

@Mixin(CollectionTag.class)
public interface AbstractNbtListMixin extends MVAbstractNbtListParent {
	
	public default Optional<Byte> nbte$getHeldType() {
		byte heldType = (byte) 0;
		for (Tag element : (CollectionTag) (Object) this) {
			if (heldType == 0)
				heldType = element.getId();
			else if (heldType != element.getId())
				return Optional.empty();
		}
		return Optional.of(heldType);
	}
	
	public default int nbte$size() {
		return ((CollectionTag) (Object) this).size();
	}
	
	public default boolean nbte$isEmpty() {
		return nbte$size() == 0;
	}
	
	@SuppressWarnings("unchecked")
	public default Iterable<Tag> nbte$iterable() {
		return (Iterable<Tag>) this;
	}
	
	@SuppressWarnings("unchecked")
	public default Stream<Tag> nbte$stream() {
		return ((CollectionTag) (Object) this).stream();
	}
	
	public default Tag nbte$get(int index) {
		return ((CollectionTag) (Object) this).get(index);
	}
	
	public default void nbte$add(int index, Tag element) {
		((CollectionTag) (Object) this).addTag(index, element);
	}
	public default void nbte$add(Tag element) {
		nbte$add(nbte$size(), element);
	}
	
	public default void nbte$set(int index, Tag element) {
		((CollectionTag) (Object) this).setTag(index, element);
	}
	
	public default Tag nbte$remove(int index) {
		return ((CollectionTag) (Object) this).remove(index);
	}
	
	public default void nbte$clear() {
		((CollectionTag) (Object) this).clear();
	}
	
}
