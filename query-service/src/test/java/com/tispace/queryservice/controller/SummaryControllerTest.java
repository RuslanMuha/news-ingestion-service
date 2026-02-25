package com.tispace.queryservice.controller;

import com.tispace.common.contract.ArticleDTO;
import com.tispace.common.contract.SummaryDTO;
import com.tispace.queryservice.service.ArticleSummaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class SummaryControllerTest {
	
	private MockMvc mockMvc;
	
	@Mock
	private ArticleSummaryService articleSummaryService;
	
	@InjectMocks
	private SummaryController summaryController;
	
	private ArticleDTO mockArticleDTO;
	private ObjectMapper objectMapper;
	private static final UUID ARTICLE_ID = UUID.fromString("01234567-89ab-7def-0123-456789abcdef");
	
	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(summaryController)
			.setControllerAdvice(new com.tispace.queryservice.web.exception.GlobalExceptionHandler())
			.build();
		
		objectMapper = new ObjectMapper();
		objectMapper.registerModule(new JavaTimeModule());
		
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
	void testGetArticleSummary_Cached() throws Exception {
		SummaryDTO cachedSummary = SummaryDTO.builder()
			.articleId(ARTICLE_ID)
			.summary("Cached summary")
			.cached(true)
			.build();
		
		when(articleSummaryService.getSummary(eq(ARTICLE_ID), any(ArticleDTO.class))).thenReturn(cachedSummary);
		
		mockMvc.perform(post("/internal/summary/" + ARTICLE_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(mockArticleDTO)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.summary").value("Cached summary"))
			.andExpect(jsonPath("$.cached").value(true));
	}
	
	@Test
	void testGetArticleSummary_NotCached_GeneratesSummary() throws Exception {
		SummaryDTO generatedSummary = SummaryDTO.builder()
			.articleId(ARTICLE_ID)
			.summary("Generated summary")
			.cached(false)
			.build();
		
		when(articleSummaryService.getSummary(eq(ARTICLE_ID), any(ArticleDTO.class))).thenReturn(generatedSummary);
		
		mockMvc.perform(post("/internal/summary/" + ARTICLE_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(mockArticleDTO)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.summary").value("Generated summary"))
			.andExpect(jsonPath("$.cached").value(false));
	}
	
	@Test
	void testGetArticleSummary_InvalidJson_ReturnsBadRequest() throws Exception {
		// Malformed JSON is mapped to 400 BAD_REQUEST by GlobalExceptionHandler
		String invalidJson = "{\"id\":\"" + ARTICLE_ID + "\",\"title\":\"Test\"";
		mockMvc.perform(post("/internal/summary/" + ARTICLE_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content(invalidJson))
			.andExpect(status().isBadRequest());
	}
	
	@Test
	void testGetArticleSummary_MissingContentType_ReturnsError() throws Exception {
		// Missing Content-Type can cause 400 (bad request) or 5xx (e.g. deserialization error)
		mockMvc.perform(post("/internal/summary/" + ARTICLE_ID)
				.content(objectMapper.writeValueAsString(mockArticleDTO)))
			.andExpect(result -> {
				int status = result.getResponse().getStatus();
				assertTrue(status == 400 || status >= 500, "Expected 400 or 5xx, got " + status);
			});
	}
	
	@Test
	void testGetArticleSummary_ServiceThrowsException_ReturnsError() throws Exception {
		when(articleSummaryService.getSummary(eq(ARTICLE_ID), any(ArticleDTO.class)))
			.thenThrow(new RuntimeException("Service error"));
		
		mockMvc.perform(post("/internal/summary/" + ARTICLE_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(mockArticleDTO)))
			.andExpect(status().isInternalServerError());
	}
	
	@Test
	void testGetArticleSummary_DifferentArticleId_ReturnsOk() throws Exception {
		// Controller doesn't validate that article ID in path matches ID in body
		// It just uses the path ID and passes the article body as-is
		UUID differentId = UUID.fromString("99999999-9999-7999-9999-999999999999");
		SummaryDTO summary = SummaryDTO.builder()
			.articleId(ARTICLE_ID)
			.summary("Summary")
			.cached(false)
			.build();
		
		ArticleDTO articleWithDifferentId = ArticleDTO.builder()
			.id(differentId)
			.title("Test Article")
			.build();
		
		when(articleSummaryService.getSummary(eq(ARTICLE_ID), any(ArticleDTO.class))).thenReturn(summary);
		
		mockMvc.perform(post("/internal/summary/" + ARTICLE_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(articleWithDifferentId)))
			.andExpect(status().isOk());
	}
	
	@Test
	void testGetArticleSummary_NullArticleDTO_ReturnsBadRequest() throws Exception {
		// JSON "null" or missing body is handled as malformed/invalid request -> 400
		mockMvc.perform(post("/internal/summary/" + ARTICLE_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content("null"))
			.andExpect(status().isBadRequest());
	}
	
	@Test
	void testGetArticleSummary_EmptyBody_ReturnsBadRequest() throws Exception {
		// Empty body with @NotNull validation fails
		mockMvc.perform(post("/internal/summary/" + ARTICLE_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
			.andExpect(status().isBadRequest());
	}
}

