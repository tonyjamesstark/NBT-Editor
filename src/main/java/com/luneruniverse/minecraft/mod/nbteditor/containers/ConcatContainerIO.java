package com.luneruniverse.minecraft.mod.nbteditor.containers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.ToIntBiFunction;

import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.Identifier;

public class ConcatContainerIO<T> implements ContainerIO<T> {
	
	private final ContainerIO<T>[] ios;
	
	@SafeVarargs
	public ConcatContainerIO(ContainerIO<T>... ios) {
		this.ios = ios;
	}
	
	@Override
	public boolean isSupported(T container) {
		for (ContainerIO<T> io : ios) {
			if (!io.isSupported(container))
				return false;
		}
		return true;
	}
	
	@Override
	public int getMaxSlots(T container) {
		int maxSlots = 0;
		for (ContainerIO<T> io : ios)
			maxSlots += io.getMaxSlots(container);
		return maxSlots;
	}
	
	@Override
	public Identifier[] getTextures(T container) {
		List<Identifier> textures = new ArrayList<>();
		for (ContainerIO<T> io : ios)
			textures.addAll(Arrays.asList(io.getTextures(container)));
		return textures.toArray(Identifier[]::new);
	}
	
	@Override
	public ItemStack[] read(T container) {
		List<ItemStack> contents = new ArrayList<>();
		for (ContainerIO<T> io : ios)
			contents.addAll(Arrays.asList(io.read(container)));
		return contents.toArray(ItemStack[]::new);
	}
	
	/**
	 * The contents an io leaves for the next one in the chain, which is everything past the
	 * {@code numWritten} entries it claimed.
	 */
	private static ItemStack[] remaining(ItemStack[] contents, int numWritten) {
		if (numWritten >= contents.length)
			return new ItemStack[0];
		return Arrays.copyOfRange(contents, numWritten, contents.length);
	}
	
	/**
	 * Hands each io in turn the contents no earlier io claimed, and adds up what each one takes.
	 *
	 * <p>{@link #write} and {@link #getNumWritten} differ only in what {@code claim} does, and the
	 * contract says they have to return the same number, so they walk the chain the same way here
	 * rather than in two copies that can drift.
	 */
	private int claimInTurn(ItemStack[] contents, ToIntBiFunction<ContainerIO<T>, ItemStack[]> claim) {
		int numWritten = 0;
		for (ContainerIO<T> io : ios) {
			int currentWritten = claim.applyAsInt(io, contents);
			contents = remaining(contents, currentWritten);
			numWritten += currentWritten;
		}
		return numWritten;
	}
	
	@Override
	public int write(T container, ItemStack[] contents) {
		return claimInTurn(contents, (io, rest) -> io.write(container, rest));
	}
	
	@Override
	public int getNumWritten(T container, ItemStack[] contents) {
		return claimInTurn(contents, (io, rest) -> io.getNumWritten(container, rest));
	}
	
	@Override
	public int getWrittenSlotIndex(T container, ItemStack[] contents, int slot) {
		int numWritten = 0;
		for (ContainerIO<T> io : ios) {
			int currentWritten = io.getNumWritten(container, contents);
			if (slot < numWritten + currentWritten)
				return io.getWrittenSlotIndex(container, contents, slot - numWritten) + numWritten;
			contents = remaining(contents, currentWritten);
			numWritten += currentWritten;
		}
		throw new IllegalArgumentException("Slot is never written: " + slot);
	}
	
}
