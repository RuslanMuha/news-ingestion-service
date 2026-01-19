package com.tispace.queryservice.service;

import com.tispace.common.dto.ArticleDTO;
import com.tispace.common.dto.SummaryDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArticleSummaryServiceTest {
	
	@Mock
	private CacheService cacheService;
	
	@Mock
	private ChatGptService chatGptService;
	
	@InjectMocks
	private ArticleSummaryService articleSummaryService;
	
	private ArticleDTO mockArticleDTO;
	private static final Long ARTICLE_ID = 1L;
	
	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(articleSummaryService, "cacheTtlHours", 24);
		
		mockArticleDTO = ArticleDTO.builder()
			.id(ARTICLE_ID)
			.title("Test Article")
			.description("Test Description")
			.author("Test Author")
			.publishedAt(LocalDateTime.now())
			.category("technology")
			.build();
	}
	
	@Test
	void testGetSummary_FromCache_ReturnsCachedSummary() {
		SummaryDTO cachedSummary = SummaryDTO.builder()
			.articleId(ARTICLE_ID)
			.summary("Cached summary")
			.cached(false)
			.build();
		
		when(cacheService.get(anyString(), eq(SummaryDTO.class))).thenReturn(cachedSummary);
		
		SummaryDTO result = articleSummaryService.getSummary(ARTICLE_ID, mockArticleDTO);
		
		assertNotNull(result);
		assertTrue(result.getCached());
		assertEquals("Cached summary", result.getSummary());
		verify(cacheService, times(1)).get(anyString(), eq(SummaryDTO.class));
		verify(chatGptService, never()).generateSummary(any(ArticleDTO.class));
	}
	
	@Test
	void testGetSummary_NotInCache_GeneratesNewSummary() {
		String generatedSummary = "Generated summary by ChatGPT";
		
		when(cacheService.get(anyString(), eq(SummaryDTO.class))).thenReturn(null);
		when(chatGptService.generateSummary(mockArticleDTO)).thenReturn(generatedSummary);
		
		SummaryDTO result = articleSummaryService.getSummary(ARTICLE_ID, mockArticleDTO);
		
		assertNotNull(result);
		assertFalse(result.getCached());
		assertEquals(ARTICLE_ID, result.getArticleId());
		assertEquals(generatedSummary, result.getSummary());
		
		verify(cacheService, times(1)).get(anyString(), eq(SummaryDTO.class));
		verify(chatGptService, times(1)).generateSummary(mockArticleDTO);
		verify(cacheService, times(1)).put(anyString(), any(SummaryDTO.class), eq(86400L)); // 24 hours * 60 * 60 = 86400 seconds
	}
}

