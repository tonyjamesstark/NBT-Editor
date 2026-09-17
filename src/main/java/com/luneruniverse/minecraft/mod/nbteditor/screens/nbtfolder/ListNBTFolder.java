package com.luneruniverse.minecraft.mod.nbteditor.screens.nbtfolder;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.nbt.MVNbtCompoundParent;
import com.luneruniverse.minecraft.mod.nbteditor.screens.NBTEditorScreen;
import com.luneruniverse.minecraft.mod.nbteditor.screens.NBTValue;

import net.minecraft.nbt.CollectionTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.StringTag;
import com.luneruniverse.minecraft.mod.nbteditor.util.IntFields;

public class ListNBTFolder implements NBTFolder<CollectionTag> {
	
	private final Supplier<CollectionTag> get;
	private final Consumer<CollectionTag> set;
	
	public ListNBTFolder(Supplier<CollectionTag> get, Consumer<CollectionTag> set) {
		this.get = get;
		this.set = set;
	}
	
	@Override
	public CollectionTag getNBT() {
		return get.get();
	}
	
	@Override
	public void setNBT(CollectionTag value) {
		set.accept(value);
	}
	
	@Override
	public List<NBTValue> getEntries(NBTEditorScreen<?> screen) {
		CollectionTag nbt = getNBT();
		return IntStream.range(0, nbt.nbte$size())
				.mapToObj(i -> new NBTValue(screen, i + "", nbt.nbte$get(i), nbt)).collect(Collectors.toList());
	}
	
	@Override
	public boolean hasEmptyKey() {
		return false;
	}
	
	@Override
	public Tag getValue(String key) {
		CollectionTag nbt = getNBT();
		try {
			int i = Integer.parseInt(key);
			if (i < 0 || i >= nbt.nbte$size())
				return null;
			return nbt.nbte$get(i);
		} catch (NumberFormatException e) {
			return null;
		}
	}
	
	@Override
	public void setValue(String key, Tag value) {
		CollectionTag nbt = getNBT();
		int i = Integer.parseInt(key);
		if (nbt.nbte$size() == 1 && i == 0 && nbt instanceof ListTag list) {
			list.setTag(0, value);
			setNBT(nbt);
		} else {
			nbt.nbte$set(i, value);
			setNBT(nbt);
		}
	}
	
	@Override
	public void addKey(String key) {
		CollectionTag nbt = getNBT();
		nbt.nbte$add(Integer.parseInt(key), getDefaultValue(nbt));
		setNBT(nbt);
	}
	
	@Override
	public void removeKey(String key) {
		CollectionTag nbt = getNBT();
		try {
			int i = Integer.parseInt(key);
			if (i >= 0 && i < nbt.nbte$size()) {
				nbt.nbte$remove(i);
				setNBT(nbt);
			}
		} catch (NumberFormatException e) {}
	}
	
	@Override
	public Optional<String> getNextKey(Optional<String> pastingKey) {
		return Optional.of(getNBT().nbte$size() + "");
	}
	
	private Tag getDefaultValue(CollectionTag nbt) {
		// An empty list reports type 0 and lands on the int case below; the fallback is only for
		// a list holding more than one type, where the last entry is the better guess.
		return switch (nbt.nbte$getHeldType().orElseGet(() -> nbt.nbte$get(nbt.nbte$size() - 1).getId())) {
			case Tag.TAG_BYTE -> ByteTag.ZERO;
			case Tag.TAG_SHORT -> ShortTag.valueOf((short) 0);
			case 0, Tag.TAG_INT -> IntTag.valueOf(0);
			case Tag.TAG_LONG -> LongTag.valueOf(0);
			case Tag.TAG_FLOAT -> FloatTag.ZERO;
			case Tag.TAG_DOUBLE -> DoubleTag.ZERO;
			case Tag.TAG_BYTE_ARRAY -> new ByteArrayTag(new byte[0]);
			case Tag.TAG_INT_ARRAY -> new IntArrayTag(new int[0]);
			case Tag.TAG_LONG_ARRAY -> new LongArrayTag(new long[0]);
			case Tag.TAG_LIST -> new ListTag();
			case Tag.TAG_COMPOUND -> new CompoundTag();
			case Tag.TAG_STRING -> StringTag.valueOf("");
			default -> throw new IllegalArgumentException("Unknown NBT type: " + nbt.nbte$getHeldType().get());
		};
	}
	
	@Override
	public Predicate<String> getKeyValidator(boolean renaming) {
		return IntFields.intPredicate(() -> 0, () -> getNBT().nbte$size() + (renaming ? -1 : 0), false);
	}
	
	@Override
	public boolean handlesDuplicateKeys() {
		return true;
	}
	
}
