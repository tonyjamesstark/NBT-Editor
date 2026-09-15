package com.luneruniverse.minecraft.mod.nbteditor.clientchest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import com.luneruniverse.minecraft.mod.nbteditor.clientchest.PageTasks.Access;
import com.luneruniverse.minecraft.mod.nbteditor.util.lock.PartitionedReadWriteLock;

/**
 * Pins the orchestration {@code ClientChest}'s page operations share: which pages a sweep visits,
 * how failures are aggregated, and how a page reports itself busy. The work lambdas themselves are
 * Minecraft-bound and are not exercised here.
 */
class PageTasksTest {

	private static final Logger LOGGER = LogManager.getLogger("nbteditor-test");

	@TempDir
	File folder;

	private PageTasks tasks(int pageCount) {
		return new PageTasks(new PartitionedReadWriteLock(), folder, () -> pageCount, LOGGER);
	}

	private void givenPageFiles(String... names) throws IOException {
		for (String name : names)
			Files.writeString(new File(folder, name).toPath(), "x");
	}

	private static List<Integer> sorted(List<Integer> pages) {
		List<Integer> copy = new java.util.ArrayList<>(pages);
		Collections.sort(copy);
		return copy;
	}

	/**
	 * The future completes inside the task's {@code try}, so the {@code finally} that releases the
	 * lock and clears the busy flag can still be pending when {@code get()} returns. That ordering
	 * is the long-standing behaviour, so teardown assertions wait for it rather than assume it.
	 */
	private static void eventually(java.util.function.BooleanSupplier condition, String message) throws Exception {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (System.nanoTime() < deadline) {
			if (condition.getAsBoolean())
				return;
			Thread.sleep(5);
		}
		throw new AssertionError(message);
	}

	@Test
	void pageNumberOfAcceptsOnlyPageFiles() {
		assertEquals(Optional.of(0), PageTasks.pageNumberOf("page0.nbt"));
		assertEquals(Optional.of(12), PageTasks.pageNumberOf("page12.nbt"));
		assertEquals(Optional.empty(), PageTasks.pageNumberOf("page_names.json"));
		assertEquals(Optional.empty(), PageTasks.pageNumberOf("page.nbt"));
		assertEquals(Optional.empty(), PageTasks.pageNumberOf("pageX.nbt"));
		assertEquals(Optional.empty(), PageTasks.pageNumberOf("page1.nbt.bak"));
		assertEquals(Optional.empty(), PageTasks.pageNumberOf("backup-page1.nbt"));
	}

	@Test
	void pageNumberOfStillThrowsOnOverflow() {
		assertThrows(NumberFormatException.class, () -> PageTasks.pageNumberOf("page99999999999.nbt"));
	}

	@Test
	@Timeout(30)
	void allPagesVisitsEveryPageFileBelowThePageCount() throws Exception {
		givenPageFiles("page0.nbt", "page1.nbt", "page2.nbt", "page7.nbt", "page_names.json", "notes.txt");
		List<Integer> visited = Collections.synchronizedList(new java.util.ArrayList<>());

		tasks(3).allPages(Access.WRITE, "Importing", page -> true, page -> {
			visited.add(page);
			return null;
		}).get(10, TimeUnit.SECONDS);

		assertEquals(List.of(0, 1, 2), sorted(visited), "page7 is past the page count and page_names.json is not a page");
	}

	@Test
	@Timeout(30)
	void allPagesSkipsPagesTheSelectorRejects() throws Exception {
		givenPageFiles("page0.nbt", "page1.nbt", "page2.nbt", "page3.nbt");
		List<Integer> visited = Collections.synchronizedList(new java.util.ArrayList<>());

		tasks(4).allPages(Access.WRITE, "Updating", page -> page % 2 == 0, page -> {
			visited.add(page);
			return null;
		}).get(10, TimeUnit.SECONDS);

		assertEquals(List.of(0, 2), sorted(visited));
	}

	@Test
	@Timeout(30)
	void allPagesCompletesNormallyWhenTheFolderIsMissing() throws Exception {
		File missing = new File(folder, "absent");
		PageTasks tasks = new PageTasks(new PartitionedReadWriteLock(), missing, () -> 4, LOGGER);
		AtomicInteger runs = new AtomicInteger();

		CompletableFuture<Void> future = tasks.allPages(Access.WRITE, "Importing", page -> true, page -> {
			runs.incrementAndGet();
			return null;
		});

		assertTrue(future.isDone(), "a missing folder must complete the future immediately");
		assertEquals(null, future.get());
		assertEquals(0, runs.get());
	}

