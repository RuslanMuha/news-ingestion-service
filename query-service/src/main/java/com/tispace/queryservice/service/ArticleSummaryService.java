package com.tispace.queryservice.service;

import com.tispace.common.dto.ArticleDTO;
import com.tispace.common.dto.SummaryDTO;
import com.tispace.queryservice.constants.ArticleConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArticleSummaryService {
	
	private static final int SECONDS_PER_HOUR = 60 * 60;
	
	private final CacheService cacheService;
	private final ChatGptService chatGptService;
	
	@Value("${cache.summary.ttl-hours:24}")
	private int cacheTtlHours;
	
	/**
	 * Get summary for an article. The article data should be provided by the caller.
	 * This method only handles caching and ChatGPT generation.
	 */
	public SummaryDTO getSummary(Long articleId, ArticleDTO article) {
		if (articleId == null) {
			throw new IllegalArgumentException("Article ID cannot be null");
		}
		
		if (article == null) {
			throw new IllegalArgumentException("Article cannot be null");
		}
		
		log.debug("Fetching summary for article with id: {}", articleId);
		
		// Check cache first
		String cacheKey = ArticleConstants.buildCacheKey(articleId);
		SummaryDTO cachedSummary = cacheService.get(cacheKey, SummaryDTO.class);
		
		if (cachedSummary != null) {
			log.debug("Summary found in cache for article id: {}", articleId);
			cachedSummary.setCached(true);
			return cachedSummary;
		}
		
		// Generate summary using ChatGPT
		String summary = chatGptService.generateSummary(article);
		
		if (summary == null || summary.trim().isEmpty()) {
			log.error("Generated summary is null or empty for article id: {}", articleId);
			throw new IllegalStateException("Generated summary is empty");
		}
		
		SummaryDTO summaryDTO = SummaryDTO.builder()
			.articleId(articleId)
			.summary(summary)
			.cached(false)
			.build();
		
		// Cache the summary (fail silently if cache fails - summary is still returned)
		try {
			long ttlSeconds = (long) cacheTtlHours * SECONDS_PER_HOUR;
			if (ttlSeconds > 0) {
				cacheService.put(cacheKey, summaryDTO, ttlSeconds);
			} else {
				log.warn("Invalid TTL calculation for article id: {} (ttlHours: {}). Skipping cache.", articleId, cacheTtlHours);
			}
		} catch (Exception e) {
			log.warn("Failed to cache summary for article id: {}. Summary will still be returned.", articleId, e);
			// Continue - cache failure should not prevent returning the summary
		}
		
		return summaryDTO;
	}
	
}

