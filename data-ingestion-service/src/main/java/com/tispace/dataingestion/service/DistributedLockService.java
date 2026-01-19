package com.tispace.dataingestion.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.function.Supplier;

/**
 * Service for distributed locking using PostgreSQL advisory locks.
 * Ensures only one instance executes scheduled jobs in a multi-instance environment.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DistributedLockService {
	
	private final EntityManager entityManager;
	
	private static final long SCHEDULER_LOCK_ID = 123456789L; // Fixed ID for scheduler lock
	
	/**
	 * Executes the given supplier with a distributed lock.
	 * If lock cannot be acquired, returns false without executing.
	 * Uses non-blocking pg_try_advisory_xact_lock which immediately returns if lock is available.
	 * 
	 * @param lockId Unique identifier for the lock
	 * @param task Supplier to execute if lock is acquired
	 * @return true if lock was acquired and task executed, false otherwise
	 */
	@Transactional
	public boolean executeWithLock(long lockId, Supplier<Boolean> task) {
		try {
			// Try to acquire advisory lock (non-blocking)
			Query lockQuery = entityManager.createNativeQuery(
				"SELECT pg_try_advisory_xact_lock(?)"
			);
			lockQuery.setParameter(1, lockId);
			
			Boolean lockAcquired = (Boolean) lockQuery.getSingleResult();
			
			if (Boolean.TRUE.equals(lockAcquired)) {
				log.debug("Distributed lock acquired for lockId: {}", lockId);
				try {
					boolean result = task.get();
					log.debug("Task executed successfully with lockId: {}", lockId);
					return result;
				} catch (Exception e) {
					log.error("Error executing task with lockId: {}", lockId, e);
					throw e;
				}
			} else {
				log.debug("Could not acquire distributed lock for lockId: {}, another instance is running", lockId);
				return false;
			}
		} catch (Exception e) {
			log.error("Error acquiring distributed lock for lockId: {}", lockId, e);
			return false;
		}
	}
	
	/**
	 * Executes scheduled data ingestion with distributed lock.
	 * Uses fixed lock ID for scheduler.
	 */
	@Transactional
	public boolean executeScheduledTaskWithLock(Runnable task) {
		return executeWithLock(SCHEDULER_LOCK_ID, () -> {
			task.run();
			return true;
		});
	}
}

