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

import java.sql.SQLTransientException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ArticleQueryService exception handling.
 * Verifies that transient database errors propagate correctly.
 * 
 * Note: These are unit tests that verify exception propagation.
 * Actual retry behavior is handled by Resilience4j AOP and requires
 * Spring context for full integration testing (see ArticleQueryServiceRetryIntegrationTest).
 */
@ExtendWith(MockitoExtension.class)
class ArticleQueryServiceRetryTest {
	
	@Mock
	private ArticleRepository articleRepository;
	
	@Mock
	private com.tispace.dataingestion.application.mapper.ArticleMapper articleMapper;
	
	@InjectMocks
	private ArticleQueryService articleQueryService;
	
	private Article mockArticle;
	private List<Article> mockArticles;
	private static final UUID ARTICLE_ID = UUID.fromString("01234567-89ab-7def-0123-456789abcdef");
	
	@BeforeEach
	void setUp() {
		mockArticle = new Article();
		mockArticle.setId(ARTICLE_ID);
		mockArticle.setTitle("Test Article");
		mockArticle.setDescription("Test Description");
		mockArticle.setAuthor("Test Author");
		mockArticle.setPublishedAt(LocalDateTime.now());
		mockArticle.setCategory("technology");
		
		mockArticles = new ArrayList<>();
		mockArticles.add(mockArticle);
	}
	
	@Test
	void testGetArticles_TransientException_PropagatesException() {
		Pageable pageable = PageRequest.of(0, 20);
		Page<Article> page = new PageImpl<>(mockArticles, pageable, 1);
		
		// Repository throws transient exception
		when(articleRepository.findAll(any(Pageable.class)))
			.thenThrow(new TransientDataAccessException("Connection timeout") {})
			.thenReturn(page);
		
		// Note: In unit tests without Spring context, exception propagates directly
		// With Resilience4j AOP in Spring context, retry would happen automatically
		assertThrows(TransientDataAccessException.class, 
			() -> articleQueryService.getArticles(pageable, null));
		
		verify(articleRepository, times(1)).findAll(pageable);
	}
	
	@Test
	void testGetArticles_TransientException_PropagatesOnFirstAttempt() {
		Pageable pageable = PageRequest.of(0, 20);
		Page<Article> page = new PageImpl<>(mockArticles, pageable, 1);
		
		// Repository throws transient exception multiple times
		when(articleRepository.findAll(any(Pageable.class)))
			.thenThrow(new TransientDataAccessException("Connection timeout") {})
			.thenThrow(new TransientDataAccessException("Connection timeout") {})
			.thenReturn(page);
		
		// Without Resilience4j AOP, exception propagates on first attempt
		// With Resilience4j in Spring context, retry would happen and eventually succeed
		assertThrows(TransientDataAccessException.class, 
			() -> articleQueryService.getArticles(pageable, null));
		
		verify(articleRepository, times(1)).findAll(pageable);
	}
	
	@Test
	void testGetArticleById_TransientException_PropagatesException() {
		// Repository throws transient exception
		when(articleRepository.findById(ARTICLE_ID))
			.thenThrow(new TransientDataAccessException("Connection timeout") {})
			.thenReturn(Optional.of(mockArticle));
		
		// Note: In unit tests without Spring context, exception propagates directly
		// With Resilience4j AOP, retry would happen automatically
		assertThrows(TransientDataAccessException.class, 
			() -> articleQueryService.getArticleById(ARTICLE_ID));
		
		verify(articleRepository, times(1)).findById(ARTICLE_ID);
	}
	
	@Test
	void testGetArticles_SQLTransientException_PropagatesException() {
		Pageable pageable = PageRequest.of(0, 20);
		Page<Article> page = new PageImpl<>(mockArticles, pageable, 1);
		
		// SQLTransientException wrapped in RuntimeException
		when(articleRepository.findAll(any(Pageable.class)))
			.thenThrow(new RuntimeException(new SQLTransientException("Database timeout")))
			.thenReturn(page);
		
		// Exception propagates in unit tests
		// With Resilience4j, SQLTransientException would trigger retry
		assertThrows(RuntimeException.class, 
			() -> articleQueryService.getArticles(pageable, null));
		
		verify(articleRepository, times(1)).findAll(pageable);
	}
	
	@Test
	void testGetArticles_NonTransientException_NoRetry() {
		Pageable pageable = PageRequest.of(0, 20);
		
		// Non-transient exception should not trigger retry
		when(articleRepository.findAll(any(Pageable.class)))
			.thenThrow(new RuntimeException("Permanent error"));
		
		assertThrows(RuntimeException.class, 
			() -> articleQueryService.getArticles(pageable, null));
		
		// Should only be called once (no retry for non-transient exceptions)
		verify(articleRepository, times(1)).findAll(pageable);
	}
	
	@Test
	void testGetArticles_WithCategory_TransientException_PropagatesException() {
		Pageable pageable = PageRequest.of(0, 20);
		Page<Article> page = new PageImpl<>(mockArticles, pageable, 1);
		
		when(articleRepository.findByCategory(anyString(), any(Pageable.class)))
			.thenThrow(new TransientDataAccessException("Connection timeout") {})
			.thenReturn(page);
		
		// Exception propagates in unit tests
		// With Resilience4j, retry would happen automatically
		assertThrows(TransientDataAccessException.class, 
			() -> articleQueryService.getArticles(pageable, "technology"));
		
		verify(articleRepository, times(1)).findByCategory("technology", pageable);
	}
	
	@Test
	void testGetArticleById_Success_NoRetryNeeded() {
		when(articleRepository.findById(ARTICLE_ID)).thenReturn(Optional.of(mockArticle));
		
		Article result = articleQueryService.getArticleById(ARTICLE_ID);
		
		assertNotNull(result);
		assertEquals(ARTICLE_ID, result.getId());
		
		// Should only be called once (no retry needed for success)
		verify(articleRepository, times(1)).findById(ARTICLE_ID);
	}
	
	@Test
	void testGetArticles_Success_NoRetryNeeded() {
		Pageable pageable = PageRequest.of(0, 20);
		Page<Article> page = new PageImpl<>(mockArticles, pageable, 1);
		
		when(articleRepository.findAll(any(Pageable.class))).thenReturn(page);
		
		Page<Article> result = articleQueryService.getArticles(pageable, null);
		
		assertNotNull(result);
		assertEquals(1, result.getContent().size());
		
		// Should only be called once (no retry needed for success)
		verify(articleRepository, times(1)).findAll(pageable);
	}
}

