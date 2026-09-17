package com.luneruniverse.minecraft.mod.nbteditor.tagreferences;

import java.util.Optional;

import com.luneruniverse.minecraft.mod.nbteditor.localnbt.LocalBlock;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.Version;
import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.general.NBTComponentTagReference;
import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.general.TagReference;
import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.specific.GameProfileNBTTagReference;
import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.specific.GameProfileNameNBTTagReference;
import com.mojang.authlib.GameProfile;

import net.minecraft.component.type.ProfileComponent;

public class BlockTagReferences {
	
	public static final TagReference<Optional<String>, LocalBlock> PROFILE_NAME = Version.<TagReference<Optional<String>, LocalBlock>>newSwitch()
			.range("1.20.5", null, () -> TagReference.forLocalNBT(Optional::empty,
					new NBTComponentTagReference<>("profile", ProfileComponent.CODEC, Optional::empty,
							ProfileComponent::getName,
							name -> name.map(ProfileComponent::ofDynamic).orElse(null))))
			.get();
	public static final TagReference<Optional<GameProfile>, LocalBlock> PROFILE = Version.<TagReference<Optional<GameProfile>, LocalBlock>>newSwitch()
			.range("1.20.5", null, () -> TagReference.forLocalNBT(Optional::empty,
					new NBTComponentTagReference<>("profile", ProfileComponent.CODEC, Optional::empty,
							profile -> Optional.ofNullable(profile.getGameProfile()),
							profile -> profile.map(ProfileComponent::ofStatic).orElse(null))))
			.get();
	
}
