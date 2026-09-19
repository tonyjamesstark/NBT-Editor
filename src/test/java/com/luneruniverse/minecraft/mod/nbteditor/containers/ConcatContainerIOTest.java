package com.luneruniverse.minecraft.mod.nbteditor.containers;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * The chaining arithmetic, which is what {@link ConcatContainerIO} is for. Every io in a chain is
 * handed only the contents no earlier io claimed, so an off-by-one in that hand-off writes each
 * container's items into the wrong one, and nothing else in the build opens a container.
 *
 * <p>The stubs never look at an {@link ItemStack}, so the arrays hold nulls and the test stays in
 * the JVM-only slice (ADR-0004).
 */
class ConcatContainerIOTest {

	/** Records how much it was handed, and claims a fixed number of slots. */
	private static class CountingIO implements ContainerIO<Object> {
		final List<Integer> handed = new ArrayList<>();
		final int slots;
		CountingIO(int slots) {
			this.slots = slots;
		}
		@Override
		public boolean isSupported(Object container) {
			return true;
		}
		@Override
		public int getMaxSlots(Object container) {
			return slots;
		}
		@Override
		public Identifier[] getTextures(Object container) {
			return new Identifier[slots];
		}
		@Override
		public ItemStack[] read(Object container) {
			return new ItemStack[slots];
		}
		@Override
		public int write(Object container, ItemStack[] contents) {
			handed.add(contents.length);
			return getNumWritten(container, contents);
		}
	}

	/** Claims fewer slots than it holds, like the ios that drop gaps rather than recording them. */
	private static class CompactingIO extends CountingIO {
		CompactingIO(int slots) {
			super(slots);
		}
		@Override
		public int getNumWritten(Object container, ItemStack[] contents) {
			return Math.min(slots, contents.length) / 2;
		}
		@Override
		public int getWrittenSlotIndex(Object container, ItemStack[] contents, int slot) {
			return slot / 2;
		}
	}

	private static ItemStack[] contents(int size) {
		return new ItemStack[size];
	}

	@Test
	void eachIoIsHandedOnlyWhatTheEarlierOnesLeft() {
		CountingIO first = new CountingIO(3);
		CountingIO second = new CountingIO(4);
		CountingIO third = new CountingIO(2);
		ConcatContainerIO<Object> concat = new ConcatContainerIO<>(first, second, third);

		assertEquals(9, concat.write(new Object(), contents(9)));

		assertEquals(List.of(9), first.handed);
		assertEquals(List.of(6), second.handed);
		assertEquals(List.of(2), third.handed);
	}

	@Test
	void anIoPastTheEndOfTheContentsIsHandedAnEmptyArray() {
		CountingIO first = new CountingIO(5);
		CountingIO second = new CountingIO(4);
		ConcatContainerIO<Object> concat = new ConcatContainerIO<>(first, second);

		concat.write(new Object(), contents(3));

		assertEquals(List.of(3), first.handed);
		assertEquals(List.of(0), second.handed);
	}

	@Test
	void getNumWrittenAgreesWithWrite() {
		Object container = new Object();
		ItemStack[] contents = contents(7);
		ConcatContainerIO<Object> concat =
				new ConcatContainerIO<>(new CountingIO(3), new CountingIO(4), new CompactingIO(6));

		assertEquals(concat.write(container, contents), concat.getNumWritten(container, contents));
	}

	@Test
	void aSlotIsOffsetByEverythingTheEarlierIosClaim() {
		ConcatContainerIO<Object> concat = new ConcatContainerIO<>(new CountingIO(3), new CountingIO(4));
		Object container = new Object();
		ItemStack[] contents = contents(7);

		assertArrayEquals(new int[] {0, 1, 2, 3, 4, 5, 6}, new int[] {
				concat.getWrittenSlotIndex(container, contents, 0),
				concat.getWrittenSlotIndex(container, contents, 1),
				concat.getWrittenSlotIndex(container, contents, 2),
				concat.getWrittenSlotIndex(container, contents, 3),
				concat.getWrittenSlotIndex(container, contents, 4),
				concat.getWrittenSlotIndex(container, contents, 5),
				concat.getWrittenSlotIndex(container, contents, 6)});
	}

	@Test
	void aCompactingIoShiftsItsOwnSlotsAndNotTheOnesBeforeIt() {
		ConcatContainerIO<Object> concat = new ConcatContainerIO<>(new CountingIO(3), new CompactingIO(4));
		Object container = new Object();
		ItemStack[] contents = contents(7);

		assertEquals(2, concat.getWrittenSlotIndex(container, contents, 2));
		assertEquals(3, concat.getWrittenSlotIndex(container, contents, 3));
		assertEquals(3, concat.getWrittenSlotIndex(container, contents, 4));
	}

	@Test
	void aSlotPastTheEndOfTheChainIsRejected() {
		ConcatContainerIO<Object> concat = new ConcatContainerIO<>(new CountingIO(3), new CountingIO(4));

		assertThrows(IllegalArgumentException.class,
				() -> concat.getWrittenSlotIndex(new Object(), contents(7), 7));
	}

}
