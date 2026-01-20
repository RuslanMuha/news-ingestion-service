package com.tispace.queryservice.cache;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class MicrometerCacheMetricsTest {

	private MeterRegistry meterRegistry;
	private MicrometerCacheMetrics metrics;

	@BeforeEach
	void setUp() {
		meterRegistry = new SimpleMeterRegistry();
		metrics = new MicrometerCacheMetrics(meterRegistry);
	}

	@Test
	void testHit_IncrementsCounter() {
		long initialCount = getCounterCount("cache.hits", "cache", "summary");

		metrics.hit();

		long finalCount = getCounterCount("cache.hits", "cache", "summary");
		assertEquals(initialCount + 1, finalCount);
	}

	@Test
	void testMiss_IncrementsCounter() {
		long initialCount = getCounterCount("cache.misses", "cache", "summary");

		metrics.miss();

		long finalCount = getCounterCount("cache.misses", "cache", "summary");
		assertEquals(initialCount + 1, finalCount);
	}

	@Test
	void testError_IncrementsCounter() {
		long initialCount = getCounterCount("cache.errors", "cache", "summary");

		metrics.error();

		long finalCount = getCounterCount("cache.errors", "cache", "summary");
		assertEquals(initialCount + 1, finalCount);
	}

	@Test
	void testUnavailable_IncrementsCounter() {
		long initialCount = getCounterCount("cache.unavailable", "cache", "summary");

		metrics.unavailable();

		long finalCount = getCounterCount("cache.unavailable", "cache", "summary");
		assertEquals(initialCount + 1, finalCount);
	}

	@Test
	void testRecordGet_RecordsTime() throws Exception {
		CacheMetrics.TimerCallable<String> callable = () -> {
			Thread.sleep(100);
			return "result";
		};

		String result = metrics.recordGet(callable);

		assertEquals("result", result);

		// Check that timer was recorded
		Timer timer = meterRegistry.find("cache.get.duration")
			.tag("cache", "summary")
			.timer();
		assertNotNull(timer);
		assertTrue(timer.count() > 0);
		assertTrue(timer.totalTime(TimeUnit.MILLISECONDS) >= 100);
	}

	@Test
	void testRecordGet_WithRuntimeException_PropagatesException() {
		CacheMetrics.TimerCallable<String> callable = () -> {
			throw new RuntimeException("Test exception");
		};

		assertThrows(RuntimeException.class, () -> metrics.recordGet(callable));
	}

	@Test
	void testRecordGet_WithCheckedException_WrapsInRuntimeException() {
		CacheMetrics.TimerCallable<String> callable = () -> {
			throw new Exception("Test checked exception");
		};

		RuntimeException thrown = assertThrows(RuntimeException.class, () -> metrics.recordGet(callable));
		assertTrue(thrown.getCause() instanceof Exception);
		assertEquals("Test checked exception", thrown.getCause().getMessage());
	}

	@Test
	void testRecordPut_RecordsTime() {
		Runnable runnable = () -> {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		};

		metrics.recordPut(runnable);

		// Check that timer was recorded
		Timer timer = meterRegistry.find("cache.put.duration")
			.tag("cache", "summary")
			.timer();
		assertNotNull(timer);
		assertTrue(timer.count() > 0);
		assertTrue(timer.totalTime(TimeUnit.MILLISECONDS) >= 100);
	}

	@Test
	void testRecordPut_WithException_PropagatesException() {
		Runnable runnable = () -> {
			throw new RuntimeException("Test exception");
		};

		assertThrows(RuntimeException.class, () -> metrics.recordPut(runnable));
	}

	@Test
	void testMultipleCalls_AccumulateCorrectly() {
		long initialHitCount = getCounterCount("cache.hits", "cache", "summary");
		long initialMissCount = getCounterCount("cache.misses", "cache", "summary");
		long initialErrorCount = getCounterCount("cache.errors", "cache", "summary");
		long initialUnavailableCount = getCounterCount("cache.unavailable", "cache", "summary");

		metrics.hit();
		metrics.hit();
		metrics.miss();
		metrics.error();
		metrics.error();
		metrics.error();
		metrics.unavailable();
		metrics.unavailable();

		long finalHitCount = getCounterCount("cache.hits", "cache", "summary");
		long finalMissCount = getCounterCount("cache.misses", "cache", "summary");
		long finalErrorCount = getCounterCount("cache.errors", "cache", "summary");
		long finalUnavailableCount = getCounterCount("cache.unavailable", "cache", "summary");

		assertEquals(initialHitCount + 2, finalHitCount);
		assertEquals(initialMissCount + 1, finalMissCount);
		assertEquals(initialErrorCount + 3, finalErrorCount);
		assertEquals(initialUnavailableCount + 2, finalUnavailableCount);
	}

	@Test
	void testRecordGet_MultipleCalls_AccumulateTime() throws Exception {
		CacheMetrics.TimerCallable<String> callable1 = () -> {
			Thread.sleep(50);
			return "result1";
		};
		CacheMetrics.TimerCallable<String> callable2 = () -> {
			Thread.sleep(50);
			return "result2";
		};

		metrics.recordGet(callable1);
		metrics.recordGet(callable2);

		Timer timer = meterRegistry.find("cache.get.duration")
			.tag("cache", "summary")
			.timer();
		assertNotNull(timer);
		assertEquals(2, timer.count());
		assertTrue(timer.totalTime(TimeUnit.MILLISECONDS) >= 100);
	}

	private long getCounterCount(String name, String tagKey, String tagValue) {
		Counter counter = meterRegistry.find(name)
			.tag(tagKey, tagValue)
			.counter();
		return counter != null ? (long) counter.count() : 0;
	}
}

