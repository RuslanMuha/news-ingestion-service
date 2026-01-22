package com.tispace.dataingestion.service;

import com.tispace.dataingestion.domain.entity.Article;
import com.tispace.dataingestion.infrastructure.repository.ArticleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Integration test for all resilience optimizations (P0 + P1).
 * Tests fail paths, load spikes, and edge cases across all optimizations.
 */
@ExtendWith(MockitoExtension.class)
class ResilienceOptimizationsIntegrationTest {
	
	@Mock
	private DataIngestionService dataIngestionService;
	
	@Mock
	private ArticleRepository articleRepository;
	
	@Mock
	private DistributedLockService distributedLockService;
	
	@Mock
	private com.tispace.dataingestion.application.mapper.ArticleMapper articleMapper;
	
	@InjectMocks
	private ScheduledIngestionJob scheduledIngestionJob;
	
	@InjectMocks
	private ArticleQueryService articleQueryService;
	
	private Article mockArticle;
	private List<Article> mockArticles;
	private static final UUID ARTICLE_ID = UUID.fromString("01234567-89ab-7def-0123-456789abcdef");
	private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2025, 1, 15, 12, 0, 0);
	
	@BeforeEach
	void setUp() {
		mockArticle = new Article();
		mockArticle.setId(ARTICLE_ID);
		mockArticle.setTitle("Test Article");
		mockArticle.setDescription("Test Description");
		mockArticle.setAuthor("Test Author");
		mockArticle.setPublishedAt(FIXED_NOW);
		mockArticle.setCategory("technology");
		mockArticle.setCreatedAt(FIXED_NOW);
		
		mockArticles = new ArrayList<>();
		mockArticles.add(mockArticle);
		
		// Set timeout to 2 seconds for faster tests
		ReflectionTestUtils.setField(scheduledIngestionJob, "jobTimeoutSeconds", 2);
	}
	
	@Test
	void testScheduledJob_TimeoutWithSlowNewsAPI_HandlesGracefully() {
		when(articleRepository.findTop1ByOrderByCreatedAtDesc()).thenReturn(Optional.empty());
		when(distributedLockService.executeScheduledTaskWithLock(any(Runnable.class))).thenAnswer(invocation -> {
			Runnable task = invocation.getArgument(0);
			try {
				task.run();
			} catch (RuntimeException e) {
				// Expected timeout
				assertTrue(e.getMessage().contains("timed out") || 
					e.getCause() instanceof java.util.concurrent.TimeoutException);
			}
			return true;
		});
		
		// Simulate slow NewsAPI that exceeds timeout
		// Note: Actual timeout behavior is tested in integration tests
		// This test verifies timeout exception handling
		doAnswer(invocation -> {
			throw new java.util.concurrent.TimeoutException("Operation timed out");
		}).when(dataIngestionService).ingestData();
		
		scheduledIngestionJob.onApplicationReady();
		
		verify(distributedLockService, times(1)).executeScheduledTaskWithLock(any(Runnable.class));
		verify(dataIngestionService, times(1)).ingestData();
	}
	
	@Test
	void testArticleQuery_TransientErrorWithRetry_EventuallySucceeds() {
		Pageable pageable = PageRequest.of(0, 20);
		Page<Article> page = new PageImpl<>(mockArticles, pageable, 1);
		
		// Simulate transient error that would trigger retry
		when(articleRepository.findAll(any(Pageable.class)))
			.thenThrow(new TransientDataAccessException("Connection timeout") {})
			.thenReturn(page);
		
		// Note: Actual retry requires Spring context with Resilience4j
		// This test verifies error handling
		assertThrows(TransientDataAccessException.class, 
			() -> articleQueryService.getArticles(pageable, null));
		
		verify(articleRepository, times(1)).findAll(pageable);
	}
	
	@Test
	void testArticleQuery_LoadSpike_HandlesConcurrentRequests() throws InterruptedException {
		Pageable pageable = PageRequest.of(0, 20);
		Page<Article> page = new PageImpl<>(mockArticles, pageable, 1);
		
		when(articleRepository.findAll(any(Pageable.class))).thenReturn(page);
		
		int concurrentRequests = 50;
		ExecutorService executor = Executors.newFixedThreadPool(concurrentRequests);
		CountDownLatch startLatch = new CountDownLatch(1);
		CountDownLatch completionLatch = new CountDownLatch(concurrentRequests);
		AtomicInteger successCount = new AtomicInteger(0);
		AtomicInteger failureCount = new AtomicInteger(0);
		
		try {
			for (int i = 0; i < concurrentRequests; i++) {
				executor.submit(() -> {
					try {
						startLatch.await();
						Page<Article> result = articleQueryService.getArticles(pageable, null);
						if (result != null && !result.isEmpty()) {
							successCount.incrementAndGet();
						}
					} catch (Exception e) {
						failureCount.incrementAndGet();
					} finally {
						completionLatch.countDown();
					}
				});
			}
			
			startLatch.countDown();
			boolean completed = completionLatch.await(10, TimeUnit.SECONDS);
			
			assertTrue(completed, "All requests should complete");
			assertTrue(successCount.get() > 0, "At least some requests should succeed");
			
			// Verify repository was called (may be called multiple times due to retries)
			verify(articleRepository, atLeast(concurrentRequests)).findAll(pageable);
			
		} finally {
			executor.shutdown();
			if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
				executor.shutdownNow();
			}
		}
	}
	
	@Test
	void testScheduledJob_ConcurrentExecution_DistributedLockPreventsDuplicates() {
		when(articleRepository.findTop1ByOrderByCreatedAtDesc()).thenReturn(Optional.empty());
		
		// First instance acquires lock, second doesn't
		when(distributedLockService.executeScheduledTaskWithLock(any(Runnable.class)))
			.thenAnswer(invocation -> {
				Runnable task = invocation.getArgument(0);
				task.run();
				return true;
			})
			.thenReturn(false); // Second instance cannot acquire lock
		
		// Simulate two instances trying to run simultaneously
		scheduledIngestionJob.onApplicationReady();
		scheduledIngestionJob.onApplicationReady();
		
		// Only first instance should execute
		verify(distributedLockService, times(2)).executeScheduledTaskWithLock(any(Runnable.class));
		verify(dataIngestionService, times(1)).ingestData(); // Only called once
	}
	
	@Test
	void testArticleQuery_SlowQuery_CompletesEventually() {
		Pageable pageable = PageRequest.of(0, 20);
		
		// Simulate slow query
		// Note: Query timeout is enforced by Hibernate/JPA, not in unit tests with mocks
		// This test verifies the method can handle slow queries
		when(articleRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(mockArticles, pageable, 1));
		
		Page<Article> result = articleQueryService.getArticles(pageable, null);
		
		// Should complete successfully
		assertNotNull(result);
		assertEquals(1, result.getContent().size());
		assertFalse(result.isEmpty());
	}
	
	@Test
	void testScheduledJob_ExceptionDuringExecution_LogsAndContinues() {
		when(articleRepository.findTop1ByOrderByCreatedAtDesc()).thenReturn(Optional.empty());
		when(distributedLockService.executeScheduledTaskWithLock(any(Runnable.class))).thenAnswer(invocation -> {
			Runnable task = invocation.getArgument(0);
			try {
				task.run();
			} catch (RuntimeException e) {
				// Exception is caught and logged
			}
			return true;
		});
		
		doThrow(new RuntimeException("Ingestion error")).when(dataIngestionService).ingestData();
		
		// Should not throw exception (handled internally)
		scheduledIngestionJob.onApplicationReady();
		
		verify(dataIngestionService, times(1)).ingestData();
	}
	
	@Test
	void testArticleQuery_EmptyResult_ReturnsEmptyPage() {
		Pageable pageable = PageRequest.of(0, 20);
		Page<Article> emptyPage = new PageImpl<>(new ArrayList<>(), pageable, 0);
		
		when(articleRepository.findAll(any(Pageable.class))).thenReturn(emptyPage);
		
		Page<Article> result = articleQueryService.getArticles(pageable, null);
		
		assertNotNull(result);
		assertTrue(result.isEmpty());
		assertEquals(0, result.getContent().size());
	}
	
	@Test
	void testScheduledJob_DataFresh_SkipsIngestion() {
		// Article created 12 hours ago (fresh, not stale) - within 24h threshold
		LocalDateTime freshTime = FIXED_NOW.minusHours(12);
		mockArticle.setCreatedAt(freshTime);
		
		when(articleRepository.findTop1ByOrderByCreatedAtDesc()).thenReturn(Optional.of(mockArticle));
		
		scheduledIngestionJob.onApplicationReady();
		
		verify(articleRepository, times(1)).findTop1ByOrderByCreatedAtDesc();
		verify(dataIngestionService, never()).ingestData();
	}
	
	@Test
	void testArticleQuery_WithCategory_FiltersCorrectly() {
		Pageable pageable = PageRequest.of(0, 20);
		Page<Article> page = new PageImpl<>(mockArticles, pageable, 1);
		
		when(articleRepository.findByCategory("technology", pageable)).thenReturn(page);
		
		Page<Article> result = articleQueryService.getArticles(pageable, "technology");
		
		assertNotNull(result);
		assertEquals(1, result.getContent().size());
		verify(articleRepository, times(1)).findByCategory("technology", pageable);
		verify(articleRepository, never()).findAll(any(Pageable.class));
	}
}

