package com.tispace.dataingestion.controller;

import com.tispace.common.contract.ArticleDTO;
import com.tispace.common.exception.NotFoundException;
import com.tispace.dataingestion.application.validation.SortStringParser;
import com.tispace.dataingestion.client.QueryServiceClient;
import com.tispace.dataingestion.service.ArticleQueryService;
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
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.SortHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ArticleControllerTest {
	
	private MockMvc mockMvc;
	
	@Mock
	private ArticleQueryService articleQueryService;
	
	@Mock
	private QueryServiceClient queryServiceClient;
	
	@Mock
	private SortStringParser sortStringParser;
	
	@InjectMocks
	private ArticleController articleController;
	
	private ArticleDTO mockArticleDTO;
	private static final UUID ARTICLE_ID = UUID.fromString("01234567-89ab-7def-0123-456789abcdef");
	
	@BeforeEach
	void setUp() {
		PageableHandlerMethodArgumentResolver pageableResolver = new PageableHandlerMethodArgumentResolver();
		pageableResolver.setOneIndexedParameters(false);
		
		// Mock SortStringParser to return default sort (lenient to avoid unnecessary stubbing errors)
		lenient().when(sortStringParser.parse(any(String.class))).thenReturn(Sort.by(Sort.Direction.DESC, "publishedAt"));
		
		mockMvc = MockMvcBuilders.standaloneSetup(articleController)
			.setControllerAdvice(new com.tispace.dataingestion.web.exception.GlobalExceptionHandler())
			.setCustomArgumentResolvers(
				pageableResolver,
				new SortHandlerMethodArgumentResolver()
			)
			.build();
		
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
	void testGetArticles_Success() throws Exception {
		List<ArticleDTO> articles = new ArrayList<>();
		articles.add(mockArticleDTO);
		Page<ArticleDTO> page = new PageImpl<>(articles, PageRequest.of(0, 20), 1);
		
		when(articleQueryService.getArticlesDTO(any(Pageable.class), any())).thenReturn(page);
		
		mockMvc.perform(get("/api/articles")
				.param("page", "0")
				.param("size", "20")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content").isArray())
			.andExpect(jsonPath("$.content[0].title").value("Test Article"));
	}
	
	@Test
	void testGetArticles_WithCategory_FiltersByCategory() throws Exception {
		List<ArticleDTO> articles = new ArrayList<>();
		articles.add(mockArticleDTO);
		Page<ArticleDTO> page = new PageImpl<>(articles, PageRequest.of(0, 20), 1);
		
		when(articleQueryService.getArticlesDTO(any(Pageable.class), eq("technology"))).thenReturn(page);
		
		mockMvc.perform(get("/api/articles")
				.param("page", "0")
				.param("size", "20")
				.param("category", "technology")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content").isArray());
	}
	
	@Test
	void testGetArticleById_Success() throws Exception {
		when(articleQueryService.getArticleDTOById(ARTICLE_ID)).thenReturn(mockArticleDTO);
		
		mockMvc.perform(get("/api/articles/" + ARTICLE_ID)
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(ARTICLE_ID.toString()))
			.andExpect(jsonPath("$.title").value("Test Article"));
	}
	
	@Test
	void testGetArticleById_NotFound() throws Exception {
		when(articleQueryService.getArticleDTOById(ARTICLE_ID))
			.thenThrow(new NotFoundException("Article", ARTICLE_ID));
		
		mockMvc.perform(get("/api/articles/" + ARTICLE_ID)
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isNotFound());
	}
	
	@Test
	void testGetArticles_EmptyPage_ReturnsEmptyPage() throws Exception {
		Page<ArticleDTO> emptyPage = new PageImpl<>(new ArrayList<>(), PageRequest.of(0, 20), 0);
		
		when(articleQueryService.getArticlesDTO(any(Pageable.class), any())).thenReturn(emptyPage);
		
		mockMvc.perform(get("/api/articles")
				.param("page", "0")
				.param("size", "20")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content").isArray())
			.andExpect(jsonPath("$.content").isEmpty());
	}
	
	@Test
	void testGetArticles_WithEmptyCategory_ReturnsAllArticles() throws Exception {
		List<ArticleDTO> articles = new ArrayList<>();
		articles.add(mockArticleDTO);
		Page<ArticleDTO> page = new PageImpl<>(articles, PageRequest.of(0, 20), 1);
		
		when(articleQueryService.getArticlesDTO(any(Pageable.class), eq(""))).thenReturn(page);
		
		mockMvc.perform(get("/api/articles")
				.param("page", "0")
				.param("size", "20")
				.param("category", "")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content").isArray());
	}
	
	@Test
	void testGetArticles_ServiceThrowsException_ReturnsError() throws Exception {
		// No need to stub sortStringParser here since it's already stubbed in setUp
		when(articleQueryService.getArticlesDTO(any(Pageable.class), any()))
			.thenThrow(new RuntimeException("Service error"));
		
		mockMvc.perform(get("/api/articles")
				.param("page", "0")
				.param("size", "20")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isInternalServerError());
	}
	
	@Test
	void testGetArticleById_ServiceThrowsException_ReturnsError() throws Exception {
		when(articleQueryService.getArticleDTOById(ARTICLE_ID))
			.thenThrow(new RuntimeException("Service error"));
		
		mockMvc.perform(get("/api/articles/" + ARTICLE_ID)
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isInternalServerError());
	}
	
	@Test
	void testGetArticles_InvalidPageNumber_ReturnsBadRequest() throws Exception {
		// Spring validation with @Min annotation returns 400 Bad Request for invalid parameters
		mockMvc.perform(get("/api/articles")
				.param("page", "-1")
				.param("size", "20")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isBadRequest());
	}
	
	@Test
	void testGetArticles_InvalidSize_ReturnsBadRequest() throws Exception {
		// Spring validation with @Min annotation returns 400 Bad Request for invalid parameters
		mockMvc.perform(get("/api/articles")
				.param("page", "0")
				.param("size", "0")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isBadRequest());
	}
	
	@Test
	void testGetArticles_NoPaginationParams_UsesDefaults() throws Exception {
		List<ArticleDTO> articles = new ArrayList<>();
		articles.add(mockArticleDTO);
		Page<ArticleDTO> page = new PageImpl<>(articles, PageRequest.of(0, 20), 1);
		
		when(articleQueryService.getArticlesDTO(any(Pageable.class), any())).thenReturn(page);
		
		mockMvc.perform(get("/api/articles")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content").isArray());
	}
	
	@Test
	void testGetArticleById_InvalidId_HandlesGracefully() throws Exception {
		UUID invalidId = UUID.fromString("99999999-9999-7999-9999-999999999999");
		when(articleQueryService.getArticleDTOById(invalidId))
			.thenThrow(new NotFoundException("Article", invalidId));
		
		mockMvc.perform(get("/api/articles/" + invalidId)
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isNotFound());
	}

	@Test
	void testGetArticles_RateLimitFallback_ReturnsStructured429() throws Exception {
		var response = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
			articleController,
			"getArticlesRateLimitFallback",
			0,
			20,
			"publishedAt,desc",
			"technology",
			null
		);

		org.springframework.http.ResponseEntity<?> entity = (org.springframework.http.ResponseEntity<?>) response;
		assertEquals(429, entity.getStatusCode().value());
		assertNotNull(entity.getBody());
		assertEquals("RATE_LIMIT_EXCEEDED", ((com.tispace.common.contract.ErrorResponseDTO) entity.getBody()).getErrorCode());
		assertEquals("/api/articles", ((com.tispace.common.contract.ErrorResponseDTO) entity.getBody()).getPath());
	}

	@Test
	void testGetArticleById_RateLimitFallback_ReturnsStructured429() throws Exception {
		var response = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
			articleController,
			"getArticleByIdRateLimitFallback",
			ARTICLE_ID,
			null
		);

		org.springframework.http.ResponseEntity<?> entity = (org.springframework.http.ResponseEntity<?>) response;
		assertEquals(429, entity.getStatusCode().value());
		assertNotNull(entity.getBody());
		assertEquals("RATE_LIMIT_EXCEEDED", ((com.tispace.common.contract.ErrorResponseDTO) entity.getBody()).getErrorCode());
		assertEquals("/api/articles/" + ARTICLE_ID, ((com.tispace.common.contract.ErrorResponseDTO) entity.getBody()).getPath());
	}
}

