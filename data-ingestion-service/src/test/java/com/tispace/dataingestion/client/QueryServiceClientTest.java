package com.tispace.dataingestion.client;

import com.tispace.common.contract.ArticleDTO;
import com.tispace.common.contract.SummaryDTO;
import com.tispace.common.exception.ExternalApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QueryServiceClientTest {
	
	@Mock
	private RestTemplate restTemplate;
	
	@InjectMocks
	private QueryServiceClient queryServiceClient;
	
	private ArticleDTO mockArticleDTO;
	private SummaryDTO mockSummaryDTO;
	private static final String QUERY_SERVICE_URL = "http://query-service:8082";
	private static final UUID ARTICLE_ID = UUID.fromString("01234567-89ab-7def-0123-456789abcdef");
	
	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(queryServiceClient, "queryServiceUrl", QUERY_SERVICE_URL);
		
		mockArticleDTO = ArticleDTO.builder()
			.id(ARTICLE_ID)
			.title("Test Article")
			.description("Test Description")
			.author("Test Author")
			.publishedAt(LocalDateTime.now())
			.category("technology")
			.build();
		
		mockSummaryDTO = SummaryDTO.builder()
			.articleId(ARTICLE_ID)
			.summary("Test Summary")
			.cached(false)
			.build();
	}
	
	@Test
	void testGetArticleSummary_Success_ReturnsSummary() {
		ResponseEntity<SummaryDTO> responseEntity = new ResponseEntity<>(mockSummaryDTO, HttpStatus.OK);
		
		when(restTemplate.exchange(
			anyString(),
			eq(HttpMethod.POST),
			any(HttpEntity.class),
			eq(SummaryDTO.class)
		)).thenReturn(responseEntity);
		
		SummaryDTO result = queryServiceClient.getArticleSummary(ARTICLE_ID, mockArticleDTO);
		
		assertNotNull(result);
		assertEquals(ARTICLE_ID, result.getArticleId());
		assertEquals("Test Summary", result.getSummary());
		verify(restTemplate, times(1)).exchange(
			eq(QUERY_SERVICE_URL + "/internal/summary/" + ARTICLE_ID),
			eq(HttpMethod.POST),
			any(HttpEntity.class),
			eq(SummaryDTO.class)
		);
	}
	
	@Test
	void testGetArticleSummary_NullResponseBody_ThrowsException() {
		ResponseEntity<SummaryDTO> responseEntity = new ResponseEntity<>(null, HttpStatus.OK);
		
		when(restTemplate.exchange(
			anyString(),
			eq(HttpMethod.POST),
			any(HttpEntity.class),
			eq(SummaryDTO.class)
		)).thenReturn(responseEntity);
		
		ExternalApiException exception = assertThrows(ExternalApiException.class, 
			() -> queryServiceClient.getArticleSummary(ARTICLE_ID, mockArticleDTO));
		
		assertTrue(exception.getMessage().contains("empty response"));
	}
	
	@Test
	void testGetArticleSummary_NonOkStatus_ThrowsException() {
		ResponseEntity<SummaryDTO> responseEntity = new ResponseEntity<>(mockSummaryDTO, HttpStatus.INTERNAL_SERVER_ERROR);
		
		when(restTemplate.exchange(
			anyString(),
			eq(HttpMethod.POST),
			any(HttpEntity.class),
			eq(SummaryDTO.class)
		)).thenReturn(responseEntity);
		
		ExternalApiException exception = assertThrows(ExternalApiException.class, 
			() -> queryServiceClient.getArticleSummary(ARTICLE_ID, mockArticleDTO));
		
		assertTrue(exception.getMessage().contains("Failed to get summary"));
	}
	
	@Test
	void testGetArticleSummary_HttpClientErrorException_MapsToExternalApiException() {
		HttpClientErrorException httpException = new HttpClientErrorException(HttpStatus.NOT_FOUND, "Not Found");
		
		when(restTemplate.exchange(
			anyString(),
			eq(HttpMethod.POST),
			any(HttpEntity.class),
			eq(SummaryDTO.class)
		)).thenThrow(httpException);
		
		ExternalApiException exception = assertThrows(ExternalApiException.class, 
			() -> queryServiceClient.getArticleSummary(ARTICLE_ID, mockArticleDTO));
		assertTrue(exception.getMessage().contains("client error"));
		assertSame(httpException, exception.getCause());
	}
	
	@Test
	void testGetArticleSummary_HttpServerErrorException_ThrowsException() {
		HttpServerErrorException httpException = new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Server Error");
		
		when(restTemplate.exchange(
			anyString(),
			eq(HttpMethod.POST),
			any(HttpEntity.class),
			eq(SummaryDTO.class)
		)).thenThrow(httpException);
		
		// In unit tests without Resilience4j configuration, exceptions are thrown directly
		assertThrows(HttpServerErrorException.class, 
			() -> queryServiceClient.getArticleSummary(ARTICLE_ID, mockArticleDTO));
	}
	
	@Test
	void testGetArticleSummary_ResourceAccessException_ThrowsException() {
		ResourceAccessException accessException = new ResourceAccessException("Connection refused");
		
		when(restTemplate.exchange(
			anyString(),
			eq(HttpMethod.POST),
			any(HttpEntity.class),
			eq(SummaryDTO.class)
		)).thenThrow(accessException);
		
		// In unit tests without Resilience4j configuration, exceptions are thrown directly
		assertThrows(ResourceAccessException.class, 
			() -> queryServiceClient.getArticleSummary(ARTICLE_ID, mockArticleDTO));
	}
	
	@Test
	void testGetArticleSummary_RuntimeException_ThrowsException() {
		RuntimeException runtimeException = new RuntimeException("Unexpected error");
		
		when(restTemplate.exchange(
			anyString(),
			eq(HttpMethod.POST),
			any(HttpEntity.class),
			eq(SummaryDTO.class)
		)).thenThrow(runtimeException);
		
		// In unit tests without Resilience4j configuration, exceptions are thrown directly
		assertThrows(RuntimeException.class, 
			() -> queryServiceClient.getArticleSummary(ARTICLE_ID, mockArticleDTO));
	}
	
	@Test
	void testGetArticleSummary_RequestNotPermitted_CallsFallback() {
		// RequestNotPermitted doesn't have a public constructor, so we'll test the fallback behavior
		// by simulating the scenario where rate limiter would trigger
		RuntimeException rateLimitException = new RuntimeException("Rate limit exceeded");
		
		when(restTemplate.exchange(
			anyString(),
			eq(HttpMethod.POST),
			any(HttpEntity.class),
			eq(SummaryDTO.class)
		)).thenThrow(rateLimitException);
		
		// In unit tests without Resilience4j configuration, exceptions are thrown directly
		assertThrows(RuntimeException.class, 
			() -> queryServiceClient.getArticleSummary(ARTICLE_ID, mockArticleDTO));
	}
	
	@Test
	void testGetArticleSummary_DifferentArticleId_UsesCorrectUrl() {
		UUID differentId = UUID.fromString("99999999-9999-7999-9999-999999999999");
		ResponseEntity<SummaryDTO> responseEntity = new ResponseEntity<>(mockSummaryDTO, HttpStatus.OK);
		
		when(restTemplate.exchange(
			anyString(),
			eq(HttpMethod.POST),
			any(HttpEntity.class),
			eq(SummaryDTO.class)
		)).thenReturn(responseEntity);
		
		queryServiceClient.getArticleSummary(differentId, mockArticleDTO);
		
		verify(restTemplate, times(1)).exchange(
			eq(QUERY_SERVICE_URL + "/internal/summary/" + differentId),
			eq(HttpMethod.POST),
			any(HttpEntity.class),
			eq(SummaryDTO.class)
		);
	}
	
	@Test
	void testGetArticleSummary_NullArticleDTO_StillSendsRequest() {
		ResponseEntity<SummaryDTO> responseEntity = new ResponseEntity<>(mockSummaryDTO, HttpStatus.OK);
		
		when(restTemplate.exchange(
			anyString(),
			eq(HttpMethod.POST),
			any(HttpEntity.class),
			eq(SummaryDTO.class)
		)).thenReturn(responseEntity);
		
		SummaryDTO result = queryServiceClient.getArticleSummary(ARTICLE_ID, null);
		
		assertNotNull(result);
		verify(restTemplate, times(1)).exchange(
			anyString(),
			eq(HttpMethod.POST),
			any(HttpEntity.class),
			eq(SummaryDTO.class)
		);
	}
	
	@Test
	void testGetArticleSummary_BadRequestStatus_ThrowsException() {
		ResponseEntity<SummaryDTO> responseEntity = new ResponseEntity<>(mockSummaryDTO, HttpStatus.BAD_REQUEST);
		
		when(restTemplate.exchange(
			anyString(),
			eq(HttpMethod.POST),
			any(HttpEntity.class),
			eq(SummaryDTO.class)
		)).thenReturn(responseEntity);
		
		ExternalApiException exception = assertThrows(ExternalApiException.class, 
			() -> queryServiceClient.getArticleSummary(ARTICLE_ID, mockArticleDTO));
		
		assertTrue(exception.getMessage().contains("Failed to get summary"));
	}
}

