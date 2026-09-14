package com.luneruniverse.minecraft.mod.nbteditor.tagreferences.specific;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.IdentifierInst;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVRegistry;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.nbt.MVNbtCompoundParent;
import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.general.TagReference;
import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.specific.data.AttributeData;
import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.specific.data.AttributeData.AttributeModifierData.AttributeModifierId;
import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.specific.data.AttributeData.AttributeModifierData.Operation;
import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.specific.data.AttributeData.AttributeModifierData.Slot;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.ListTag;

public class AttributesNBTTagReference implements TagReference<List<AttributeData>, CompoundTag> {
	
	public enum NBTLayout {
		/**
		 * 1.20.4-
		 */
		ITEM_OLD(true, "AttributeModifiers", "AttributeName", "Amount"),
		/**
		 * 1.20.6-
		 */
		ENTITY_OLD(false, "Attributes", "Name", "Base"),
		/**
		 * 1.21+
		 */
		ENTITY_NEW(false, "attributes", "id", "base");
		
		private final boolean modifiers;
		private final String attributeListTag;
		private final String attributeNameTag;
		private final String amountTag;
		
		private NBTLayout(boolean modifiers, String attributeListTag, String attributeNameTag, String amountTag) {
			this.modifiers = modifiers;
			this.attributeListTag = attributeListTag;
			this.attributeNameTag = attributeNameTag;
			this.amountTag = amountTag;
		}
		
		public boolean isModifiers() {
			return modifiers;
		}
		public String getAttributeListTag() {
			return attributeListTag;
		}
		public String getAttributeNameTag() {
			return attributeNameTag;
		}
		public String getAmountTag() {
			return amountTag;
		}
	}
	
	private final NBTLayout layout;
	
	public AttributesNBTTagReference(NBTLayout layout) {
		this.layout = layout;
	}
	
	@Override
	public List<AttributeData> get(CompoundTag object) {
		ListTag attributesNbt = object.nbte$getListOrDefault(layout.getAttributeListTag(), Tag.TAG_COMPOUND);
		List<AttributeData> output = new ArrayList<>();
		for (Tag attributeNbtElement : attributesNbt.nbte$iterable()) {
			CompoundTag attributeNbt = (CompoundTag) attributeNbtElement;
			
			Attribute attribute = attributeNbt.nbte$getString(layout.getAttributeNameTag())
					.map(IdentifierInst::of).map(MVRegistry.ATTRIBUTE::get).orElse(null);
			if (attribute == null)
				continue;
			
			if (!attributeNbt.nbte$contains(layout.getAmountTag(), MVNbtCompoundParent.NUMBER_TYPE))
				continue;
			double value = attributeNbt.nbte$getDoubleOrDefault(layout.getAmountTag());
			
			if (layout.isModifiers()) {
				if (!attributeNbt.nbte$contains("Operation", MVNbtCompoundParent.NUMBER_TYPE))
					continue;
				int operation = attributeNbt.nbte$getIntOrDefault("Operation");
				if (operation < 0 || operation >= Operation.values().length)
					continue;
				
				Slot slot = Slot.ANY;
				if (attributeNbt.nbte$contains("Slot", Tag.TAG_STRING)) {
					try {
						slot = Slot.valueOf(attributeNbt.nbte$getStringOrDefault("Slot").toUpperCase());
					} catch (IllegalArgumentException e) {
						continue;
					}
				}
				
				if (!attributeNbt.nbte$containsUuid("UUID"))
					continue;
				UUID uuid = attributeNbt.nbte$getUuid("UUID").get();
				
				output.add(new AttributeData(attribute, value, Operation.values()[operation], slot, new AttributeModifierId(uuid)));
			} else
				output.add(new AttributeData(attribute, value));
		}
		return output;
	}
	
	@Override
	public void set(CompoundTag object, List<AttributeData> value) {
		if (value.isEmpty()) {
			object.remove(layout.getAttributeListTag());
			return;
		}
		ListTag output = new ListTag();
		for (AttributeData attribute : value) {
			CompoundTag attributeNbt = new CompoundTag();
			
			attributeNbt.putString(layout.getAttributeNameTag(), MVRegistry.ATTRIBUTE.getId(attribute.attribute()).toString());
			attributeNbt.putDouble(layout.getAmountTag(), attribute.value());
			
			if (layout.isModifiers()) {
				attributeNbt.putString("Name", attributeNbt.nbte$getStringOrDefault("AttributeName"));
				attributeNbt.putInt("Operation", attribute.modifierData().get().operation().ordinal());
				if (attribute.modifierData().get().slot() != Slot.ANY) {
					attributeNbt.putString("Slot", attribute.modifierData().get().slot().name().toLowerCase());
				}
				attributeNbt.nbte$putUuid("UUID", attribute.modifierData().get().id().getUUID());
			}
			
			output.add(attributeNbt);
		}
		object.put(layout.getAttributeListTag(), output);
	}
	
}
