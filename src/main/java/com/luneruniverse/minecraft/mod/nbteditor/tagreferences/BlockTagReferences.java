package com.luneruniverse.minecraft.mod.nbteditor.tagreferences;

import java.util.Optional;

import com.luneruniverse.minecraft.mod.nbteditor.localnbt.LocalBlock;
import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.general.NBTComponentTagReference;
import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.general.TagReference;
import com.mojang.authlib.GameProfile;

import net.minecraft.world.item.component.ResolvableProfile;

public class BlockTagReferences {
	
	public static final TagReference<Optional<String>, LocalBlock> PROFILE_NAME = TagReference.forLocalNBT(Optional::empty,
					new NBTComponentTagReference<>("profile", ResolvableProfile.CODEC, Optional::empty,
							ResolvableProfile::name,
							name -> name.map(ResolvableProfile::createUnresolved).orElse(null)));
	public static final TagReference<Optional<GameProfile>, LocalBlock> PROFILE = TagReference.forLocalNBT(Optional::empty,
					new NBTComponentTagReference<>("profile", ResolvableProfile.CODEC, Optional::empty,
							profile -> Optional.ofNullable(profile.partialProfile()),
							profile -> profile.map(ResolvableProfile::createResolved).orElse(null)));
	
}
