package com.luneruniverse.minecraft.mod.nbteditor.multiversion.mixin;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.spongepowered.asm.mixin.Mixin;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.nbt.MVNbtCompoundParent;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.ListTag;
import net.minecraft.core.UUIDUtil;

@Mixin(CompoundTag.class)
public class NbtCompoundMixin implements MVNbtCompoundParent {
	
	public boolean nbte$contains(String key, byte type) {
		Tag value = ((CompoundTag) (Object) this).get(key);
		return value != null && (value.getId() == type || type == NUMBER_TYPE &&
				value.getId() >= Tag.TAG_BYTE && value.getId() <= Tag.TAG_DOUBLE);
	}
	
	public boolean nbte$containsUuid(String key) {
		return ((CompoundTag) (Object) this).read(key, UUIDUtil.CODEC).isPresent();
	}
	
	public Optional<Byte> nbte$getByte(String key) {
		return ((CompoundTag) (Object) this).getByte(key);
	}
	public byte nbte$getByteOrDefault(String key) {
		return ((CompoundTag) (Object) this).getByteOr(key, (byte) 0);
	}
	
	public Optional<Short> nbte$getShort(String key) {
		return ((CompoundTag) (Object) this).getShort(key);
	}
	public short nbte$getShortOrDefault(String key) {
		return ((CompoundTag) (Object) this).getShortOr(key, (short) 0);
	}
	
	public Optional<Integer> nbte$getInt(String key) {
		return ((CompoundTag) (Object) this).getInt(key);
	}
	public int nbte$getIntOrDefault(String key) {
		return ((CompoundTag) (Object) this).getIntOr(key, 0);
	}
	
	public Optional<Long> nbte$getLong(String key) {
		return ((CompoundTag) (Object) this).getLong(key);
	}
	public long nbte$getLongOrDefault(String key) {
		return ((CompoundTag) (Object) this).getLongOr(key, 0);
	}
	
	public Optional<Float> nbte$getFloat(String key) {
		return ((CompoundTag) (Object) this).getFloat(key);
	}
	public float nbte$getFloatOrDefault(String key) {
		return ((CompoundTag) (Object) this).getFloatOr(key, 0);
	}
	
	public Optional<Double> nbte$getDouble(String key) {
		return ((CompoundTag) (Object) this).getDouble(key);
	}
	public double nbte$getDoubleOrDefault(String key) {
		return ((CompoundTag) (Object) this).getDoubleOr(key, 0);
	}
	
	public Optional<String> nbte$getString(String key) {
		return ((CompoundTag) (Object) this).getString(key);
	}
	public String nbte$getStringOrDefault(String key) {
		return ((CompoundTag) (Object) this).getStringOr(key, "");
	}
	
	public Optional<byte[]> nbte$getByteArray(String key) {
		return ((CompoundTag) (Object) this).getByteArray(key);
	}
	public byte[] nbte$getByteArrayOrDefault(String key) {
		return ((CompoundTag) (Object) this).getByteArray(key).orElseGet(() -> new byte[0]);
	}
	
	public Optional<int[]> nbte$getIntArray(String key) {
		return ((CompoundTag) (Object) this).getIntArray(key);
	}
	public int[] nbte$getIntArrayOrDefault(String key) {
		return ((CompoundTag) (Object) this).getIntArray(key).orElseGet(() -> new int[0]);
	}
	
	public Optional<long[]> nbte$getLongArray(String key) {
		return ((CompoundTag) (Object) this).getLongArray(key);
	}
	public long[] nbte$getLongArrayOrDefault(String key) {
		return ((CompoundTag) (Object) this).getLongArray(key).orElseGet(() -> new long[0]);
	}
	
	public Optional<CompoundTag> nbte$getCompound(String key) {
		return ((CompoundTag) (Object) this).getCompound(key);
	}
	public CompoundTag nbte$getCompoundOrDefault(String key) {
		return ((CompoundTag) (Object) this).getCompoundOrEmpty(key);
	}
	
	public Optional<ListTag> nbte$getList(String key) {
		return ((CompoundTag) (Object) this).getList(key);
	}
	public ListTag nbte$getListOrDefault(String key) {
		return ((CompoundTag) (Object) this).getListOrEmpty(key);
	}
	public Optional<ListTag> nbte$getList(String key, byte type) {
		return ((CompoundTag) (Object) this).getList(key).filter(list -> list.stream().allMatch(element -> element.getId() == type));
	}
	public ListTag nbte$getListOrDefault(String key, byte type) {
		return nbte$getList(key, type).orElseGet(ListTag::new);
	}
	public Optional<ListTag> nbte$getPartialList(String key, byte type) {
		return ((CompoundTag) (Object) this).getList(key).map(list -> list.stream().filter(element -> element.getId() == type)
				.collect(Collectors.toCollection(ListTag::new)));
	}
	public ListTag nbte$getPartialListOrDefault(String key, byte type) {
		return nbte$getPartialList(key, type).orElseGet(ListTag::new);
	}
	
	public Optional<Boolean> nbte$getBoolean(String key) {
		return nbte$getByte(key).map(b -> b != 0);
	}
	public boolean nbte$getBooleanOrDefault(String key) {
		return nbte$getByteOrDefault(key) != 0;
	}
	
	public Optional<UUID> nbte$getUuid(String key) {
		return ((CompoundTag) (Object) this).read(key, UUIDUtil.CODEC);
	}
	
	public void nbte$putUuid(String key, UUID uuid) {
		((CompoundTag) (Object) this).store(key, UUIDUtil.CODEC, uuid);
	}
	
}
