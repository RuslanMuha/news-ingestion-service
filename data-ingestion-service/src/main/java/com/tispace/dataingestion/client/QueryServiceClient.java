package com.tispace.dataingestion.client;

import com.tispace.common.dto.ArticleDTO;
import com.tispace.common.dto.SummaryDTO;
import com.tispace.common.exception.ExternalApiException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class QueryServiceClient {
	
	@Value("${services.query-service.url:http://query-service:8082}")
	private String queryServiceUrl;
	
	private final RestTemplate restTemplate;
	
	private static final String SUMMARY_ENDPOINT = "/internal/summary";
	
	@CircuitBreaker(name = "queryService", fallbackMethod = "getArticleSummaryFallback")
	@Retry(name = "queryService")
	@RateLimiter(name = "queryService", fallbackMethod = "getArticleSummaryFallback")
	public SummaryDTO getArticleSummary(Long articleId, ArticleDTO article) {
		String url = String.format("%s%s/%d", queryServiceUrl, SUMMARY_ENDPOINT, articleId);
		
		HttpEntity<ArticleDTO> request = new HttpEntity<>(article);
		ResponseEntity<SummaryDTO> response = restTemplate.exchange(
			url,
			HttpMethod.POST,
			request,
			SummaryDTO.class
		);
		
		if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
			return response.getBody();
		}
		
		if (response.getBody() == null) {
			log.warn("Query-service returned null response body with status: {}", response.getStatusCode());
			throw new ExternalApiException("Query-service returned empty response");
		}
		
		log.warn("Unexpected response from query-service: {}", response.getStatusCode());
		throw new ExternalApiException(String.format("Failed to get summary from query-service: status %s", response.getStatusCode()));
	}
	
	/**
	 * Fallback method when circuit breaker is open, rate limit exceeded, or service is unavailable.
	 * This method is invoked by Resilience4j annotations, not directly by code.
	 */
	@SuppressWarnings("unused")
	private SummaryDTO getArticleSummaryFallback(Long articleId, ArticleDTO article, Exception e) {
		if (e instanceof RequestNotPermitted) {
			log.warn("Rate limit exceeded for query-service. Article id: {}", articleId);
			throw new ExternalApiException("Rate limit exceeded. Please try again later.", e);
		}
		log.error("Query-service circuit breaker is open or service unavailable. Using fallback for article id: {}", articleId, e);
		throw new ExternalApiException("Query-service is currently unavailable. Please try again later.", e);
	}
}