	@Test
	@Timeout(30)
	void allPagesKeepsGoingAfterAFailureAndReportsEveryOne() throws Exception {
		givenPageFiles("page0.nbt", "page1.nbt", "page2.nbt");
		List<Integer> visited = Collections.synchronizedList(new java.util.ArrayList<>());

		CompletableFuture<Void> future = tasks(3).allPages(Access.WRITE, "Unloading", page -> true, page -> {
			visited.add(page);
			if (page != 1)
				throw new IllegalStateException("page " + page + " is broken");
			return null;
		});

		ExecutionException thrown = assertThrows(ExecutionException.class, () -> future.get(10, TimeUnit.SECONDS));
		assertEquals(List.of(0, 1, 2), sorted(visited), "a failing page must not abandon the sweep");

		Throwable[] suppressed = thrown.getCause().getSuppressed();
		assertEquals(2, suppressed.length, "one suppressed entry per failed page");
		List<String> messages = List.of(suppressed[0].getMessage(), suppressed[1].getMessage());
		assertTrue(messages.contains("Page 1"), "pages are reported one-indexed, got " + messages);
		assertTrue(messages.contains("Page 3"), "pages are reported one-indexed, got " + messages);
	}

	@Test
	@Timeout(30)
	void pageCompletesWithTheWorkResult() throws Exception {
		Object result = new Object();
		assertSame(result, tasks(4).page(2, Access.READ, "Unloading", page -> result).get(10, TimeUnit.SECONDS));
	}

	@Test
	@Timeout(30)
	void pageCompletesExceptionallyWhenTheWorkThrows() {
		IllegalStateException boom = new IllegalStateException("boom");

		CompletableFuture<Object> future = tasks(4).page(1, Access.WRITE, "Discarding", page -> {
			throw boom;
		});

		ExecutionException thrown = assertThrows(ExecutionException.class, () -> future.get(10, TimeUnit.SECONDS));
		assertSame(boom, thrown.getCause());
	}

	@Test
	@Timeout(30)
	void aPageReportsBusyForTheDurationOfItsTask() throws Exception {
		PageTasks tasks = tasks(4);
		CountDownLatch inside = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);

		assertFalse(tasks.isProcessing(2), "page 2 was busy before any task started");

		CompletableFuture<Object> future = tasks.page(2, Access.WRITE, "Updating", page -> {
			inside.countDown();
			release.await();
			return null;
		});

		assertTrue(inside.await(10, TimeUnit.SECONDS), "task never started");
		assertTrue(tasks.isProcessing(2), "page 2 was not reported busy while its task ran");
		assertFalse(tasks.isProcessing(3), "an unrelated page was reported busy");

		release.countDown();
		future.get(10, TimeUnit.SECONDS);
		eventually(() -> !tasks.isProcessing(2), "page 2 stayed busy after its task finished");
	}

	@Test
	@Timeout(30)
	void aSweepReportsEveryPageBusy() throws Exception {
		givenPageFiles("page0.nbt");
		PageTasks tasks = tasks(4);
		CountDownLatch inside = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);

		CompletableFuture<Void> future = tasks.allPages(Access.WRITE, "Importing", page -> true, page -> {
			inside.countDown();
			release.await();
			return null;
		});

		assertTrue(inside.await(10, TimeUnit.SECONDS), "sweep never started");
		assertTrue(tasks.isProcessing(999), "a sweep must report every page busy, including ones with no file");

		release.countDown();
		future.get(10, TimeUnit.SECONDS);
		eventually(() -> !tasks.isProcessing(999), "pages stayed busy after the sweep finished");
	}

	@Test
	@Timeout(30)
	void workRunsUnderThePageLock() throws Exception {
		PartitionedReadWriteLock lock = new PartitionedReadWriteLock();
		PageTasks tasks = new PageTasks(lock, folder, () -> 4, LOGGER);
		CountDownLatch inside = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);

		CompletableFuture<Object> future = tasks.page(2, Access.WRITE, "Updating", page -> {
			inside.countDown();
			release.await();
			return null;
		});
		assertTrue(inside.await(10, TimeUnit.SECONDS), "task never started");

		assertTrue(lock.write().isLocked(2), "page 2 was not write-locked while its task ran");

		release.countDown();
		future.get(10, TimeUnit.SECONDS);
		eventually(() -> !lock.write().isLocked(2), "page 2 stayed locked after its task finished");
	}

}
