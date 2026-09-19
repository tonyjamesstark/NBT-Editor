package com.luneruniverse.minecraft.mod.nbteditor.util;

import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import com.mojang.datafixers.DSL.TypeReference;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;

/**
 * Pins the one decision {@code DataFixes} makes before it reaches a fixer: whether the nbt needs
 * updating at all. Guessing wrong there runs a player's data through a chain of fixes it has
 * already had, so the guard matters more than the fixer call it guards.
 *
 * <p>The fixer call itself needs {@code Minecraft.getInstance()} and cannot run here (ADR-0004).
 * Only the branches that return before it are covered.
 */
class DataFixesTest {

	private static final TypeReference ANY_TYPE = () -> "nbteditor_test";

	@Test
	void nbtWithNoDataVersionAndNoDefaultIsHandedBackUntouched() {
		CompoundTag nbt = new CompoundTag();
		nbt.putString("id", "minecraft:stone");

		assertSame(nbt, DataFixes.updateDynamic(ANY_TYPE, nbt));
	}

	@Test
	void aDataVersionThatIsNotANumberIsNotGuessedAt() {
		CompoundTag nbt = new CompoundTag();
		nbt.put("DataVersion", StringTag.valueOf("3105"));

		assertSame(nbt, DataFixes.updateDynamic(ANY_TYPE, nbt));
	}

	@Test
	void anAbsentTagWithNoDefaultReturnsBeforeTheFixer() {
		CompoundTag nbt = new CompoundTag();

		assertSame(nbt, DataFixes.updateDynamic(ANY_TYPE, nbt, null, -1));
	}

}
