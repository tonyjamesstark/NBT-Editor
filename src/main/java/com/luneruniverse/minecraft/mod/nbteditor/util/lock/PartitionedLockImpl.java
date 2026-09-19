package com.luneruniverse.minecraft.mod.nbteditor.util.lock;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class PartitionedLockImpl implements PartitionedLock {
	
	private volatile boolean stopped;
	private final Lock globalLock;
	private final Map<Integer, Lock> locks;
	private final AtomicInteger globallyLocked;
	private final Map<Integer, Integer> lockedPartitions;
	
	public PartitionedLockImpl() {
		stopped = false;
		globalLock = new ReentrantLock(true);
		locks = new ConcurrentHashMap<>();
		globallyLocked = new AtomicInteger();
		lockedPartitions = new ConcurrentHashMap<>();
	}
	
	/**
	 * Wait until all locks are unlocked, and cause all currently waiting
	 * lock requests to never finish, freezing those threads
	 */
	@Override
	public void stop() {
		stopped = true;
		
		globallyLocked.incrementAndGet();
		globalLock.lock();
		locks.values().forEach(Lock::lock);
	}
	private void checkStop(Integer partition) {
		if (stopped) {
			if (partition != null)
				unlock(partition);
			else
				unlockAll();
			
			while (true) {
				try {
					Thread.sleep(Long.MAX_VALUE);
				} catch (InterruptedException e) {}
			}
		}
	}
	
	@Override
	public void lockAll() {
		// Raised before the global lock is taken, so a pending lockAll already reads as locked.
		// Atomic because that puts it outside the mutex, where two callers can collide on it.
		globallyLocked.incrementAndGet();
		globalLock.lock();
		locks.values().forEach(Lock::lock);
		
		checkStop(null);
	}
	
	@Override
	public void unlockAll() {
		// Not cleared. Entries are per-partition locks that outlive any one acquisition, and a
		// waiter parked on one must still find it here when it comes to release it. No new entry
		// can appear between lockAll and here, since adding one requires the global lock.
		locks.values().forEach(Lock::unlock);
		globalLock.unlock();
		globallyLocked.decrementAndGet();
	}
	
	@Override
	public void lock(int partition) {
		lockedPartitions.compute(partition, (key, value) -> (value == null ? 0 : value) + 1);
		Lock lock;
		globalLock.lock();
		try {
			lock = locks.computeIfAbsent(partition, key -> new ReentrantLock(true));
		} finally {
			globalLock.unlock();
		}

		// Outside the global lock on purpose. Waiting here while holding it would make every
		// other partition queue behind this one, which defeats partitioning entirely.
		lock.lock();

		checkStop(partition);
	}

	@Override
	public void unlock(int partition) {
		Lock lock = locks.get(partition);
		lockedPartitions.compute(partition, (key, value) -> value == 1 ? null : value - 1);
		lock.unlock();
	}
	
	@Override
	public boolean isAllLocked() {
		return globallyLocked.get() > 0;
	}
	
	@Override
	public boolean isLocked(int partition) {
		return globallyLocked.get() > 0 || lockedPartitions.getOrDefault(partition, 0) > 0;
	}
	
}
