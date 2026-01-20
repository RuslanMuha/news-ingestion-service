package com.tispace.dataingestion.service;

import com.tispace.common.entity.Article;
import com.tispace.common.exception.ExternalApiException;
import com.tispace.common.exception.SerializationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NewsApiClientTest {
	
	@Mock
	private NewsApiClientCore core;
	
	@Mock
	private NewsApiClientMetrics metrics;
	
	private NewsApiClient newsApiClient;
	
	@BeforeEach
	void setUp() {
		newsApiClient = new NewsApiClient(core, metrics);
	}
	
	@Test
	void testFetchArticles_Success_ReturnsArticles() throws Exception {
		String keyword = "technology";
		String category = "technology";
		Article mockArticle = createMockArticle(category);
		List<Article> expectedArticles = List.of(mockArticle);
		
		when(core.fetchArticles(keyword, category)).thenReturn(expectedArticles);
		doNothing().when(metrics).onRequest();
		when(metrics.recordLatency(any())).thenAnswer(invocation -> {
			java.util.concurrent.Callable<List<Article>> callable = invocation.getArgument(0);
			return callable.call();
		});
		
		List<Article> result = newsApiClient.fetchArticles(keyword, category);
		
		assertNotNull(result);
		assertFalse(result.isEmpty());
		assertEquals("Test Article", result.get(0).getTitle());
		assertEquals(category, result.get(0).getCategory());
		
		verify(metrics, times(1)).onRequest();
		verify(metrics, times(1)).recordLatency(any());
		verify(metrics, never()).onError();
		verify(metrics, never()).onFallback();
		verify(core, times(1)).fetchArticles(keyword, category);
	}
	
	@Test
	void testFetchArticles_ExternalApiException_PropagatesException() throws Exception {
		String keyword = "technology";
		String category = "technology";
		ExternalApiException exception = new ExternalApiException("API error");
		
		doNothing().when(metrics).onRequest();
		doNothing().when(metrics).onError();
		when(metrics.recordLatency(any())).thenAnswer(invocation -> {
			java.util.concurrent.Callable<List<Article>> callable = invocation.getArgument(0);
			return callable.call();
		});
		when(core.fetchArticles(keyword, category)).thenThrow(exception);
		
		assertThrows(ExternalApiException.class, () -> newsApiClient.fetchArticles(keyword, category));
		
		verify(metrics, times(1)).onRequest();
		verify(metrics, times(1)).onError();
		verify(metrics, times(1)).recordLatency(any());
		verify(core, times(1)).fetchArticles(keyword, category);
	}
	
	@Test
	void testFetchArticles_SerializationException_PropagatesException() throws Exception {
		String keyword = "technology";
		String category = "technology";
		SerializationException exception = new SerializationException("Serialization error");
		
		doNothing().when(metrics).onRequest();
		doNothing().when(metrics).onError();
		when(metrics.recordLatency(any())).thenAnswer(invocation -> {
			java.util.concurrent.Callable<List<Article>> callable = invocation.getArgument(0);
			return callable.call();
		});
		when(core.fetchArticles(keyword, category)).thenThrow(exception);
		
		assertThrows(SerializationException.class, () -> newsApiClient.fetchArticles(keyword, category));
		
		verify(metrics, times(1)).onRequest();
		verify(metrics, times(1)).onError();
		verify(metrics, times(1)).recordLatency(any());
		verify(core, times(1)).fetchArticles(keyword, category);
	}
	
	@Test
	void testFetchArticles_UnexpectedException_WrapsInExternalApiException() throws Exception {
		String keyword = "technology";
		String category = "technology";
		RuntimeException exception = new RuntimeException("Unexpected error");
		
		doNothing().when(metrics).onRequest();
		doNothing().when(metrics).onError();
		when(metrics.recordLatency(any())).thenAnswer(invocation -> {
			java.util.concurrent.Callable<List<Article>> callable = invocation.getArgument(0);
			return callable.call();
		});
		when(core.fetchArticles(keyword, category)).thenThrow(exception);
		
		ExternalApiException thrown = assertThrows(ExternalApiException.class, 
			() -> newsApiClient.fetchArticles(keyword, category));
		
		assertEquals("Unexpected error fetching articles from NewsAPI", thrown.getMessage());
		assertEquals(exception, thrown.getCause());
		
		verify(metrics, times(1)).onRequest();
		verify(metrics, times(1)).onError();
		verify(metrics, times(1)).recordLatency(any());
		verify(core, times(1)).fetchArticles(keyword, category);
	}
	
	@Test
	void testFetchArticles_WithNullKeyword_StillReturnsArticles() throws Exception {
		String category = "technology";
		Article mockArticle = createMockArticle(category);
		List<Article> expectedArticles = List.of(mockArticle);
		
		doNothing().when(metrics).onRequest();
		when(metrics.recordLatency(any())).thenAnswer(invocation -> {
			java.util.concurrent.Callable<List<Article>> callable = invocation.getArgument(0);
			return callable.call();
		});
		when(core.fetchArticles(null, category)).thenReturn(expectedArticles);
		
		List<Article> result = newsApiClient.fetchArticles(null, category);
		
		assertNotNull(result);
		assertFalse(result.isEmpty());
		verify(core, times(1)).fetchArticles(null, category);
	}
	
	@Test
	void testFetchArticles_WithNullCategory_ReturnsArticles() throws Exception {
		String keyword = "technology";
		Article mockArticle = createMockArticle(null);
		List<Article> expectedArticles = List.of(mockArticle);
		
		doNothing().when(metrics).onRequest();
		when(metrics.recordLatency(any())).thenAnswer(invocation -> {
			java.util.concurrent.Callable<List<Article>> callable = invocation.getArgument(0);
			return callable.call();
		});
		when(core.fetchArticles(keyword, null)).thenReturn(expectedArticles);
		
		List<Article> result = newsApiClient.fetchArticles(keyword, null);
		
		assertNotNull(result);
		assertFalse(result.isEmpty());
		verify(core, times(1)).fetchArticles(keyword, null);
	}
	
	@Test
	void testFetchArticles_EmptyArticlesList_ReturnsEmptyList() throws Exception {
		String keyword = "technology";
		String category = "technology";
		List<Article> emptyList = new ArrayList<>();
		
		doNothing().when(metrics).onRequest();
		when(metrics.recordLatency(any())).thenAnswer(invocation -> {
			java.util.concurrent.Callable<List<Article>> callable = invocation.getArgument(0);
			return callable.call();
		});
		when(core.fetchArticles(keyword, category)).thenReturn(emptyList);
		
		List<Article> result = newsApiClient.fetchArticles(keyword, category);
		
		assertNotNull(result);
		assertTrue(result.isEmpty());
		verify(core, times(1)).fetchArticles(keyword, category);
	}
	
	@Test
	void testGetApiName_ReturnsNewsAPI() {
		String apiName = newsApiClient.getApiName();
		
		assertEquals("NewsAPI", apiName);
	}
	
	
	@Test
	void testFetchArticlesFallback_ReturnsEmptyList() {
		String keyword = "technology";
		String category = "technology";
		Exception cause = new RuntimeException("Service unavailable");
		
		doNothing().when(metrics).onFallback();
		doNothing().when(metrics).onError();
		
		// Fallback returns empty list
		List<Article> result = newsApiClient.fetchArticlesFallback(keyword, category, cause);
		
		assertNotNull(result);
		assertTrue(result.isEmpty());
		verify(metrics, times(1)).onFallback();
		verify(metrics, times(1)).onError();
	}
	
	private Article createMockArticle(String category) {
		Article article = new Article();
		article.setTitle("Test Article");
		article.setDescription("Test Description");
		article.setAuthor("Test Author");
		article.setPublishedAt(LocalDateTime.now());
		article.setCategory(category);
		return article;
	}
}

