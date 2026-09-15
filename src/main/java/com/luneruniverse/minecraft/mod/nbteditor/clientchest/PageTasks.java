package com.luneruniverse.minecraft.mod.nbteditor.clientchest;

import java.io.File;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntPredicate;
import java.util.function.IntSupplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.Logger;

import com.luneruniverse.minecraft.mod.nbteditor.util.lock.PartitionedLock;
import com.luneruniverse.minecraft.mod.nbteditor.util.lock.PartitionedReadWriteLock;

/**
 * Runs client chest page work off-thread, under the page lock, while recording the page as busy
 * so {@link ClientChest#isProcessingPage(int)} can report it.
 *
 * <p>Two shapes. {@link #page} runs one unit of work against a single page and completes with its
 * result. {@link #allPages} walks the page files on disk and runs work against every page a
 * selector accepts, collecting per-page failures as suppressed exceptions so one bad page does not
 * abandon the sweep.
 *
 * <p>Holds no Minecraft types, so it is testable without a game runtime. Everything
 * version-specific stays in the caller's work lambda.
 */
public class PageTasks {

	/** Page work that is allowed to fail; failures are routed to the returned future. */
	@FunctionalInterface
	public interface PageWork<T> {
		T run(int page) throws Throwable;
	}

	/** Which half of the lock a task needs. */
	public enum Access {
		READ,
		WRITE
	}

	private static final Pattern PAGE_FILE = Pattern.compile("page([0-9]+)\\.nbt");

	/** Key under which a whole-chest sweep records itself in {@link #processing}. */
	private static final int ALL_PAGES = -1;

	private final PartitionedReadWriteLock lock;
	private final File folder;
	private final IntSupplier pageCount;
	private final Logger logger;
	private final Map<Integer, Integer> processing;

	public PageTasks(PartitionedReadWriteLock lock, File folder, IntSupplier pageCount, Logger logger) {
		this.lock = lock;
		this.folder = folder;
		this.pageCount = pageCount;
		this.logger = logger;
		this.processing = new ConcurrentHashMap<>();
	}

	/** True while any task is running against this page, or against every page. */
	public boolean isProcessing(int page) {
		return processing.getOrDefault(page, 0) > 0 || processing.getOrDefault(ALL_PAGES, 0) > 0;
	}

	private void start(int page) {
		processing.compute(page, (key, value) -> (value == null ? 0 : value) + 1);
	}

	private void finish(int page) {
		processing.compute(page, (key, value) -> value == 1 ? null : value - 1);
	}

	private PartitionedLock lockFor(Access access) {
		return access == Access.WRITE ? lock.write() : lock.read();
	}

	/**
	 * @param op capitalized gerund naming the operation, such as {@code "Unloading"}. Names the
	 *           thread, and lowercased it names the operation in the failure log.
	 */
	public <T> CompletableFuture<T> page(int page, Access access, String op, PageWork<T> work) {
		start(page);
		CompletableFuture<T> future = new CompletableFuture<>();
		Thread thread = new Thread(() -> {
			PartitionedLock pageLock = lockFor(access);
			pageLock.lock(page);
			try {
				future.complete(work.run(page));
			} catch (Throwable e) {
				logger.error("Error " + op.toLowerCase() + " client chest page " + (page + 1), e);
				future.completeExceptionally(e);
			} finally {
				pageLock.unlock(page);
				finish(page);
			}
		}, "NBTEditor/Async/ClientChest/" + op + "/" + page);
		thread.start();
		return future;
	}

	/**
	 * Completes with null once every selected page has been attempted. If any page failed, the
	 * future completes exceptionally with one exception carrying a suppressed entry per page.
	 *
	 * @param op capitalized gerund naming the operation, such as {@code "Importing"}.
	 */
	public CompletableFuture<Void> allPages(Access access, String op, IntPredicate selector, PageWork<Void> work) {
		if (!folder.exists())
			return CompletableFuture.completedFuture(null);

		start(ALL_PAGES);
		CompletableFuture<Void> future = new CompletableFuture<>();
		Thread thread = new Thread(() -> {
			PartitionedLock allLock = lockFor(access);
			allLock.lockAll();
			try {
				Exception toThrow = new Exception("Error " + op.toLowerCase() + " page(s)");
				File[] files = folder.listFiles();
				for (File file : files == null ? new File[0] : files) {
					Optional<Integer> parsed = pageNumberOf(file.getName());
					if (parsed.isEmpty())
						continue;
					int page = parsed.get();
					if (page >= pageCount.getAsInt())
						continue;
					if (!selector.test(page))
						continue;
					try {
						work.run(page);
					} catch (Throwable e) {
						toThrow.addSuppressed(new Exception("Page " + (page + 1), e));
					}
				}
				if (toThrow.getSuppressed().length > 0) {
					logger.error("Error " + op.toLowerCase() + " the client chest!", toThrow);
					future.completeExceptionally(toThrow);
				} else
					future.complete(null);
			} finally {
				allLock.unlockAll();
				finish(ALL_PAGES);
			}
		}, "NBTEditor/Async/ClientChest/" + op);
		thread.start();
		return future;
	}

	/**
	 * {@code page12.nbt} to {@code 12}, empty for any other name. A name that matches but whose
	 * number overflows an int still throws, as it always has.
	 */
	static Optional<Integer> pageNumberOf(String fileName) {
		Matcher matcher = PAGE_FILE.matcher(fileName);
		if (!matcher.matches())
			return Optional.empty();
		return Optional.of(Integer.parseInt(matcher.group(1)));
	}

}
