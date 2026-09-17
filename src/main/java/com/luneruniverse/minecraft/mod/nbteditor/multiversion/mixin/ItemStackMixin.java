package com.luneruniverse.minecraft.mod.nbteditor.multiversion.mixin;

import java.lang.invoke.MethodType;
import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Mixin;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVComponentType;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.Reflection;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.nbt.MVItemStackParent;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.nbt.manager.IntegratedNBTManager;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.nbt.manager.NBTManagers;

import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

@Mixin(ItemStack.class)
public class ItemStackMixin implements IntegratedNBTManager, MVItemStackParent {
	@Override
	public CompoundTag nbte$serialize(boolean requireSuccess) {
		return NBTManagers.ITEM.serialize((ItemStack) (Object) this, requireSuccess);
	}
	
	@Override
	public boolean nbte$hasNbt() {
		return NBTManagers.ITEM.hasNbt((ItemStack) (Object) this);
	}
	@Override
	public CompoundTag nbte$getNbt() {
		return NBTManagers.ITEM.getNbt((ItemStack) (Object) this);
	}
	@Override
	public CompoundTag nbte$getOrCreateNbt() {
		return NBTManagers.ITEM.getOrCreateNbt((ItemStack) (Object) this);
	}
	@Override
	public void nbte$setNbt(CompoundTag nbt) {
		NBTManagers.ITEM.setNbt((ItemStack) (Object) this, nbt);
	}
	
	
	@Override
	public boolean nbte$hasCustomName() {
		return ((ItemStack) (Object) this).contains(MVComponentType.CUSTOM_NAME);
	}
	@Override
	public ItemStack nbte$setCustomName(Component name) {
		((ItemStack) (Object) this).set(MVComponentType.CUSTOM_NAME, name);
		return (ItemStack) (Object) this;
	}
}
