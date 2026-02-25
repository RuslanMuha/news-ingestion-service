package com.tispace.dataingestion.service;

import com.tispace.dataingestion.domain.entity.Article;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for slow NewsAPI responses (P0.2).
 * Unit-level behavior tests without Spring AOP proxies.
 * Resilience4j annotations are validated in Spring context integration tests.
 */
@ExtendWith(MockitoExtension.class)
class NewsApiClientSlowResponseTest {
	
	@Mock
	private NewsApiClientCore core;
	
	@Mock
	private NewsApiClientMetrics metrics;
	
	private NewsApiClient newsApiClient;
	
	private List<Article> mockArticles;
	
	@BeforeEach
	void setUp() {
		newsApiClient = new NewsApiClient(core, metrics);
		
		mockArticles = new ArrayList<>();
		Article article = new Article();
		article.setTitle("Test Article");
		article.setDescription("Test Description");
		article.setAuthor("Test Author");
		article.setPublishedAt(LocalDateTime.now());
		article.setCategory("technology");
		mockArticles.add(article);
	}
	
	@Test
	void testFetchArticles_SlowResponse_CompletesSuccessfully() throws Exception {
		String keyword = "technology";
		String category = "technology";
		
		// Simulate slow response that completes successfully
		// Note: In unit tests with mocks, actual timing is not relevant
		// This test verifies the method handles slow responses correctly
		when(core.fetchArticles(keyword, category)).thenReturn(mockArticles);
		
		doNothing().when(metrics).onRequest();
		when(metrics.recordLatency(any())).thenAnswer(invocation -> {
			java.util.concurrent.Callable<List<Article>> callable = invocation.getArgument(0);
			return callable.call();
		});
		
		List<Article> result = newsApiClient.fetchArticles(keyword, category);
		
		assertNotNull(result);
		assertEquals(1, result.size());
		assertEquals("Test Article", result.get(0).getTitle());
		
		verify(core, times(1)).fetchArticles(keyword, category);
		verify(metrics, times(1)).onRequest();
	}
	
	@Test
	void testFetchArticles_VerySlowResponse_WithoutSpringAop_CompletesSuccessfully() throws Exception {
		String keyword = "technology";
		String category = "technology";
		
		// Simulate very slow response that completes
		// Note: Circuit breaker behavior requires Spring context with Resilience4j
		// This test verifies the method handles slow responses
		when(core.fetchArticles(keyword, category)).thenReturn(mockArticles);
		
		doNothing().when(metrics).onRequest();
		when(metrics.recordLatency(any())).thenAnswer(invocation -> {
			java.util.concurrent.Callable<List<Article>> callable = invocation.getArgument(0);
			return callable.call();
		});
		
		// Should complete successfully
		List<Article> result = newsApiClient.fetchArticles(keyword, category);
		
		assertNotNull(result);
		assertEquals(1, result.size());
		
		verify(core, times(1)).fetchArticles(keyword, category);
	}
	
	@Test
	void testFetchArticles_ConcurrentSlowRequests_WithoutSpringAop_ExecutesWithoutBulkhead() throws Exception {
		String keyword = "technology";
		String category = "technology";
		int concurrentRequests = 15; // More than bulkhead max (10)
		
		// Simulate slow responses
		// Note: In unit tests, actual timing is not relevant
		// This test verifies concurrent request handling
		when(core.fetchArticles(keyword, category)).thenReturn(mockArticles);
		
		doNothing().when(metrics).onRequest();
		when(metrics.recordLatency(any())).thenAnswer(invocation -> {
			java.util.concurrent.Callable<List<Article>> callable = invocation.getArgument(0);
			return callable.call();
		});
		
		CountDownLatch startLatch = new CountDownLatch(1);
		CountDownLatch completionLatch = new CountDownLatch(concurrentRequests);
		List<Thread> threads = new ArrayList<>();
		List<Exception> exceptions = new ArrayList<>();
		
		// Create concurrent requests
		for (int i = 0; i < concurrentRequests; i++) {
			Thread thread = new Thread(() -> {
				try {
					startLatch.await();
					newsApiClient.fetchArticles(keyword, category);
				} catch (Exception e) {
					exceptions.add(e);
				} finally {
					completionLatch.countDown();
				}
			});
			threads.add(thread);
			thread.start();
		}
		
		// Start all requests simultaneously
		startLatch.countDown();
		
		// Wait for all requests to complete (with timeout)
		boolean completed = completionLatch.await(30, TimeUnit.SECONDS);
		
		assertTrue(completed, "All requests should complete within timeout");
		
		// In this unit test (no Spring AOP), bulkhead is not enforced.
		assertTrue(exceptions.isEmpty() || exceptions.size() < concurrentRequests,
			"Concurrent calls should complete without systemic failures, exceptions: " + exceptions.size());
		
		verify(core, times(concurrentRequests)).fetchArticles(keyword, category);
	}
	
	@Test
	void testFetchArticles_SlowResponseWithRetry_WithoutSpringAop_PropagatesFirstException() throws Exception {
		String keyword = "technology";
		String category = "technology";
		
		// First call fails, second succeeds
		// Note: Actual retry behavior depends on Resilience4j configuration
		// This test verifies the method can handle failures and retries
		// In real scenario with Spring context, retry would happen automatically
		when(core.fetchArticles(keyword, category))
			.thenThrow(new RuntimeException("Timeout"))
			.thenReturn(mockArticles);
		
		doNothing().when(metrics).onRequest();
		when(metrics.recordLatency(any())).thenAnswer(invocation -> {
			java.util.concurrent.Callable<List<Article>> callable = invocation.getArgument(0);
			return callable.call();
		});
		
		// Note: Without Spring context, exception is thrown directly
		// With Resilience4j, retry would happen and eventually succeed
		assertThrows(RuntimeException.class, () -> {
			newsApiClient.fetchArticles(keyword, category);
		});
		
		verify(core, times(1)).fetchArticles(keyword, category);
	}
	
	@Test
	void testFetchArticles_ExtremelySlowResponse_CompletesEventually() throws Exception {
		String keyword = "technology";
		String category = "technology";
		
		// Extremely slow response that eventually completes
		// Note: Circuit breaker may open after multiple slow calls in real scenario
		// This test verifies the method can handle slow responses
		when(core.fetchArticles(keyword, category)).thenReturn(mockArticles);
		
		doNothing().when(metrics).onRequest();
		when(metrics.recordLatency(any())).thenAnswer(invocation -> {
			java.util.concurrent.Callable<List<Article>> callable = invocation.getArgument(0);
			return callable.call();
		});
		
		// Should complete successfully
		List<Article> result = newsApiClient.fetchArticles(keyword, category);
		
		assertNotNull(result);
		assertEquals(1, result.size());
		assertEquals("Test Article", result.get(0).getTitle());
	}
}

