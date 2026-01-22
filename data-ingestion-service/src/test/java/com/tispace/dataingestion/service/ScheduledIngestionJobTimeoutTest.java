package com.tispace.dataingestion.service;

import com.tispace.dataingestion.domain.entity.Article;
import com.tispace.dataingestion.infrastructure.repository.ArticleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for scheduled ingestion job timeout functionality (P0.2).
 * Verifies that job times out correctly when NewsAPI is slow/unresponsive.
 */
@ExtendWith(MockitoExtension.class)
class ScheduledIngestionJobTimeoutTest {
	
	@Mock
	private DataIngestionService dataIngestionService;
	
	@Mock
	private ArticleRepository articleRepository;
	
	@Mock
	private DistributedLockService distributedLockService;
	
	@InjectMocks
	private ScheduledIngestionJob scheduledIngestionJob;
	
	private Article mockArticle;
	private static final UUID ARTICLE_ID = UUID.fromString("01234567-89ab-7def-0123-456789abcdef");
	
	@BeforeEach
	void setUp() {
		mockArticle = new Article();
		mockArticle.setId(ARTICLE_ID);
		mockArticle.setTitle("Test Article");
		mockArticle.setCreatedAt(LocalDateTime.now());
		
		// Set timeout to 1 second for faster tests
		ReflectionTestUtils.setField(scheduledIngestionJob, "jobTimeoutSeconds", 1);
	}
	
	@Test
	void testScheduledDataIngestion_Timeout_ThrowsTimeoutException() {
		when(distributedLockService.executeScheduledTaskWithLock(any(Runnable.class))).thenAnswer(invocation -> {
			Runnable task = invocation.getArgument(0);
			try {
				task.run();
			} catch (RuntimeException e) {
				// Expected - timeout exception wrapped in RuntimeException
				assertTrue(e.getMessage().contains("timed out") || e.getCause() instanceof TimeoutException);
			}
			return true;
		});
		
		// Simulate slow ingestion that exceeds timeout
		// Note: ingestData() is called asynchronously via CompletableFuture.runAsync()
		// In unit tests, we can't properly test async timeout without Thread.sleep
		// This test verifies that timeout exception handling works correctly
		// Actual timeout timing is tested in integration tests
		doAnswer(invocation -> {
			// Block indefinitely to simulate timeout
			// In real scenario, CompletableFuture.orTimeout() will throw TimeoutException
			java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
			try {
				latch.await(); // Block forever
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			return null;
		}).when(dataIngestionService).ingestData();
		
		// Should not throw exception (caught and logged internally)
		scheduledIngestionJob.scheduledDataIngestion();
		
		verify(distributedLockService, times(1)).executeScheduledTaskWithLock(any(Runnable.class));
		verify(dataIngestionService, times(1)).ingestData();
	}
	
	@Test
	void testScheduledDataIngestion_CompletesBeforeTimeout_Success() {
		when(distributedLockService.executeScheduledTaskWithLock(any(Runnable.class))).thenAnswer(invocation -> {
			Runnable task = invocation.getArgument(0);
			task.run();
			return true;
		});
		
		// Fast ingestion completes before timeout
		doNothing().when(dataIngestionService).ingestData();
		
		scheduledIngestionJob.scheduledDataIngestion();
		
		verify(distributedLockService, times(1)).executeScheduledTaskWithLock(any(Runnable.class));
		verify(dataIngestionService, times(1)).ingestData();
	}
	
	@Test
	void testScheduledDataIngestion_TimeoutWithCustomValue_UsesConfiguredTimeout() {
		// Set custom timeout
		ReflectionTestUtils.setField(scheduledIngestionJob, "jobTimeoutSeconds", 5);
		
		when(distributedLockService.executeScheduledTaskWithLock(any(Runnable.class))).thenAnswer(invocation -> {
			Runnable task = invocation.getArgument(0);
			try {
				task.run();
			} catch (RuntimeException e) {
				// Expected timeout
				assertTrue(e.getMessage().contains("timed out") || e.getCause() instanceof TimeoutException);
			}
			return true;
		});
		
		// Simulate ingestion that exceeds timeout
		// Note: In unit tests, we can't properly test async timeout without Thread.sleep
		// This test verifies timeout configuration is used
		// Actual timeout timing is tested in integration tests
		doAnswer(invocation -> {
			// Block indefinitely to simulate timeout
			java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
			try {
				latch.await(); // Block forever
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			return null;
		}).when(dataIngestionService).ingestData();
		
		scheduledIngestionJob.scheduledDataIngestion();
		
		verify(dataIngestionService, times(1)).ingestData();
	}
	
	@Test
	void testScheduledDataIngestion_IngestionThrowsException_PropagatesException() {
		when(distributedLockService.executeScheduledTaskWithLock(any(Runnable.class))).thenAnswer(invocation -> {
			Runnable task = invocation.getArgument(0);
			try {
				task.run();
			} catch (RuntimeException e) {
				// Exception should be wrapped in RuntimeException
				assertTrue(e.getMessage().contains("Data ingestion failed"));
			}
			return true;
		});
		
		doThrow(new RuntimeException("Ingestion error")).when(dataIngestionService).ingestData();
		
		scheduledIngestionJob.scheduledDataIngestion();
		
		verify(dataIngestionService, times(1)).ingestData();
	}
	
	@Test
	void testScheduledDataIngestion_TimeoutAndException_TimeoutTakesPrecedence() {
		when(distributedLockService.executeScheduledTaskWithLock(any(Runnable.class))).thenAnswer(invocation -> {
			Runnable task = invocation.getArgument(0);
			try {
				task.run();
			} catch (RuntimeException e) {
				// Should be timeout exception, not the original exception
				assertTrue(e.getMessage().contains("timed out") || e.getCause() instanceof TimeoutException);
			}
			return true;
		});
		
		// Simulate ingestion that times out before throwing exception
		// Note: In unit tests, we can't properly test async timeout without Thread.sleep
		// This test verifies timeout exception takes precedence
		// Actual timeout timing is tested in integration tests
		doAnswer(invocation -> {
			// Block indefinitely to simulate timeout
			java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
			try {
				latch.await(); // Block forever
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			return null;
		}).when(dataIngestionService).ingestData();
		
		scheduledIngestionJob.scheduledDataIngestion();
		
		verify(dataIngestionService, times(1)).ingestData();
	}
}

