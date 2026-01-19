package com.tispace.queryservice.service;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import com.tispace.common.dto.ArticleDTO;
import com.tispace.common.exception.ExternalApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatGptServiceTest {
	
	@Mock
	private OpenAiService openAiService;
	
	@Mock
	private ObjectProvider<OpenAiService> openAiServiceProvider;
	
	private ChatGptService chatGptService;
	
	private ArticleDTO mockArticleDTO;
	
	@BeforeEach
	void setUp() {
		when(openAiServiceProvider.getIfAvailable()).thenReturn(openAiService);
		chatGptService = new ChatGptService(openAiServiceProvider);
		ReflectionTestUtils.setField(chatGptService, "model", "gpt-3.5-turbo");
		
		mockArticleDTO = ArticleDTO.builder()
			.id(1L)
			.title("Test Article Title")
			.description("Test Article Description")
			.author("Test Author")
			.publishedAt(LocalDateTime.now())
			.category("technology")
			.build();
	}
	
	@Test
	@SuppressWarnings("unchecked")
	void testGenerateSummary_OpenAiServiceNull_ReturnsMockSummary() {
		ObjectProvider<OpenAiService> nullProvider = mock(ObjectProvider.class);
		when(nullProvider.getIfAvailable()).thenReturn(null);
		ChatGptService service = new ChatGptService(nullProvider);
		ReflectionTestUtils.setField(service, "model", "gpt-3.5-turbo");
		
		String result = service.generateSummary(mockArticleDTO);
		
		assertNotNull(result);
		assertTrue(result.contains("mock summary"), "Result should contain 'mock summary'");
		assertTrue(result.contains(mockArticleDTO.getTitle()), "Result should contain article title");
		assertTrue(result.contains("OpenAI API key is not configured"), 
			"Result should indicate that API key is not configured");
	}
	
	@Test
	void testGenerateSummary_Success_ReturnsSummary() {
		String expectedSummary = "This is a generated summary";
		ChatCompletionResult mockResult = createMockChatCompletionResult(expectedSummary);
		
		when(openAiService.createChatCompletion(any(ChatCompletionRequest.class))).thenReturn(mockResult);
		
		String result = chatGptService.generateSummary(mockArticleDTO);
		
		assertNotNull(result);
		assertEquals(expectedSummary, result);
		verify(openAiService, times(1)).createChatCompletion(any(ChatCompletionRequest.class));
	}
	
	@Test
	void testGenerateSummary_Exception_ThrowsExternalApiException() {
		when(openAiService.createChatCompletion(any(ChatCompletionRequest.class)))
			.thenThrow(new RuntimeException("OpenAI API error"));
		
		assertThrows(ExternalApiException.class, () -> chatGptService.generateSummary(mockArticleDTO));
		
		verify(openAiService, times(1)).createChatCompletion(any(ChatCompletionRequest.class));
	}
	
	@Test
	void testGenerateSummary_WithDescription_IncludesDescriptionInPrompt() {
		String expectedSummary = "Summary with description";
		ChatCompletionResult mockResult = createMockChatCompletionResult(expectedSummary);
		
		when(openAiService.createChatCompletion(any(ChatCompletionRequest.class))).thenReturn(mockResult);
		
		chatGptService.generateSummary(mockArticleDTO);
		
		verify(openAiService, times(1)).createChatCompletion(any(ChatCompletionRequest.class));
	}
	
	@Test
	void testGenerateSummary_WithoutDescription_StillGeneratesSummary() {
		ArticleDTO articleWithoutDescription = ArticleDTO.builder()
			.id(2L)
			.title("Test Article")
			.description(null)
			.author("Test Author")
			.publishedAt(LocalDateTime.now())
			.category("technology")
			.build();
		
		String expectedSummary = "Summary without description";
		ChatCompletionResult mockResult = createMockChatCompletionResult(expectedSummary);
		
		when(openAiService.createChatCompletion(any(ChatCompletionRequest.class))).thenReturn(mockResult);
		
		String result = chatGptService.generateSummary(articleWithoutDescription);
		
		assertNotNull(result);
		assertEquals(expectedSummary, result);
		verify(openAiService, times(1)).createChatCompletion(any(ChatCompletionRequest.class));
	}
	
	private ChatCompletionResult createMockChatCompletionResult(String content) {
		ChatCompletionResult result = mock(ChatCompletionResult.class);
		com.theokanning.openai.completion.chat.ChatCompletionChoice choice = 
			mock(com.theokanning.openai.completion.chat.ChatCompletionChoice.class);
		ChatMessage message = new ChatMessage("assistant", content);
		
		List<com.theokanning.openai.completion.chat.ChatCompletionChoice> choices = new ArrayList<>();
		choices.add(choice);
		
		when(result.getChoices()).thenReturn(choices);
		when(choice.getMessage()).thenReturn(message);
		
		return result;
	}
}


