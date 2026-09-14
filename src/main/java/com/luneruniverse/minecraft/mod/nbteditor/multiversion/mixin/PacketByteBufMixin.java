package com.luneruniverse.minecraft.mod.nbteditor.multiversion.mixin;


import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.DynamicRegistryManagerHolder;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.IdentifierInst;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVPacketByteBufParent;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.Reflection;
import com.luneruniverse.minecraft.mod.nbteditor.server.ServerMVMisc;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

@Mixin(FriendlyByteBuf.class)
public abstract class PacketByteBufMixin implements MVPacketByteBufParent {
	
	@Shadow
	private ByteBuf source;
	@Shadow
	public abstract String readUtf();
	@Shadow
	public abstract FriendlyByteBuf writeUtf(String str);
	@Shadow
	public abstract double readDouble();
	
	@Override
	public FriendlyByteBuf writeBoolean(boolean value) {
		source.writeBoolean(value);
		return (FriendlyByteBuf) (Object) this;
	}
	
	@Override
	public FriendlyByteBuf writeDouble(double value) {
		source.writeDouble(value);
		return (FriendlyByteBuf) (Object) this;
	}
	
	@Override
	public Identifier readIdentifier() {
		return IdentifierInst.of(readUtf());
	}
	@Override
	public FriendlyByteBuf writeIdentifier(Identifier id) {
		return writeUtf(id.toString());
	}
	
	@Override
	public <T> ResourceKey<T> readRegistryKey(ResourceKey<? extends Registry<T>> registryRef) {
		return ResourceKey.create(registryRef, readIdentifier());
	}
	@Override
	public void writeRegistryKey(ResourceKey<?> key) {
		writeIdentifier(key.identifier());
	}
	
	@Override
	public FriendlyByteBuf writeNbtCompound(CompoundTag element) {
		return ((FriendlyByteBuf) (Object) this).writeNbt(element);
	}
	
	@Override
	public Vec3 readVec3d() {
		return new Vec3(readDouble(), readDouble(), readDouble());
	}
	@Override
	public void writeVec3d(Vec3 vector) {
		writeDouble(vector.x());
		writeDouble(vector.y());
		writeDouble(vector.z());
	}
	
	@Override
	public ItemStack readItemStack() {
		return ServerMVMisc.packetCodecDecode(ItemStack.OPTIONAL_STREAM_CODEC, createRegistryByteBuf());
	}
	@Override
	public FriendlyByteBuf writeItemStack(ItemStack item) {
		ServerMVMisc.packetCodecEncode(ItemStack.OPTIONAL_STREAM_CODEC, createRegistryByteBuf(), item);
		return (FriendlyByteBuf) (Object) this;
	}
	
	private Object createRegistryByteBuf() {
		return new RegistryFriendlyByteBuf(source, DynamicRegistryManagerHolder.getManager());
	}
	
}
