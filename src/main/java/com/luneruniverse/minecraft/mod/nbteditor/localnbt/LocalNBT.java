package com.luneruniverse.minecraft.mod.nbteditor.localnbt;

import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;


import org.joml.Quaternionf;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public interface LocalNBT {
	public static Optional<LocalNBT> deserialize(CompoundTag nbt, int defaultDataVersion) {
		return Optional.ofNullable(switch (nbt.nbte$getString("type").orElse("item")) {
			case "item" -> LocalItemStack.deserialize(nbt, defaultDataVersion);
			case "block" -> LocalBlock.deserialize(nbt, defaultDataVersion);
			case "entity" -> LocalEntity.deserialize(nbt, defaultDataVersion);
			default -> null;
		});
	}
	
	@SuppressWarnings("unchecked")
	public static <T extends LocalNBT> T copy(T localNBT) {
		return (T) localNBT.copy();
	}
	
	/** The spin every 3D preview icon shares, as a rotation the GUI can queue. */
	public static Quaternionf iconSpin() {
		float yaw = (float) (System.currentTimeMillis() % 2000 / 2000.0f * Math.PI * 2);
		return new Quaternionf().rotateX((float) (-Math.PI / 6)).rotateY(yaw);
	}
	
	public default boolean isEmpty() {
		return isEmpty(getId());
	}
	public boolean isEmpty(Identifier id);
	
	public Component getName();
	public void setName(Component name);
	public String getDefaultName();
	
	public Identifier getId();
	public void setId(Identifier id);
	public Set<Identifier> getIdOptions();
	
	public CompoundTag getNBT();
	public void setNBT(CompoundTag nbt);
	public default CompoundTag getOrCreateNBT() {
		CompoundTag nbt = getNBT();
		if (nbt == null) {
			nbt = new CompoundTag();
			setNBT(nbt);
		}
		return nbt;
	}
	public default void modifyNBT(Consumer<CompoundTag> modifier) {
		CompoundTag nbt = getNBT();
		if (nbt == null)
			nbt = new CompoundTag();
		modifier.accept(nbt);
		setNBT(nbt);
	}
	
	public void renderIcon(GuiGraphics context, int x, int y, float tickDelta);
	
	public Optional<ItemStack> toItem(boolean cleanup);
	public CompoundTag serialize();
	public Component toHoverableText();
	
	public LocalNBT copy();
	@Override
	public boolean equals(Object nbt);
}
