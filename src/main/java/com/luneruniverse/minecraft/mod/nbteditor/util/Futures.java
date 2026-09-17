package com.luneruniverse.minecraft.mod.nbteditor.util;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Fan-out for {@link CompletableFuture}, which only ships fan-in. */
public class Futures {
	
	/**
	 * One future whose outcome, whichever it is, becomes the outcome of all of
	 * <code>futures</code>.
	 *
	 * <p>The queues use this to collapse a run of waiters onto the single load or save that will
	 * actually answer them.
	 */
	public static <T> CompletableFuture<T> mergeFutures(List<CompletableFuture<T>> futures) {
		CompletableFuture<T> output = new CompletableFuture<>();
		output.thenAccept(value -> futures.forEach(future -> future.complete(value)));
		output.exceptionally(e -> {
			futures.forEach(future -> future.completeExceptionally(e));
			return null;
		});
		return output;
	}
	
	private Futures() {}
	
}
