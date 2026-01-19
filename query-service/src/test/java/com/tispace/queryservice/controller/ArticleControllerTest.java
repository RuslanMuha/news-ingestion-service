package com.tispace.queryservice.controller;

import com.tispace.common.dto.ArticleDTO;
import com.tispace.common.dto.SummaryDTO;
import com.tispace.queryservice.service.ArticleQueryService;
import com.tispace.queryservice.service.ArticleSummaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.SortHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ArticleControllerTest {
	
	private MockMvc mockMvc;
	
	@Mock
	private ArticleQueryService articleQueryService;
	
	@Mock
	private ArticleSummaryService articleSummaryService;
	
	@InjectMocks
	private ArticleController articleController;
	
	private ArticleDTO mockArticleDTO;
	
	@BeforeEach
	void setUp() {
		// Set up MockMvc with exception handler and Pageable support
		PageableHandlerMethodArgumentResolver pageableResolver = new PageableHandlerMethodArgumentResolver();
		pageableResolver.setOneIndexedParameters(false);
		
		mockMvc = MockMvcBuilders.standaloneSetup(articleController)
			.setControllerAdvice(new com.tispace.common.exception.GlobalExceptionHandler())
			.setCustomArgumentResolvers(
				pageableResolver,
				new SortHandlerMethodArgumentResolver()
			)
			.build();
		
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
	void testGetArticles_Success() throws Exception {
		List<ArticleDTO> articles = new ArrayList<>();
		articles.add(mockArticleDTO);
		Page<ArticleDTO> page = new PageImpl<>(articles, PageRequest.of(0, 20), 1);
		
		when(articleQueryService.getArticlesDTO(any(org.springframework.data.domain.Pageable.class), any())).thenReturn(page);
		
		mockMvc.perform(get("/articles")
				.param("page", "0")
				.param("size", "20")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content").isArray())
			.andExpect(jsonPath("$.content[0].title").value("Test Article"));
	}
	
	@Test
	void testGetArticleById_Success() throws Exception {
		when(articleQueryService.getArticleDTOById(1L)).thenReturn(mockArticleDTO);
		
		mockMvc.perform(get("/articles/1")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(1))
			.andExpect(jsonPath("$.title").value("Test Article"));
	}
	
	@Test
	void testGetArticleById_NotFound() throws Exception {
		when(articleQueryService.getArticleDTOById(1L))
			.thenThrow(new com.tispace.common.exception.NotFoundException("Article", 1L));
		
		mockMvc.perform(get("/articles/1")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isNotFound());
	}
	
	@Test
	void testGetArticleSummary_Cached() throws Exception {
		SummaryDTO cachedSummary = SummaryDTO.builder()
			.articleId(1L)
			.summary("Cached summary")
			.cached(true)
			.build();
		
		when(articleSummaryService.getSummary(1L)).thenReturn(cachedSummary);
		
		mockMvc.perform(get("/articles/1/summary")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.summary").value("Cached summary"))
			.andExpect(jsonPath("$.cached").value(true));
	}
	
	@Test
	void testGetArticleSummary_NotCached_GeneratesSummary() throws Exception {
		SummaryDTO generatedSummary = SummaryDTO.builder()
			.articleId(1L)
			.summary("Generated summary")
			.cached(false)
			.build();
		
		when(articleSummaryService.getSummary(1L)).thenReturn(generatedSummary);
		
		mockMvc.perform(get("/articles/1/summary")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.summary").value("Generated summary"))
			.andExpect(jsonPath("$.cached").value(false));
	}
}

