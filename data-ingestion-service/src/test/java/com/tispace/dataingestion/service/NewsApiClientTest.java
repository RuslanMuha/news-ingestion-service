package com.tispace.dataingestion.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tispace.common.entity.Article;
import com.tispace.common.exception.ExternalApiException;
import com.tispace.dataingestion.adapter.NewsApiAdapter;
import com.tispace.dataingestion.mapper.NewsApiArticleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NewsApiClientTest {
	
	@Mock
	private RestTemplate restTemplate;
	
	@Mock
	private ObjectMapper objectMapper;
	
	@Mock
	private NewsApiArticleMapper newsApiArticleMapper;
	
	@InjectMocks
	private NewsApiClient newsApiClient;
	
	private static final String NEWS_API_URL = "https://newsapi.org/v2/everything";
	private static final String API_KEY = "test-api-key";
	
	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(newsApiClient, "newsApiUrl", NEWS_API_URL);
		ReflectionTestUtils.setField(newsApiClient, "apiKey", API_KEY);
	}
	
	@Test
	void testFetchArticles_Success_ReturnsArticles() throws Exception {
		String keyword = "technology";
		String category = "technology";
		String jsonResponse = createMockJsonResponse();
		NewsApiAdapter adapter = createMockAdapter();
		Article mockArticle = createMockArticle(category);
		
		ResponseEntity<String> responseEntity = new ResponseEntity<>(jsonResponse, HttpStatus.OK);
		
		when(restTemplate.getForEntity(anyString(), eq(String.class))).thenReturn(responseEntity);
		when(objectMapper.readValue(jsonResponse, NewsApiAdapter.class)).thenReturn(adapter);
		when(newsApiArticleMapper.toArticle(any(NewsApiAdapter.ArticleResponse.class))).thenReturn(mockArticle);
		
		List<Article> result = newsApiClient.fetchArticles(keyword, category);
		
		assertNotNull(result);
		assertFalse(result.isEmpty());
		assertEquals("Test Article", result.get(0).getTitle());
		assertEquals(category, result.get(0).getCategory());
		
		verify(restTemplate, times(1)).getForEntity(anyString(), eq(String.class));
		verify(objectMapper, times(1)).readValue(jsonResponse, NewsApiAdapter.class);
		verify(newsApiArticleMapper, times(1)).toArticle(any(NewsApiAdapter.ArticleResponse.class));
		verify(newsApiArticleMapper, times(1)).updateCategory(any(Article.class), eq(category));
	}
	
	@Test
	void testFetchArticles_NonOkStatus_ThrowsException() {
		String keyword = "technology";
		String category = "technology";
		
		ResponseEntity<String> responseEntity = new ResponseEntity<>("", HttpStatus.BAD_REQUEST);
		
		when(restTemplate.getForEntity(anyString(), eq(String.class))).thenReturn(responseEntity);
		
		assertThrows(ExternalApiException.class, () -> newsApiClient.fetchArticles(keyword, category));
	}
	
	@Test
	void testFetchArticles_NonOkStatusInAdapter_ThrowsException() throws Exception {
		String keyword = "technology";
		String category = "technology";
		String jsonResponse = "{\"status\":\"error\"}";
		NewsApiAdapter adapter = new NewsApiAdapter();
		adapter.setStatus("error");
		
		ResponseEntity<String> responseEntity = new ResponseEntity<>(jsonResponse, HttpStatus.OK);
		
		when(restTemplate.getForEntity(anyString(), eq(String.class))).thenReturn(responseEntity);
		when(objectMapper.readValue(jsonResponse, NewsApiAdapter.class)).thenReturn(adapter);
		
		assertThrows(ExternalApiException.class, () -> newsApiClient.fetchArticles(keyword, category));
	}
	
	@Test
	void testFetchArticles_Exception_ThrowsExternalApiException() {
		String keyword = "technology";
		String category = "technology";
		
		when(restTemplate.getForEntity(anyString(), eq(String.class)))
			.thenThrow(new RuntimeException("Connection error"));
		
		assertThrows(ExternalApiException.class, () -> newsApiClient.fetchArticles(keyword, category));
	}
	
	@Test
	void testFetchArticles_WithNullKeyword_StillReturnsArticles() throws Exception {
		String category = "technology";
		String jsonResponse = createMockJsonResponse();
		NewsApiAdapter adapter = createMockAdapter();
		Article mockArticle = createMockArticle(category);
		
		ResponseEntity<String> responseEntity = new ResponseEntity<>(jsonResponse, HttpStatus.OK);
		
		when(restTemplate.getForEntity(anyString(), eq(String.class))).thenReturn(responseEntity);
		when(objectMapper.readValue(jsonResponse, NewsApiAdapter.class)).thenReturn(adapter);
		when(newsApiArticleMapper.toArticle(any(NewsApiAdapter.ArticleResponse.class))).thenReturn(mockArticle);
		
		List<Article> result = newsApiClient.fetchArticles(null, category);
		
		assertNotNull(result);
		assertFalse(result.isEmpty());
		verify(restTemplate, times(1)).getForEntity(anyString(), eq(String.class));
		verify(newsApiArticleMapper, times(1)).toArticle(any(NewsApiAdapter.ArticleResponse.class));
	}
	
	@Test
	void testFetchArticles_WithNullCategory_ReturnsArticlesWithDefaultCategory() throws Exception {
		String keyword = "technology";
		String jsonResponse = createMockJsonResponse();
		NewsApiAdapter adapter = createMockAdapter();
		Article mockArticle = createMockArticle(null);
		
		ResponseEntity<String> responseEntity = new ResponseEntity<>(jsonResponse, HttpStatus.OK);
		
		when(restTemplate.getForEntity(anyString(), eq(String.class))).thenReturn(responseEntity);
		when(objectMapper.readValue(jsonResponse, NewsApiAdapter.class)).thenReturn(adapter);
		when(newsApiArticleMapper.toArticle(any(NewsApiAdapter.ArticleResponse.class))).thenReturn(mockArticle);
		
		List<Article> result = newsApiClient.fetchArticles(keyword, null);
		
		assertNotNull(result);
		assertFalse(result.isEmpty());
		verify(newsApiArticleMapper, times(1)).updateCategory(any(Article.class), isNull());
	}
	
	@Test
	void testFetchArticles_EmptyArticlesList_ReturnsEmptyList() throws Exception {
		String keyword = "technology";
		String category = "technology";
		String jsonResponse = "{\"status\":\"ok\",\"articles\":[]}";
		NewsApiAdapter adapter = new NewsApiAdapter();
		adapter.setStatus("ok");
		adapter.setArticles(new ArrayList<>());
		
		ResponseEntity<String> responseEntity = new ResponseEntity<>(jsonResponse, HttpStatus.OK);
		
		when(restTemplate.getForEntity(anyString(), eq(String.class))).thenReturn(responseEntity);
		when(objectMapper.readValue(jsonResponse, NewsApiAdapter.class)).thenReturn(adapter);
		
		List<Article> result = newsApiClient.fetchArticles(keyword, category);
		
		assertNotNull(result);
		assertTrue(result.isEmpty());
	}
	
	@Test
	void testFetchArticles_ArticleWithoutTitle_ExcludesFromResult() throws Exception {
		String keyword = "technology";
		String category = "technology";
		NewsApiAdapter adapter = createMockAdapter();
		adapter.getArticles().get(0).setTitle(null); // Remove title
		Article mockArticle = new Article();
		mockArticle.setTitle(null); // Article without title
		mockArticle.setDescription("Test Description");
		
		String jsonResponse = "{\"status\":\"ok\",\"articles\":[]}";
		ResponseEntity<String> responseEntity = new ResponseEntity<>(jsonResponse, HttpStatus.OK);
		
		when(restTemplate.getForEntity(anyString(), eq(String.class))).thenReturn(responseEntity);
		when(objectMapper.readValue(jsonResponse, NewsApiAdapter.class)).thenReturn(adapter);
		when(newsApiArticleMapper.toArticle(any(NewsApiAdapter.ArticleResponse.class))).thenReturn(mockArticle);
		
		List<Article> result = newsApiClient.fetchArticles(keyword, category);
		
		assertNotNull(result);
		// Article without title should be excluded
		assertTrue(result.isEmpty());
		verify(restTemplate, times(1)).getForEntity(anyString(), eq(String.class));
		verify(newsApiArticleMapper, times(1)).toArticle(any(NewsApiAdapter.ArticleResponse.class));
	}
	
	@Test
	void testGetApiName_ReturnsNewsAPI() {
		String apiName = newsApiClient.getApiName();
		
		assertEquals("NewsAPI", apiName);
	}
	
	private String createMockJsonResponse() {
		return "{\"status\":\"ok\",\"totalResults\":1,\"articles\":[{\"title\":\"Test Article\",\"description\":\"Test Description\",\"author\":\"Test Author\",\"publishedAt\":\"2025-01-18T10:00:00Z\"}]}";
	}
	
	private NewsApiAdapter createMockAdapter() {
		NewsApiAdapter adapter = new NewsApiAdapter();
		adapter.setStatus("ok");
		adapter.setTotalResults(1);
		
		NewsApiAdapter.ArticleResponse articleResponse = new NewsApiAdapter.ArticleResponse();
		articleResponse.setTitle("Test Article");
		articleResponse.setDescription("Test Description");
		articleResponse.setAuthor("Test Author");
		articleResponse.setPublishedAt("2025-01-18T10:00:00Z");
		
		List<NewsApiAdapter.ArticleResponse> articles = new ArrayList<>();
		articles.add(articleResponse);
		adapter.setArticles(articles);
		
		return adapter;
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

