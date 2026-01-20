package com.tispace.queryservice.service;

import com.tispace.common.dto.ArticleDTO;
import com.tispace.queryservice.constants.ChatGptConstants;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PromptBuilderTest {
	
	private static final UUID ARTICLE_ID = UUID.fromString("01234567-89ab-7def-0123-456789abcdef");
	
	@Test
	void testBuildSummaryPrompt_WithDescription_IncludesDescription() {
		ArticleDTO article = ArticleDTO.builder()
			.id(ARTICLE_ID)
			.title("Test Article Title")
			.description("Test Article Description")
			.author("Test Author")
			.publishedAt(LocalDateTime.now())
			.category("technology")
			.build();
		
		String prompt = PromptBuilder.buildSummaryPrompt(article);
		
		assertNotNull(prompt);
		assertTrue(prompt.contains(ChatGptConstants.PROMPT_PREFIX));
		assertTrue(prompt.contains(ChatGptConstants.PROMPT_TITLE_PREFIX));
		assertTrue(prompt.contains("Test Article Title"));
		assertTrue(prompt.contains(ChatGptConstants.PROMPT_DESCRIPTION_PREFIX));
		assertTrue(prompt.contains("Test Article Description"));
		assertTrue(prompt.contains(ChatGptConstants.PROMPT_SUMMARY_SUFFIX));
	}
	
	@Test
	void testBuildSummaryPrompt_WithoutDescription_ExcludesDescription() {
		ArticleDTO article = ArticleDTO.builder()
			.id(ARTICLE_ID)
			.title("Test Article Title")
			.description(null)
			.author("Test Author")
			.publishedAt(LocalDateTime.now())
			.category("technology")
			.build();
		
		String prompt = PromptBuilder.buildSummaryPrompt(article);
		
		assertNotNull(prompt);
		assertTrue(prompt.contains(ChatGptConstants.PROMPT_PREFIX));
		assertTrue(prompt.contains(ChatGptConstants.PROMPT_TITLE_PREFIX));
		assertTrue(prompt.contains("Test Article Title"));
		assertFalse(prompt.contains(ChatGptConstants.PROMPT_DESCRIPTION_PREFIX));
		assertTrue(prompt.contains(ChatGptConstants.PROMPT_SUMMARY_SUFFIX));
	}
	
	@Test
	void testBuildSummaryPrompt_WithEmptyDescription_ExcludesDescription() {
		ArticleDTO article = ArticleDTO.builder()
			.id(ARTICLE_ID)
			.title("Test Article Title")
			.description("")
			.author("Test Author")
			.publishedAt(LocalDateTime.now())
			.category("technology")
			.build();
		
		String prompt = PromptBuilder.buildSummaryPrompt(article);
		
		assertNotNull(prompt);
		assertTrue(prompt.contains(ChatGptConstants.PROMPT_PREFIX));
		assertTrue(prompt.contains("Test Article Title"));
		assertFalse(prompt.contains(ChatGptConstants.PROMPT_DESCRIPTION_PREFIX));
	}
	
	@Test
	void testBuildSummaryPrompt_ContainsAllRequiredParts() {
		ArticleDTO article = ArticleDTO.builder()
			.id(ARTICLE_ID)
			.title("Test Title")
			.description("Test Description")
			.author("Test Author")
			.publishedAt(LocalDateTime.now())
			.category("technology")
			.build();
		
		String prompt = PromptBuilder.buildSummaryPrompt(article);
		
		assertNotNull(prompt);
		assertTrue(prompt.startsWith(ChatGptConstants.PROMPT_PREFIX));
		assertTrue(prompt.contains("Title: Test Title"));
		assertTrue(prompt.contains("Description: Test Description"));
		assertTrue(prompt.endsWith(ChatGptConstants.PROMPT_SUMMARY_SUFFIX));
	}
}



