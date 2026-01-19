package com.tispace.dataingestion.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tispace.common.entity.Article;
import com.tispace.common.exception.ExternalApiException;
import com.tispace.common.exception.SerializationException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.apache.commons.lang3.StringUtils;
import com.tispace.dataingestion.adapter.NewsApiAdapter;
import com.tispace.dataingestion.constants.NewsApiConstants;
import com.tispace.dataingestion.mapper.NewsApiArticleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsApiClient implements ExternalApiClient {
	
	private final RestTemplate restTemplate;
	private final ObjectMapper objectMapper;
	private final NewsApiArticleMapper newsApiArticleMapper;
	
	@Value("${external-api.news-api.url:https://newsapi.org/v2/everything}")
	private String newsApiUrl;
	
	@Value("${external-api.news-api.api-key:}")
	private String apiKey;
	
	@Override
	@CircuitBreaker(name = "newsApi", fallbackMethod = "fetchArticlesFallback")
	@Retry(name = "newsApi")
	public List<Article> fetchArticles(String keyword, String category) {
		log.info("Fetching articles from NewsAPI with keyword: {}, category: {}", keyword, category);
		
		String url = buildUrl(keyword, category);
		log.debug("NewsAPI URL: {}", maskApiKey(url));
		
		ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
		
		if (!response.getStatusCode().is2xxSuccessful()) {
			throw new ExternalApiException(String.format("NewsAPI returned status: %s", response.getStatusCode()));
		}
		
		String responseBody = response.getBody();
		if (StringUtils.isEmpty(responseBody)) {
			log.warn("NewsAPI returned empty response body");
			return new ArrayList<>();
		}
		
		NewsApiAdapter adapter;
		try {
			adapter = objectMapper.readValue(responseBody, NewsApiAdapter.class);
		} catch (JsonProcessingException e) {
			log.error("Failed to parse NewsAPI response as JSON", e);
			throw new SerializationException("Failed to parse NewsAPI response", e);
		}
		
		if (!NewsApiConstants.STATUS_OK.equalsIgnoreCase(adapter.getStatus())) {
			throw new ExternalApiException(String.format("NewsAPI returned status: %s", adapter.getStatus()));
		}
		
		return mapToArticles(adapter, category);
	}
	
	private String buildUrl(String keyword, String category) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(newsApiUrl)
			.queryParam(NewsApiConstants.PARAM_API_KEY, apiKey)
			.queryParam(NewsApiConstants.PARAM_PAGE_SIZE, NewsApiConstants.DEFAULT_PAGE_SIZE)
			.queryParam(NewsApiConstants.PARAM_SORT_BY, NewsApiConstants.DEFAULT_SORT_BY);
		
		if (StringUtils.isNotEmpty(keyword)) {
			builder.queryParam(NewsApiConstants.PARAM_QUERY, keyword);
		}
		
		// Note: category parameter is not supported by /everything endpoint
		// Category is only used for setting the category field in Article entities
		// If using /top-headlines endpoint, category parameter would be supported
		
		return builder.toUriString();
	}
	
	/**
	 * Masks API key in URL for logging purposes.
	 * Replaces apiKey value with "***" to prevent exposure in logs.
	 */
	private String maskApiKey(String url) {
		if (url == null || apiKey == null || apiKey.isEmpty()) {
			return url;
		}
		// Replace API key parameter value with masked version
		return url.replace("apiKey=" + apiKey, "apiKey=***");
	}
	
	private List<Article> mapToArticles(NewsApiAdapter adapter, String category) {
		if (adapter == null || adapter.getArticles() == null || adapter.getArticles().isEmpty()) {
			return new ArrayList<>();
		}
		
		List<Article> articles = new ArrayList<>(adapter.getArticles().size());
		int skippedNull = 0;
		int skippedMapping = 0;
		int skippedEmptyTitle = 0;
		int mappingErrors = 0;
		
		for (NewsApiAdapter.ArticleResponse articleResponse : adapter.getArticles()) {
			try {
				if (articleResponse == null) {
					skippedNull++;
					continue;
				}
				
				Article article = newsApiArticleMapper.toArticle(articleResponse);
				if (article == null) {
					skippedMapping++;
					continue;
				}
				
				newsApiArticleMapper.updateCategory(article, category);
				
				// Validate title is not null or empty before adding
				if (StringUtils.isNotEmpty(article.getTitle())) {
					articles.add(article);
				} else {
					skippedEmptyTitle++;
				}
			} catch (Exception e) {
				mappingErrors++;
				// Continue processing other articles even if one fails
				// Log only first few errors to avoid log spam
				if (mappingErrors <= 3) {
					log.warn("Error mapping article response to Article entity, skipping: {}", e.getMessage());
				}
			}
		}
		
		// Log aggregated statistics once after processing
		if (skippedNull > 0 || skippedMapping > 0 || skippedEmptyTitle > 0 || mappingErrors > 0) {
			log.debug("Article mapping statistics: {} mapped, {} skipped (null: {}, mapping_failed: {}, empty_title: {}, errors: {})",
				articles.size(), skippedNull + skippedMapping + skippedEmptyTitle + mappingErrors,
				skippedNull, skippedMapping, skippedEmptyTitle, mappingErrors);
		}
		
		return articles;
	}
	
	/**
	 * Fallback method when NewsAPI circuit breaker is open or service is unavailable.
	 * Returns empty list instead of throwing exception to allow graceful degradation.
	 */
	public List<Article> fetchArticlesFallback(String keyword, String category, Exception e) {
		log.error("NewsAPI circuit breaker is open or service unavailable. Using fallback for keyword: {}, category: {}. Returning empty list.", keyword, category, e);
		return new ArrayList<>();
	}
	
	@Override
	public String getApiName() {
		return "NewsAPI";
	}
}

