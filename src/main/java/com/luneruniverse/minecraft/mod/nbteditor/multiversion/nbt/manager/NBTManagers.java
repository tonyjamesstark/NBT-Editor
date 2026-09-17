package com.luneruniverse.minecraft.mod.nbteditor.multiversion.nbt.manager;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.nbt.manager.components.ComponentBlockEntityNBTManager;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.nbt.manager.components.ComponentEntityNBTManager;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.nbt.manager.components.ComponentItemNBTManager;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;

public class NBTManagers {
	/** Components landed in 1.20.5, below the supported floor. */
	public static final boolean COMPONENTS_EXIST = true;
	public static final DeserializableNBTManager<ItemStack> ITEM = new ComponentItemNBTManager();
	public static final NBTManager<BlockEntity> BLOCK_ENTITY = new ComponentBlockEntityNBTManager();
	public static final NBTManager<Entity> ENTITY = new ComponentEntityNBTManager();
}
