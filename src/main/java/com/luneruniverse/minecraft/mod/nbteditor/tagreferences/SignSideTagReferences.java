package com.luneruniverse.minecraft.mod.nbteditor.tagreferences;

import java.util.List;

import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.general.NBTTagReference;
import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.general.TagReference;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

public class SignSideTagReferences {
	
	public static final TagReference<Boolean, CompoundTag> GLOWING = (new NBTTagReference<>(Boolean.class, "has_glowing_text"));
	
	public static final TagReference<String, CompoundTag> COLOR = (new NBTTagReference<>(String.class, "color"));
	
	public static final TagReference<List<Component>, CompoundTag> TEXT = TagReference.forLists(Component.class, new NBTTagReference<>(Component[].class, "messages"));
	
}
