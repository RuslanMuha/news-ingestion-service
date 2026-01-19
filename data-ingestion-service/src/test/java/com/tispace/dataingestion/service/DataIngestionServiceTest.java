package com.tispace.dataingestion.service;

import com.tispace.common.entity.Article;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataIngestionServiceTest {
	
	@Mock
	private ExternalApiClient externalApiClient;
	
	@Mock
	private ArticlePersistenceService articlePersistenceService;
	
	@InjectMocks
	private DataIngestionService dataIngestionService;
	
	private List<Article> mockArticles;
	
	@BeforeEach
	void setUp() {
		// Set default values using reflection since @Value doesn't work in unit tests
		ReflectionTestUtils.setField(dataIngestionService, "defaultKeyword", "technology");
		ReflectionTestUtils.setField(dataIngestionService, "defaultCategory", "technology");
		
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
	void testIngestData_Success() {
		when(externalApiClient.fetchArticles(anyString(), anyString())).thenReturn(mockArticles);
		when(articlePersistenceService.saveArticles(anyList())).thenReturn(1);
		
		assertDoesNotThrow(() -> dataIngestionService.ingestData("technology", "technology"));
		
		verify(externalApiClient, times(1)).fetchArticles("technology", "technology");
		verify(articlePersistenceService, times(1)).saveArticles(anyList());
	}
	
	@Test
	void testIngestData_WithNullParams_UsesDefaults() {
		when(externalApiClient.fetchArticles(anyString(), anyString())).thenReturn(mockArticles);
		when(articlePersistenceService.saveArticles(anyList())).thenReturn(1);
		
		assertDoesNotThrow(() -> dataIngestionService.ingestData(null, null));
		
		// Should use default values when null is passed
		verify(externalApiClient, times(1)).fetchArticles("technology", "technology");
		verify(articlePersistenceService, times(1)).saveArticles(anyList());
	}
	
	@Test
	void testIngestData_EmptyArticles_DoesNotSave() {
		when(externalApiClient.fetchArticles(anyString(), anyString())).thenReturn(new ArrayList<>());
		
		assertDoesNotThrow(() -> dataIngestionService.ingestData("technology", "technology"));
		
		verify(externalApiClient, times(1)).fetchArticles("technology", "technology");
		verify(articlePersistenceService, never()).saveArticles(anyList());
	}
}

