package com.tispace.queryservice.service;

import com.tispace.common.dto.ArticleDTO;
import com.tispace.queryservice.client.DataIngestionServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArticleQueryServiceTest {
	
	@Mock
	private DataIngestionServiceClient dataIngestionServiceClient;
	
	@InjectMocks
	private ArticleQueryService articleQueryService;
	
	private ArticleDTO mockArticleDTO;
	private List<ArticleDTO> mockArticles;
	
	@BeforeEach
	void setUp() {
		mockArticleDTO = ArticleDTO.builder()
			.id(1L)
			.title("Test Article")
			.description("Test Description")
			.author("Test Author")
			.publishedAt(LocalDateTime.now())
			.category("technology")
			.build();
		
		mockArticles = new ArrayList<>();
		mockArticles.add(mockArticleDTO);
	}
	
	@Test
	void testGetArticlesDTO_WithCategory_CallsClient() {
		Pageable pageable = PageRequest.of(0, 20);
		Page<ArticleDTO> page = new PageImpl<>(mockArticles, pageable, 1);
		
		when(dataIngestionServiceClient.getArticles(any(Pageable.class), anyString()))
			.thenReturn(page);
		
		Page<ArticleDTO> result = articleQueryService.getArticlesDTO(pageable, "technology");
		
		assertNotNull(result);
		assertEquals(1, result.getContent().size());
		verify(dataIngestionServiceClient, times(1)).getArticles(pageable, "technology");
	}
	
	@Test
	void testGetArticlesDTO_WithoutCategory_CallsClient() {
		Pageable pageable = PageRequest.of(0, 20);
		Page<ArticleDTO> page = new PageImpl<>(mockArticles, pageable, 1);
		
		when(dataIngestionServiceClient.getArticles(any(Pageable.class), any()))
			.thenReturn(page);
		
		Page<ArticleDTO> result = articleQueryService.getArticlesDTO(pageable, null);
		
		assertNotNull(result);
		assertEquals(1, result.getContent().size());
		verify(dataIngestionServiceClient, times(1)).getArticles(pageable, null);
	}
	
	@Test
	void testGetArticleDTOById_Exists_ReturnsArticle() {
		when(dataIngestionServiceClient.getArticleById(1L)).thenReturn(mockArticleDTO);
		
		ArticleDTO result = articleQueryService.getArticleDTOById(1L);
		
		assertNotNull(result);
		assertEquals(1L, result.getId());
		assertEquals("Test Article", result.getTitle());
		verify(dataIngestionServiceClient, times(1)).getArticleById(1L);
	}
}
