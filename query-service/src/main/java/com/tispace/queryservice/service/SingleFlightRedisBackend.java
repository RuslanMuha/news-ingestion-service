package com.tispace.queryservice.service;

import com.tispace.queryservice.dto.SingleFlightEnvelope;
import java.time.Duration;

/**
 * Redis backend for single-flight pattern operations.
 * Isolates infrastructure details from coordination logic.
 */
public interface SingleFlightRedisBackend {

    enum LockAcquireResult {
        ACQUIRED,
        LOCKED,
        BACKEND_UNAVAILABLE
    }

    /**
     * Attempts to acquire a distributed lock.
     * @return explicit lock acquisition state
     */
    LockAcquireResult tryAcquireLock(String lockKey, String token, Duration ttl);
    
    /**
     * Releases lock safely (only if token matches).
     */
    void releaseLock(String lockKey, String token);
    
    /**
     * Reads cached result.
     * @return envelope if found, null if not cached
     * @throws Exception on Redis error (not on cache miss)
     */
    SingleFlightEnvelope readResult(String resultKey) throws Exception;
    
    /**
     * Stores result with TTL. Logs warning on error but doesn't throw.
     */
    void writeResult(String resultKey, SingleFlightEnvelope envelope, Duration ttl);
}

