package com.luneruniverse.minecraft.mod.nbteditor.util.lock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Covers the locking invariants {@code ClientChest} depends on when it runs page IO off-thread.
 * These are the guarantees that must survive the {@code perPage}/{@code allPages} collapse
 * described in {@code docs/AUDIT-2026-09-14.md} item 5.
 *
 * <p>{@link PartitionedLock#stop()} is deliberately not covered: by design it parks every waiting
 * thread in {@code Thread.sleep(Long.MAX_VALUE)} and never releases them, which would hang the
 * test JVM rather than fail it.
 */
class PartitionedReadWriteLockTest {

	/** Long enough that a real acquisition wins the race; short enough to keep the suite quick. */
	private static final long BLOCKED_MILLIS = 300;
	private static final long ACQUIRED_SECONDS = 5;

	private static Thread start(Runnable body) {
		Thread thread = new Thread(body);
		thread.setDaemon(true);
		thread.start();
		return thread;
	}

	private static void await(CountDownLatch latch) {
		try {
			latch.await();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	@Test
	@Timeout(30)
	void writeLockSerializesTheSamePartition() throws Exception {
		PartitionedReadWriteLock lock = new PartitionedReadWriteLock();
		CountDownLatch held = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		CountDownLatch secondDone = new CountDownLatch(1);
		AtomicBoolean secondAcquired = new AtomicBoolean();

		start(() -> {
			lock.write().lock(1);
			held.countDown();
			await(release);
			lock.write().unlock(1);
		});
		assertTrue(held.await(ACQUIRED_SECONDS, TimeUnit.SECONDS), "first writer never acquired partition 1");

		start(() -> {
			lock.write().lock(1);
			secondAcquired.set(true);
			lock.write().unlock(1);
			secondDone.countDown();
		});

		assertFalse(secondDone.await(BLOCKED_MILLIS, TimeUnit.MILLISECONDS),
				"a second writer entered partition 1 while it was already held");
		assertFalse(secondAcquired.get(), "two writers held partition 1 at the same time");

		release.countDown();
		assertTrue(secondDone.await(ACQUIRED_SECONDS, TimeUnit.SECONDS),
				"the second writer never acquired partition 1 after it was released");
	}

	@Test
	@Timeout(30)
	void writeLocksOnDifferentPartitionsRunConcurrently() throws Exception {
		PartitionedReadWriteLock lock = new PartitionedReadWriteLock();
		CountDownLatch bothInside = new CountDownLatch(2);
		CountDownLatch release = new CountDownLatch(1);

		for (int partition : new int[] {1, 2}) {
			start(() -> {
				lock.write().lock(partition);
				bothInside.countDown();
				await(release);
				lock.write().unlock(partition);
			});
		}

		assertTrue(bothInside.await(ACQUIRED_SECONDS, TimeUnit.SECONDS),
				"partitions 1 and 2 did not lock concurrently - the lock has collapsed to a global lock");
		release.countDown();
	}

	@Test
	@Timeout(30)
	void writeLockExcludesAReaderOnTheSamePartition() throws Exception {
		PartitionedReadWriteLock lock = new PartitionedReadWriteLock();
		CountDownLatch held = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		CountDownLatch readDone = new CountDownLatch(1);

		start(() -> {
			lock.write().lock(3);
			held.countDown();
			await(release);
			lock.write().unlock(3);
		});
		assertTrue(held.await(ACQUIRED_SECONDS, TimeUnit.SECONDS), "writer never acquired partition 3");

		start(() -> {
			lock.read().lock(3);
			lock.read().unlock(3);
			readDone.countDown();
		});

		assertFalse(readDone.await(BLOCKED_MILLIS, TimeUnit.MILLISECONDS),
				"a reader entered partition 3 while a writer held it");

		release.countDown();
		assertTrue(readDone.await(ACQUIRED_SECONDS, TimeUnit.SECONDS),
				"the reader never acquired partition 3 after the writer released it");
	}

	@Test
	@Timeout(30)
	void aReaderOnAnotherPartitionIsUnaffectedByAWrite() throws Exception {
		PartitionedReadWriteLock lock = new PartitionedReadWriteLock();
		CountDownLatch held = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		CountDownLatch readDone = new CountDownLatch(1);

		start(() -> {
			lock.write().lock(3);
			held.countDown();
			await(release);
			lock.write().unlock(3);
		});
		assertTrue(held.await(ACQUIRED_SECONDS, TimeUnit.SECONDS), "writer never acquired partition 3");

		start(() -> {
			lock.read().lock(4);
			lock.read().unlock(4);
			readDone.countDown();
		});

		assertTrue(readDone.await(ACQUIRED_SECONDS, TimeUnit.SECONDS),
				"a read of partition 4 was blocked by a write to partition 3");
		release.countDown();
	}

	/**
	 * The point of partitioning. A thread waiting for a busy partition must not hold up work on
	 * any other partition, which is what happens if the wait occurs while the global lock is held.
	 */
	@Test
	@Timeout(30)
	void aContendedPartitionDoesNotStallTheOthers() throws Exception {
		PartitionedReadWriteLock lock = new PartitionedReadWriteLock();
		CountDownLatch held = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		CountDownLatch contenderStarted = new CountDownLatch(1);
		CountDownLatch unrelatedDone = new CountDownLatch(1);

		start(() -> {
			lock.write().lock(1);
			held.countDown();
			await(release);
			lock.write().unlock(1);
		});
		assertTrue(held.await(ACQUIRED_SECONDS, TimeUnit.SECONDS), "first writer never acquired partition 1");

		start(() -> {
			contenderStarted.countDown();
			lock.write().lock(1);
			lock.write().unlock(1);
		});
		assertTrue(contenderStarted.await(ACQUIRED_SECONDS, TimeUnit.SECONDS), "contender never started");
		Thread.sleep(BLOCKED_MILLIS);

		start(() -> {
			lock.write().lock(99);
			lock.write().unlock(99);
			unrelatedDone.countDown();
		});

		assertTrue(unrelatedDone.await(ACQUIRED_SECONDS, TimeUnit.SECONDS),
				"partition 99 was blocked by contention on partition 1 - the partitioning has collapsed");
		release.countDown();
	}

	/** Two waiters on one partition must both get in, and neither may trip over the other's release. */
	@Test
	@Timeout(30)
	void twoWaitersOnOnePartitionEachAcquireInTurn() throws Exception {
		PartitionedReadWriteLock lock = new PartitionedReadWriteLock();
		CountDownLatch held = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		CountDownLatch bothDone = new CountDownLatch(2);
		AtomicInteger overlapping = new AtomicInteger();
		AtomicInteger maxOverlap = new AtomicInteger();

		start(() -> {
			lock.write().lock(5);
			held.countDown();
			await(release);
			lock.write().unlock(5);
		});
		assertTrue(held.await(ACQUIRED_SECONDS, TimeUnit.SECONDS), "holder never acquired partition 5");

		for (int i = 0; i < 2; i++) {
			start(() -> {
				lock.write().lock(5);
				maxOverlap.accumulateAndGet(overlapping.incrementAndGet(), Math::max);
				try {
					Thread.sleep(20);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				overlapping.decrementAndGet();
				lock.write().unlock(5);
				bothDone.countDown();
			});
		}

		release.countDown();
		assertTrue(bothDone.await(ACQUIRED_SECONDS, TimeUnit.SECONDS), "both waiters never acquired partition 5");
		assertEquals(1, maxOverlap.get(), "two threads held partition 5 at once");
	}

	/** Locking the same partition twice on one thread must be releasable twice. */
	@Test
	@Timeout(30)
	void aPartitionLockIsReentrant() {
		PartitionedReadWriteLock lock = new PartitionedReadWriteLock();

		lock.write().lock(4);
		lock.write().lock(4);
		assertTrue(lock.write().isLocked(4));
		lock.write().unlock(4);
		lock.write().unlock(4);

		assertFalse(lock.write().isLocked(4), "partition 4 stayed locked after both releases");
	}

	@Test
	@Timeout(30)
	void isLockedTracksPartitionState() {
		PartitionedReadWriteLock lock = new PartitionedReadWriteLock();

		assertFalse(lock.write().isLocked(7), "partition 7 reported locked before anything locked it");

		lock.write().lock(7);
		assertTrue(lock.write().isLocked(7), "partition 7 reported unlocked while held");
		assertFalse(lock.write().isLocked(8), "an unrelated partition reported locked");
		assertFalse(lock.write().isAllLocked(), "a single partition lock reported as a global lock");

		lock.write().unlock(7);
		assertFalse(lock.write().isLocked(7), "partition 7 still reported locked after release");
	}

	/**
	 * Heavy contention over few partitions, which is the shape that exposed the two defects the
	 * targeted tests above pin. Asserts the invariant directly: never two threads inside one
	 * partition, and nothing still held once the threads are done.
	 */
	@Test
	@Timeout(60)
	void mutualExclusionHoldsUnderContention() throws Exception {
		int threads = 8;
		int iterations = 200;
		int partitions = 4;

		PartitionedReadWriteLock lock = new PartitionedReadWriteLock();
		AtomicInteger[] inside = new AtomicInteger[partitions];
		for (int i = 0; i < partitions; i++)
			inside[i] = new AtomicInteger();
		AtomicInteger violations = new AtomicInteger();
		AtomicReference<Throwable> failure = new AtomicReference<>();
		CountDownLatch go = new CountDownLatch(1);
		CountDownLatch done = new CountDownLatch(threads);

		for (int t = 0; t < threads; t++) {
			long seed = t;
			start(() -> {
				java.util.Random random = new java.util.Random(seed);
				try {
					go.await();
					for (int i = 0; i < iterations; i++) {
						int partition = random.nextInt(partitions);
						lock.write().lock(partition);
						try {
							if (inside[partition].incrementAndGet() != 1)
								violations.incrementAndGet();
							Thread.yield();
							inside[partition].decrementAndGet();
						} finally {
							lock.write().unlock(partition);
						}
					}
				} catch (Throwable e) {
					failure.compareAndSet(null, e);
				} finally {
					done.countDown();
				}
			});
		}

		go.countDown();
		assertTrue(done.await(45, TimeUnit.SECONDS), "threads did not finish - the lock deadlocked");

		if (failure.get() != null)
			throw new AssertionError("a worker threw", failure.get());
		assertEquals(0, violations.get(), "two threads were inside the same partition at once");

		for (int i = 0; i < partitions; i++)
			assertFalse(lock.write().isLocked(i), "partition " + i + " was still locked after every thread finished");
	}

	@Test
	@Timeout(30)
	void lockAllBlocksEveryPartition() throws Exception {
		PartitionedReadWriteLock lock = new PartitionedReadWriteLock();
		CountDownLatch acquired = new CountDownLatch(1);

		lock.write().lockAll();
		assertTrue(lock.write().isAllLocked(), "isAllLocked was false while all partitions were locked");
		assertTrue(lock.write().isLocked(42), "isLocked must report true for any partition while all are locked");

		start(() -> {
			lock.write().lock(42);
			acquired.countDown();
			lock.write().unlock(42);
		});
		assertFalse(acquired.await(BLOCKED_MILLIS, TimeUnit.MILLISECONDS),
				"a partition lock was granted while lockAll was held");

		lock.write().unlockAll();
		assertTrue(acquired.await(ACQUIRED_SECONDS, TimeUnit.SECONDS),
				"a partition lock was never granted after unlockAll");
		assertFalse(lock.write().isAllLocked(), "isAllLocked was still true after unlockAll");
	}

	/**
	 * Concurrent global locking, which no other test here covers: every caller must get in and out,
	 * and the lock must read unlocked once they are done.
	 *
	 * <p>This does not pin the lost-update defect that made {@code globallyLocked} an
	 * {@link java.util.concurrent.atomic.AtomicInteger}. That race needs two threads inside one
	 * {@code ++}, which measured at roughly one occurrence per 100k iterations, so a test that
	 * waited for it would pass on the broken code far more often than not. The field's type is what
	 * rules it out; see the log for 2026-09-18 in {@code docs/plans/12-review-fixes.md}.
	 */
	@Test
	@Timeout(60)
	void concurrentGlobalLocksDoNotDeadlockOrLeakState() throws Exception {
		int threads = 8;
		int iterations = 500;

		PartitionedLock lock = new PartitionedReadWriteLock().read();
		AtomicReference<Throwable> failure = new AtomicReference<>();
		CountDownLatch go = new CountDownLatch(1);
		CountDownLatch done = new CountDownLatch(threads);

		for (int t = 0; t < threads; t++) {
			start(() -> {
				try {
					go.await();
					for (int i = 0; i < iterations; i++) {
						lock.lockAll();
						lock.unlockAll();
					}
				} catch (Throwable e) {
					failure.compareAndSet(null, e);
				} finally {
					done.countDown();
				}
			});
		}

		go.countDown();
		assertTrue(done.await(45, TimeUnit.SECONDS), "threads did not finish - the lock deadlocked");
		if (failure.get() != null)
			throw new AssertionError("a worker threw", failure.get());

		assertFalse(lock.isAllLocked(), "isAllLocked was true after every global lock was released");
		lock.lockAll();
		assertTrue(lock.isAllLocked(), "isAllLocked was false while a global lock was held");
		lock.unlockAll();
	}

}
