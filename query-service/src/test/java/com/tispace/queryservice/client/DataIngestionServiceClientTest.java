package com.tispace.queryservice.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tispace.common.dto.ArticleDTO;
import com.tispace.common.exception.ExternalApiException;
import com.tispace.common.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataIngestionServiceClientTest {
	
	@Mock
	private RestTemplate restTemplate;
	
	@Mock
	private ObjectMapper objectMapper;
	
	@InjectMocks
	private DataIngestionServiceClient dataIngestionServiceClient;
	
	private static final String BASE_URL = "http://test-service:8081";
	private ArticleDTO mockArticleDTO;
	
	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(dataIngestionServiceClient, "ingestionServiceUrl", BASE_URL);
		
		mockArticleDTO = ArticleDTO.builder()
			.id(1L)
			.title("Test Article")
			.description("Test Description")
			.author("Test Author")
			.publishedAt(LocalDateTime.now())
			.category("technology")
			.build();
	}
	
	@Test
	@SuppressWarnings("unchecked")
	void testGetArticles_Success_ReturnsPage() throws Exception {
		Pageable pageable = PageRequest.of(0, 20);
		List<ArticleDTO> articles = new ArrayList<>();
		articles.add(mockArticleDTO);
		
		String jsonResponse = "{\"content\":[{\"id\":1,\"title\":\"Test Article\"}],\"totalElements\":1,\"number\":0,\"size\":20}";
		ResponseEntity<String> responseEntity = new ResponseEntity<>(jsonResponse, HttpStatus.OK);
		
		Map<String, Object> pageMap = new HashMap<>();
		List<Map<String, Object>> contentList = new ArrayList<>();
		Map<String, Object> articleMap = new HashMap<>();
		articleMap.put("id", 1L);
		articleMap.put("title", "Test Article");
		contentList.add(articleMap);
		pageMap.put("content", contentList);
		pageMap.put("totalElements", 1);
		pageMap.put("number", 0);
		pageMap.put("size", 20);
		
		when(restTemplate.exchange(
			anyString(),
			any(),
			isNull(),
			eq(String.class)
		)).thenReturn(responseEntity);
		
		when(objectMapper.readValue(eq(jsonResponse), any(TypeReference.class))).thenReturn(pageMap);
		when(objectMapper.convertValue(eq(contentList), any(TypeReference.class))).thenReturn(articles);
		
		Page<ArticleDTO> result = dataIngestionServiceClient.getArticles(pageable, null);
		
		assertNotNull(result);
		assertEquals(1, result.getContent().size());
		assertEquals("Test Article", result.getContent().get(0).getTitle());
		verify(restTemplate, times(1)).exchange(
			anyString(),
			any(),
			isNull(),
			eq(String.class)
		);
	}
	
	@Test
	@SuppressWarnings("unchecked")
	void testGetArticles_WithCategory_CallsClientWithCategory() throws Exception {
		Pageable pageable = PageRequest.of(0, 20);
		List<ArticleDTO> articles = new ArrayList<>();
		articles.add(mockArticleDTO);
		
		String jsonResponse = "{\"content\":[{\"id\":1,\"title\":\"Test Article\"}],\"totalElements\":1,\"number\":0,\"size\":20}";
		ResponseEntity<String> responseEntity = new ResponseEntity<>(jsonResponse, HttpStatus.OK);
		
		Map<String, Object> pageMap = new HashMap<>();
		List<Map<String, Object>> contentList = new ArrayList<>();
		Map<String, Object> articleMap = new HashMap<>();
		articleMap.put("id", 1L);
		articleMap.put("title", "Test Article");
		contentList.add(articleMap);
		pageMap.put("content", contentList);
		pageMap.put("totalElements", 1);
		pageMap.put("number", 0);
		pageMap.put("size", 20);
		
		when(restTemplate.exchange(
			anyString(),
			any(),
			isNull(),
			eq(String.class)
		)).thenReturn(responseEntity);
		
		when(objectMapper.readValue(eq(jsonResponse), any(TypeReference.class))).thenReturn(pageMap);
		when(objectMapper.convertValue(eq(contentList), any(TypeReference.class))).thenReturn(articles);
		
		Page<ArticleDTO> result = dataIngestionServiceClient.getArticles(pageable, "technology");
		
		assertNotNull(result);
		assertEquals(1, result.getContent().size());
		verify(restTemplate, times(1)).exchange(
			anyString(),
			any(),
			isNull(),
			eq(String.class)
		);
	}
	
	@Test
	void testGetArticles_NotFound_ReturnsEmptyPage() {
		Pageable pageable = PageRequest.of(0, 20);
		
		HttpClientErrorException notFoundException = HttpClientErrorException.create(
			HttpStatus.NOT_FOUND, "Articles not found", null, null, null);
		
		when(restTemplate.exchange(
			anyString(),
			any(),
			isNull(),
			eq(String.class)
		)).thenThrow(notFoundException);
		
		Page<ArticleDTO> result = dataIngestionServiceClient.getArticles(pageable, null);
		
		assertNotNull(result);
		assertTrue(result.getContent().isEmpty());
	}
	
	@Test
	void testGetArticles_Exception_ThrowsExternalApiException() {
		Pageable pageable = PageRequest.of(0, 20);
		
		when(restTemplate.exchange(
			anyString(),
			any(),
			isNull(),
			eq(String.class)
		)).thenThrow(new RestClientException("Connection error"));
		
		assertThrows(ExternalApiException.class, () -> dataIngestionServiceClient.getArticles(pageable, null));
	}
	
	@Test
	void testGetArticleById_Success_ReturnsArticle() {
		Long articleId = 1L;
		
		ResponseEntity<ArticleDTO> responseEntity = new ResponseEntity<>(mockArticleDTO, HttpStatus.OK);
		
		when(restTemplate.getForEntity(anyString(), eq(ArticleDTO.class))).thenReturn(responseEntity);
		
		ArticleDTO result = dataIngestionServiceClient.getArticleById(articleId);
		
		assertNotNull(result);
		assertEquals(articleId, result.getId());
		assertEquals("Test Article", result.getTitle());
		verify(restTemplate, times(1)).getForEntity(anyString(), eq(ArticleDTO.class));
	}
	
	@Test
	void testGetArticleById_NotFound_ThrowsNotFoundException() {
		Long articleId = 999L;
		
		HttpClientErrorException notFoundException = HttpClientErrorException.create(
			HttpStatus.NOT_FOUND, "Article not found", null, null, null);
		
		when(restTemplate.getForEntity(anyString(), eq(ArticleDTO.class)))
			.thenThrow(notFoundException);
		
		assertThrows(NotFoundException.class, () -> dataIngestionServiceClient.getArticleById(articleId));
	}
	
	@Test
	void testGetArticleById_Exception_ThrowsExternalApiException() {
		Long articleId = 1L;
		
		when(restTemplate.getForEntity(anyString(), eq(ArticleDTO.class)))
			.thenThrow(new RestClientException("Connection error"));
		
		assertThrows(ExternalApiException.class, () -> dataIngestionServiceClient.getArticleById(articleId));
	}
}

