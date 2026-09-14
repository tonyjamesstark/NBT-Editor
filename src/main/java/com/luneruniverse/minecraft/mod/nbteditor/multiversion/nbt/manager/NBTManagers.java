package com.luneruniverse.minecraft.mod.nbteditor.multiversion.nbt.manager;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.nbt.manager.components.ComponentBlockEntityNBTManager;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.nbt.manager.components.ComponentEntityNBTManager;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.nbt.manager.components.ComponentItemNBTManager;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

public class NBTManagers {
	public static final DeserializableNBTManager<ItemStack> ITEM = new ComponentItemNBTManager();
	public static final NBTManager<BlockEntity> BLOCK_ENTITY = new ComponentBlockEntityNBTManager();
	public static final NBTManager<Entity> ENTITY = new ComponentEntityNBTManager();
}
