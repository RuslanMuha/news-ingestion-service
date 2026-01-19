package com.tispace.dataingestion.service;

import com.tispace.common.entity.Article;
import com.tispace.common.repository.ArticleRepository;
import org.apache.commons.lang3.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataIngestionService {
	
	private final ExternalApiClient externalApiClient;
	private final ArticleRepository articleRepository;
	
	@Value("${scheduler.keyword:technology}")
	private String defaultKeyword;
	
	@Value("${scheduler.category:technology}")
	private String defaultCategory;
	
	@Transactional
	public void ingestData(String keyword, String category) {
		try {
			log.info("Starting data ingestion with keyword: {}, category: {}", keyword, category);
			
			String searchKeyword = StringUtils.isNotEmpty(keyword) ? keyword : defaultKeyword;
			String searchCategory = StringUtils.isNotEmpty(category) ? category : defaultCategory;
			
			List<Article> articles = externalApiClient.fetchArticles(searchKeyword, searchCategory);
			
			log.info("Fetched {} articles from {}", articles.size(), externalApiClient.getApiName());
			
			if (articles.isEmpty()) {
				log.warn("No articles fetched from {}", externalApiClient.getApiName());
				return;
			}
			
			int savedCount = 0;
			for (Article article : articles) {
				if (articleRepository.findByTitle(article.getTitle()).isEmpty()) {
					articleRepository.save(article);
					savedCount++;
				}
			}
			
			log.info("Successfully saved {} new articles to database", savedCount);
			
		} catch (Exception e) {
			log.error("Error during data ingestion", e);
			throw e;
		}
	}

    @Transactional
	public void ingestData() {
		ingestData(defaultKeyword, defaultCategory);
	}
}

