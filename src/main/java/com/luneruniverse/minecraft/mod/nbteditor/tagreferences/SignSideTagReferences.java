package com.luneruniverse.minecraft.mod.nbteditor.tagreferences;

import java.util.List;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.TextInst;
import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.general.ArraySplitTagReference;
import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.general.NBTTagReference;
import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.general.TagReference;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;

public class SignSideTagReferences {
	
	public static final TagReference<Boolean, NbtCompound> GLOWING = (new NBTTagReference<>(Boolean.class, "has_glowing_text"));
	
	public static final TagReference<String, NbtCompound> COLOR = (new NBTTagReference<>(String.class, "color"));
	
	public static final TagReference<List<Text>, NbtCompound> TEXT = TagReference.forLists(Text.class, new NBTTagReference<>(Text[].class, "messages"));
	
}
