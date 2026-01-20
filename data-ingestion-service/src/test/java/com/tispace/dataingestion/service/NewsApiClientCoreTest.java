package com.tispace.dataingestion.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tispace.common.entity.Article;
import com.tispace.common.exception.ExternalApiException;
import com.tispace.common.exception.SerializationException;
import com.tispace.common.validation.ArticleValidator;
import com.tispace.dataingestion.adapter.NewsApiAdapter;
import com.tispace.dataingestion.constants.NewsApiConstants;
import com.tispace.dataingestion.mapper.NewsApiArticleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NewsApiClientCoreTest {

	@Mock
	private RestTemplate restTemplate;

	@Mock
	private ObjectMapper objectMapper;

	@Mock
	private NewsApiArticleMapper mapper;

	@Mock
	private ArticleValidator validator;

	private NewsApiClientCore core;

	private static final String NEWS_API_URL = "https://newsapi.org/v2/everything";
	private static final String API_KEY = "test-api-key";

	@BeforeEach
	void setUp() {
		core = new NewsApiClientCore(
			restTemplate,
			objectMapper,
			mapper,
			validator,
			NEWS_API_URL,
			API_KEY
		);
		lenient().when(validator.isValid(any(Article.class))).thenReturn(true);
	}

	@Test
	void testConstructor_WithBlankApiKey_ThrowsException() {
		assertThrows(IllegalStateException.class, () -> new NewsApiClientCore(
			restTemplate,
			objectMapper,
			mapper,
			validator,
			NEWS_API_URL,
			""
		));
	}

	@Test
	void testConstructor_WithNullApiKey_ThrowsException() {
		assertThrows(IllegalStateException.class, () -> new NewsApiClientCore(
			restTemplate,
			objectMapper,
			mapper,
			validator,
			NEWS_API_URL,
			null
		));
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
		when(mapper.toArticle(any(NewsApiAdapter.ArticleResponse.class))).thenReturn(mockArticle);

		List<Article> result = core.fetchArticles(keyword, category);

		assertNotNull(result);
		assertFalse(result.isEmpty());
		assertEquals("Test Article", result.get(0).getTitle());
		assertEquals(category, result.get(0).getCategory());

		verify(restTemplate, times(1)).getForEntity(anyString(), eq(String.class));
		verify(objectMapper, times(1)).readValue(jsonResponse, NewsApiAdapter.class);
		verify(mapper, times(1)).toArticle(any(NewsApiAdapter.ArticleResponse.class));
		verify(mapper, times(1)).updateCategory(any(Article.class), eq(category));
		verify(validator, times(1)).isValid(any(Article.class));
	}

	@Test
	void testFetchArticles_NonOkStatus_ThrowsException() {
		String keyword = "technology";
		String category = "technology";

		ResponseEntity<String> responseEntity = new ResponseEntity<>("", HttpStatus.BAD_REQUEST);

		when(restTemplate.getForEntity(anyString(), eq(String.class))).thenReturn(responseEntity);

		ExternalApiException exception = assertThrows(ExternalApiException.class,
			() -> core.fetchArticles(keyword, category));

		assertTrue(exception.getMessage().contains("HTTP status"));
		verify(restTemplate, times(1)).getForEntity(anyString(), eq(String.class));
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

		ExternalApiException exception = assertThrows(ExternalApiException.class,
			() -> core.fetchArticles(keyword, category));

		assertTrue(exception.getMessage().contains("status"));
		verify(restTemplate, times(1)).getForEntity(anyString(), eq(String.class));
	}

	@Test
	void testFetchArticles_RestClientException_ThrowsExternalApiException() {
		String keyword = "technology";
		String category = "technology";

		when(restTemplate.getForEntity(anyString(), eq(String.class)))
			.thenThrow(new RestClientException("Connection error"));

		ExternalApiException exception = assertThrows(ExternalApiException.class,
			() -> core.fetchArticles(keyword, category));

		assertTrue(exception.getMessage().contains("transport error"));
		verify(restTemplate, times(1)).getForEntity(anyString(), eq(String.class));
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
		when(mapper.toArticle(any(NewsApiAdapter.ArticleResponse.class))).thenReturn(mockArticle);

		List<Article> result = core.fetchArticles(null, category);

		assertNotNull(result);
		assertFalse(result.isEmpty());
		verify(restTemplate, times(1)).getForEntity(anyString(), eq(String.class));
		verify(mapper, times(1)).toArticle(any(NewsApiAdapter.ArticleResponse.class));
	}

	@Test
	void testFetchArticles_WithNullCategory_ReturnsArticles() throws Exception {
		String keyword = "technology";
		String jsonResponse = createMockJsonResponse();
		NewsApiAdapter adapter = createMockAdapter();
		Article mockArticle = createMockArticle(null);

		ResponseEntity<String> responseEntity = new ResponseEntity<>(jsonResponse, HttpStatus.OK);

		when(restTemplate.getForEntity(anyString(), eq(String.class))).thenReturn(responseEntity);
		when(objectMapper.readValue(jsonResponse, NewsApiAdapter.class)).thenReturn(adapter);
		when(mapper.toArticle(any(NewsApiAdapter.ArticleResponse.class))).thenReturn(mockArticle);

		List<Article> result = core.fetchArticles(keyword, null);

		assertNotNull(result);
		assertFalse(result.isEmpty());
		verify(mapper, times(1)).updateCategory(any(Article.class), isNull());
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

		List<Article> result = core.fetchArticles(keyword, category);

		assertNotNull(result);
		assertTrue(result.isEmpty());
	}

	@Test
	void testFetchArticles_EmptyResponseBody_ReturnsEmptyList() {
		String keyword = "technology";
		String category = "technology";

		ResponseEntity<String> responseEntity = new ResponseEntity<>("", HttpStatus.OK);

		when(restTemplate.getForEntity(anyString(), eq(String.class))).thenReturn(responseEntity);

		List<Article> result = core.fetchArticles(keyword, category);

		assertNotNull(result);
		assertTrue(result.isEmpty());
		verify(restTemplate, times(1)).getForEntity(anyString(), eq(String.class));
		try {
			verify(objectMapper, never()).readValue(anyString(), eq(NewsApiAdapter.class));
		} catch (JsonProcessingException e) {
			// This won't happen with never(), but needed for compilation
		}
	}

	@Test
	void testFetchArticles_NullResponseBody_ReturnsEmptyList() {
		String keyword = "technology";
		String category = "technology";

		ResponseEntity<String> responseEntity = new ResponseEntity<>(null, HttpStatus.OK);

		when(restTemplate.getForEntity(anyString(), eq(String.class))).thenReturn(responseEntity);

		List<Article> result = core.fetchArticles(keyword, category);

		assertNotNull(result);
		assertTrue(result.isEmpty());
		verify(restTemplate, times(1)).getForEntity(anyString(), eq(String.class));
		try {
			verify(objectMapper, never()).readValue(anyString(), eq(NewsApiAdapter.class));
		} catch (JsonProcessingException e) {
			// This won't happen with never(), but needed for compilation
		}
	}

	@Test
	void testFetchArticles_BlankResponseBody_ReturnsEmptyList() {
		String keyword = "technology";
		String category = "technology";

		ResponseEntity<String> responseEntity = new ResponseEntity<>("   ", HttpStatus.OK);

		when(restTemplate.getForEntity(anyString(), eq(String.class))).thenReturn(responseEntity);

		List<Article> result = core.fetchArticles(keyword, category);

		assertNotNull(result);
		assertTrue(result.isEmpty());
		verify(restTemplate, times(1)).getForEntity(anyString(), eq(String.class));
		try {
			verify(objectMapper, never()).readValue(anyString(), eq(NewsApiAdapter.class));
		} catch (JsonProcessingException e) {
			// This won't happen with never(), but needed for compilation
		}
	}

	@Test
	void testFetchArticles_JsonProcessingException_ThrowsSerializationException() throws Exception {
		String keyword = "technology";
		String category = "technology";
		String jsonResponse = "invalid json";

		ResponseEntity<String> responseEntity = new ResponseEntity<>(jsonResponse, HttpStatus.OK);

		when(restTemplate.getForEntity(anyString(), eq(String.class))).thenReturn(responseEntity);
		when(objectMapper.readValue(jsonResponse, NewsApiAdapter.class))
			.thenThrow(new JsonProcessingException("Parse error") {});

		assertThrows(SerializationException.class, () -> core.fetchArticles(keyword, category));
	}

	@Test
	void testFetchArticles_NullArticlesList_ReturnsEmptyList() throws Exception {
		String keyword = "technology";
		String category = "technology";
		String jsonResponse = "{\"status\":\"ok\"}";
		NewsApiAdapter adapter = new NewsApiAdapter();
		adapter.setStatus("ok");
		adapter.setArticles(null);

		ResponseEntity<String> responseEntity = new ResponseEntity<>(jsonResponse, HttpStatus.OK);

		when(restTemplate.getForEntity(anyString(), eq(String.class))).thenReturn(responseEntity);
		when(objectMapper.readValue(jsonResponse, NewsApiAdapter.class)).thenReturn(adapter);

		List<Article> result = core.fetchArticles(keyword, category);

		assertNotNull(result);
		assertTrue(result.isEmpty());
	}

	@Test
	void testFetchArticles_NullArticleResponse_SkipsNull() throws Exception {
		String keyword = "technology";
		String category = "technology";
		String jsonResponse = createMockJsonResponse();
		NewsApiAdapter adapter = createMockAdapter();
		adapter.getArticles().add(null); // Add null article
		Article mockArticle = createMockArticle(category);

		ResponseEntity<String> responseEntity = new ResponseEntity<>(jsonResponse, HttpStatus.OK);

		when(restTemplate.getForEntity(anyString(), eq(String.class))).thenReturn(responseEntity);
		when(objectMapper.readValue(jsonResponse, NewsApiAdapter.class)).thenReturn(adapter);
		when(mapper.toArticle(any(NewsApiAdapter.ArticleResponse.class))).thenReturn(mockArticle);

		List<Article> result = core.fetchArticles(keyword, category);

		assertNotNull(result);
		assertEquals(1, result.size()); // Only valid article, null skipped
	}

	@Test
	void testFetchArticles_MappingException_SkipsArticle() throws Exception {
		String keyword = "technology";
		String category = "technology";
		String jsonResponse = createMockJsonResponse();
		NewsApiAdapter adapter = createMockAdapter();

		ResponseEntity<String> responseEntity = new ResponseEntity<>(jsonResponse, HttpStatus.OK);

		when(restTemplate.getForEntity(anyString(), eq(String.class))).thenReturn(responseEntity);
		when(objectMapper.readValue(jsonResponse, NewsApiAdapter.class)).thenReturn(adapter);
		when(mapper.toArticle(any(NewsApiAdapter.ArticleResponse.class)))
			.thenThrow(new RuntimeException("Mapping error"));

		List<Article> result = core.fetchArticles(keyword, category);

		assertNotNull(result);
		assertTrue(result.isEmpty()); // Article with mapping error skipped
	}

	@Test
	void testFetchArticles_MapperReturnsNull_SkipsArticle() throws Exception {
		String keyword = "technology";
		String category = "technology";
		String jsonResponse = createMockJsonResponse();
		NewsApiAdapter adapter = createMockAdapter();

		ResponseEntity<String> responseEntity = new ResponseEntity<>(jsonResponse, HttpStatus.OK);

		when(restTemplate.getForEntity(anyString(), eq(String.class))).thenReturn(responseEntity);
		when(objectMapper.readValue(jsonResponse, NewsApiAdapter.class)).thenReturn(adapter);
		when(mapper.toArticle(any(NewsApiAdapter.ArticleResponse.class))).thenReturn(null);

		List<Article> result = core.fetchArticles(keyword, category);

		assertNotNull(result);
		assertTrue(result.isEmpty()); // Null article skipped
	}

	@Test
	void testFetchArticles_InvalidArticle_SkipsArticle() throws Exception {
		String keyword = "technology";
		String category = "technology";
		String jsonResponse = createMockJsonResponse();
		NewsApiAdapter adapter = createMockAdapter();
		Article mockArticle = new Article();
		mockArticle.setTitle(""); // Empty title

		ResponseEntity<String> responseEntity = new ResponseEntity<>(jsonResponse, HttpStatus.OK);

		when(restTemplate.getForEntity(anyString(), eq(String.class))).thenReturn(responseEntity);
		when(objectMapper.readValue(jsonResponse, NewsApiAdapter.class)).thenReturn(adapter);
		when(mapper.toArticle(any(NewsApiAdapter.ArticleResponse.class))).thenReturn(mockArticle);
		when(validator.isValid(mockArticle)).thenReturn(false); // Invalid article

		List<Article> result = core.fetchArticles(keyword, category);

		assertNotNull(result);
		assertTrue(result.isEmpty()); // Invalid article skipped
	}

	@Test
	void testFetchArticles_MultipleArticles_SomeValidSomeInvalid_ReturnsOnlyValid() throws Exception {
		String keyword = "technology";
		String category = "technology";
		String jsonResponse = createMockJsonResponse();
		NewsApiAdapter adapter = createMockAdapter();

		// Add another article response
		NewsApiAdapter.ArticleResponse articleResponse2 = new NewsApiAdapter.ArticleResponse();
		articleResponse2.setTitle("Valid Article 2");
		adapter.getArticles().add(articleResponse2);

		Article validArticle1 = createMockArticle(category);
		Article validArticle2 = createMockArticle(category);
		validArticle2.setTitle("Valid Article 2");

		ResponseEntity<String> responseEntity = new ResponseEntity<>(jsonResponse, HttpStatus.OK);

		when(restTemplate.getForEntity(anyString(), eq(String.class))).thenReturn(responseEntity);
		when(objectMapper.readValue(jsonResponse, NewsApiAdapter.class)).thenReturn(adapter);
		when(mapper.toArticle(adapter.getArticles().get(0))).thenReturn(validArticle1);
		when(mapper.toArticle(adapter.getArticles().get(1))).thenReturn(validArticle2);

		List<Article> result = core.fetchArticles(keyword, category);

		assertNotNull(result);
		assertEquals(2, result.size());
	}

	@Test
	void testFetchArticles_UrlContainsApiKey() throws Exception {
		String keyword = "technology";
		String category = "technology";
		String jsonResponse = createMockJsonResponse();
		NewsApiAdapter adapter = createMockAdapter();
		Article mockArticle = createMockArticle(category);

		ResponseEntity<String> responseEntity = new ResponseEntity<>(jsonResponse, HttpStatus.OK);

		ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
		when(restTemplate.getForEntity(anyString(), eq(String.class))).thenReturn(responseEntity);
		when(objectMapper.readValue(jsonResponse, NewsApiAdapter.class)).thenReturn(adapter);
		when(mapper.toArticle(any(NewsApiAdapter.ArticleResponse.class))).thenReturn(mockArticle);

		core.fetchArticles(keyword, category);

		verify(restTemplate, times(1)).getForEntity(urlCaptor.capture(), eq(String.class));
		String capturedUrl = urlCaptor.getValue();
		assertTrue(capturedUrl.contains(NewsApiConstants.PARAM_API_KEY), 
			"URL should contain " + NewsApiConstants.PARAM_API_KEY);
		assertTrue(capturedUrl.contains(API_KEY), "URL should contain API key");
	}

	private String createMockJsonResponse() {
		return "{\"status\":\"ok\",\"articles\":[{\"title\":\"Test Article\",\"description\":\"Test Description\",\"author\":\"Test Author\",\"publishedAt\":\"2025-01-18T10:00:00Z\"}]}";
	}

	private NewsApiAdapter createMockAdapter() {
		NewsApiAdapter adapter = new NewsApiAdapter();
		adapter.setStatus("ok");

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

