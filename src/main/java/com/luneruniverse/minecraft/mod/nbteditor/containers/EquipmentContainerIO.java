package com.luneruniverse.minecraft.mod.nbteditor.containers;

import static com.luneruniverse.minecraft.mod.nbteditor.containers.ContainerIO.BOOTS_TEXTURE;
import static com.luneruniverse.minecraft.mod.nbteditor.containers.ContainerIO.CHESTPLATE_TEXTURE;
import static com.luneruniverse.minecraft.mod.nbteditor.containers.ContainerIO.HELMET_TEXTURE;
import static com.luneruniverse.minecraft.mod.nbteditor.containers.ContainerIO.HORSE_ARMOR_TEXTURE;
import static com.luneruniverse.minecraft.mod.nbteditor.containers.ContainerIO.LEGGINGS_TEXTURE;
import static com.luneruniverse.minecraft.mod.nbteditor.containers.ContainerIO.LLAMA_ARMOR_TEXTURE;
import static com.luneruniverse.minecraft.mod.nbteditor.containers.ContainerIO.SADDLE_TEXTURE;
import static com.luneruniverse.minecraft.mod.nbteditor.containers.ContainerIO.SHIELD_TEXTURE;
import static com.luneruniverse.minecraft.mod.nbteditor.containers.ContainerIO.SWORD_TEXTURE;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;

/**
 * The eight equipment slots an entity wears, held in its {@code equipment} compound.
 *
 * <p>Slot addressing is nothing more than a fixed set of key names, so the io is a
 * {@link KeysContainerIO} over those names. What this class contributes is the names themselves
 * and the textures that tell the player which empty slot is which.
 */
public class EquipmentContainerIO {
	
	/** The names {@link net.minecraft.world.entity.EquipmentSlot} serializes to, in texture order. */
	private static final String[] KEYS = {
			"head", "chest", "legs", "feet", "saddle", "body", "mainhand", "offhand"};
	
	private static final Identifier[] HORSE_ARMOR_TEXTURES = {
			HELMET_TEXTURE, CHESTPLATE_TEXTURE, LEGGINGS_TEXTURE, BOOTS_TEXTURE,
			SADDLE_TEXTURE, HORSE_ARMOR_TEXTURE, SWORD_TEXTURE, SHIELD_TEXTURE};
	private static final Identifier[] LLAMA_ARMOR_TEXTURES = {
			HELMET_TEXTURE, CHESTPLATE_TEXTURE, LEGGINGS_TEXTURE, BOOTS_TEXTURE,
			SADDLE_TEXTURE, LLAMA_ARMOR_TEXTURE, SWORD_TEXTURE, SHIELD_TEXTURE};
	
	/**
	 * @param llama Picks the body-slot texture; a llama wears a carpet where a horse wears armour
	 * @return An io over the <code>equipment</code> compound of an entity's nbt
	 */
	public static ContainerIO<CompoundTag> forNbtCompoundEquipment(boolean llama) {
		ContainerIO<CompoundTag> equipment = new KeysContainerIO(true, KEYS)
				.withTextures(llama ? LLAMA_ARMOR_TEXTURES : HORSE_ARMOR_TEXTURES);
		return DelegateContainerIO.map(equipment,
				nbt -> nbt.nbte$getCompoundOrDefault("equipment"), (nbt, slots) -> nbt.put("equipment", slots));
	}
	
	private EquipmentContainerIO() {}
	
}
