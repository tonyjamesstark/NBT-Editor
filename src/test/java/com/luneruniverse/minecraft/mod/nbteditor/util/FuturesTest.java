package com.luneruniverse.minecraft.mod.nbteditor.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;

/**
 * Pins the fan-out the page queues collapse their waiters onto. A waiter left uncompleted here is
 * a caller of the client chest that never returns, so both outcomes have to reach every member of
 * the list and not just the first.
 */
class FuturesTest {

	@Test
	void everyMemberGetsTheValue() {
		List<CompletableFuture<String>> members = List.of(new CompletableFuture<>(),
				new CompletableFuture<>(), new CompletableFuture<>());

		Futures.mergeFutures(members).complete("page");

		for (CompletableFuture<String> member : members)
			assertEquals("page", member.getNow(null));
	}

	@Test
	void everyMemberGetsTheFailure() {
		List<CompletableFuture<String>> members = List.of(new CompletableFuture<>(),
				new CompletableFuture<>(), new CompletableFuture<>());
		RuntimeException failure = new RuntimeException("disk gone");

		Futures.mergeFutures(members).completeExceptionally(failure);

		for (CompletableFuture<String> member : members) {
			assertTrue(member.isCompletedExceptionally());
			ExecutionException thrown = assertThrows(ExecutionException.class, member::get);
			assertSame(failure, thrown.getCause(), "the original failure reaches the waiter");
		}
	}

	@Test
	void aMemberThatAlreadyAnsweredKeepsItsOwnValue() {
		CompletableFuture<String> answered = CompletableFuture.completedFuture("cached");
		CompletableFuture<String> waiting = new CompletableFuture<>();

		Futures.mergeFutures(List.of(answered, waiting)).complete("loaded");

		assertEquals("cached", answered.getNow(null));
		assertEquals("loaded", waiting.getNow(null));
	}

	@Test
	void anEmptyListIsNotAnError() {
		CompletableFuture<String> merged = Futures.mergeFutures(List.of());
		merged.complete("nobody waiting");
		assertFalse(merged.isCompletedExceptionally());
	}

}
