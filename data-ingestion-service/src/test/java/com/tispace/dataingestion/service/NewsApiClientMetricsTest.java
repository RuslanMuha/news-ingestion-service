package com.tispace.dataingestion.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class NewsApiClientMetricsTest {

	private MeterRegistry meterRegistry;
	private NewsApiClientMetrics metrics;

	@BeforeEach
	void setUp() {
		meterRegistry = new SimpleMeterRegistry();
		metrics = new NewsApiClientMetrics(meterRegistry);
	}

	@Test
	void testOnRequest_IncrementsCounter() {
		long initialCount = getCounterCount("external_api_requests_total", "client", "newsapi");

		metrics.onRequest();

		long finalCount = getCounterCount("external_api_requests_total", "client", "newsapi");
		assertEquals(initialCount + 1, finalCount);
	}

	@Test
	void testOnError_IncrementsCounter() {
		long initialCount = getCounterCount("external_api_errors_total", "client", "newsapi");

		metrics.onError();

		long finalCount = getCounterCount("external_api_errors_total", "client", "newsapi");
		assertEquals(initialCount + 1, finalCount);
	}

	@Test
	void testOnFallback_IncrementsCounter() {
		long initialCount = getCounterCount("external_api_fallback_total", "client", "newsapi");

		metrics.onFallback();

		long finalCount = getCounterCount("external_api_fallback_total", "client", "newsapi");
		assertEquals(initialCount + 1, finalCount);
	}

	@Test
	void testRecordLatency_RecordsTime() throws Exception {
		Callable<String> callable = () -> {
			Thread.sleep(100);
			return "result";
		};

		String result = metrics.recordLatency(callable);

		assertEquals("result", result);

		// Check that timer was recorded
		Timer timer = meterRegistry.find("external_api_latency_seconds")
			.tag("client", "newsapi")
			.timer();
		assertNotNull(timer);
		assertTrue(timer.count() > 0);
		assertTrue(timer.totalTime(TimeUnit.MILLISECONDS) >= 100);
	}

	@Test
	void testRecordLatency_WithException_PropagatesException() {
		Callable<String> callable = () -> {
			throw new RuntimeException("Test exception");
		};

		assertThrows(RuntimeException.class, () -> metrics.recordLatency(callable));
	}

	@Test
	void testMultipleCalls_AccumulateCorrectly() {
		long initialRequestCount = getCounterCount("external_api_requests_total", "client", "newsapi");
		long initialErrorCount = getCounterCount("external_api_errors_total", "client", "newsapi");
		long initialFallbackCount = getCounterCount("external_api_fallback_total", "client", "newsapi");

		metrics.onRequest();
		metrics.onRequest();
		metrics.onError();
		metrics.onFallback();
		metrics.onFallback();
		metrics.onFallback();

		long finalRequestCount = getCounterCount("external_api_requests_total", "client", "newsapi");
		long finalErrorCount = getCounterCount("external_api_errors_total", "client", "newsapi");
		long finalFallbackCount = getCounterCount("external_api_fallback_total", "client", "newsapi");

		assertEquals(initialRequestCount + 2, finalRequestCount);
		assertEquals(initialErrorCount + 1, finalErrorCount);
		assertEquals(initialFallbackCount + 3, finalFallbackCount);
	}

	private long getCounterCount(String name, String tagKey, String tagValue) {
		Counter counter = meterRegistry.find(name)
			.tag(tagKey, tagValue)
			.counter();
		return counter != null ? (long) counter.count() : 0;
	}
}


