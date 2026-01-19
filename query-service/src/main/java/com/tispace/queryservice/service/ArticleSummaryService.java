package com.tispace.queryservice.service;

import com.tispace.common.dto.ArticleDTO;
import com.tispace.common.dto.SummaryDTO;
import com.tispace.queryservice.constants.ArticleConstants;
import com.tispace.queryservice.service.ArticleQueryService;
import com.tispace.queryservice.service.CacheService;
import com.tispace.queryservice.service.ChatGptService;
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
	private final ArticleQueryService articleQueryService;
	
	@Value("${cache.summary.ttl-hours:24}")
	private int cacheTtlHours;
	
	public SummaryDTO getSummary(Long articleId) {
		log.debug("Fetching summary for article with id: {}", articleId);
		
		// Check cache first
		String cacheKey = ArticleConstants.buildCacheKey(articleId);
		SummaryDTO cachedSummary = cacheService.get(cacheKey, SummaryDTO.class);
		
		if (cachedSummary != null) {
			log.debug("Summary found in cache for article id: {}", articleId);
			cachedSummary.setCached(true);
			return cachedSummary;
		}
		
		// Verify article exists and get it
		ArticleDTO article = articleQueryService.getArticleDTOById(articleId);
		
		// Generate summary using ChatGPT
		String summary = chatGptService.generateSummary(article);
		
		SummaryDTO summaryDTO = SummaryDTO.builder()
			.articleId(articleId)
			.summary(summary)
			.cached(false)
			.build();
		
		// Cache the summary
		long ttlSeconds = (long) cacheTtlHours * SECONDS_PER_HOUR;
		cacheService.put(cacheKey, summaryDTO, ttlSeconds);
		
		return summaryDTO;
	}
}

