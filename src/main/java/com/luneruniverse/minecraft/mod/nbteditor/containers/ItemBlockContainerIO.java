package com.luneruniverse.minecraft.mod.nbteditor.containers;

import com.luneruniverse.minecraft.mod.nbteditor.localnbt.LocalBlock;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;

public record ItemBlockContainerIO(ContainerIO<ItemStack> item, ContainerIO<LocalBlock> block) {
	
	public static ItemBlockContainerIO forBlockEntityTagIO(ContainerIO<CompoundTag> io, String entityId) {
		return new ItemBlockContainerIO(ContainerIO.forItemStackBlockEntityTag(io, entityId), ContainerIO.forLocalNBT(io));
	}
	public static ItemBlockContainerIO forBlockEntityTagIO(ContainerIO<CompoundTag> io, BlockEntityType<?> entityId) {
		return forBlockEntityTagIO(io, BlockEntityType.getKey(entityId).toString());
	}
	
	public static ItemBlockContainerIO forSlotKeyItems(int numSlots) {
		return new ItemBlockContainerIO(
				new ContainerComponentContainerIO(numSlots),
				ContainerIO.forLocalNBT(new SlotKeyNbtListContainerIO(numSlots).forNbtCompoundItems()));
	}
	
	public static ItemBlockContainerIO forKeys(String entityId, String... keys) {
		return new ItemBlockContainerIO(
				ContainerIO.forItemStackBlockEntityTag(new KeysContainerIO(true, keys), entityId),
				ContainerIO.forLocalNBT(new KeysContainerIO(false, keys)));
	}
	public static ItemBlockContainerIO forKeys(BlockEntityType<?> entityId, String... keys) {
		return forKeys(BlockEntityType.getKey(entityId).toString(), keys);
	}
	
	public ItemBlockContainerIO withTextures(Identifier... textures) {
		return new ItemBlockContainerIO(item.withTextures(textures), block.withTextures(textures));
	}
	
}
