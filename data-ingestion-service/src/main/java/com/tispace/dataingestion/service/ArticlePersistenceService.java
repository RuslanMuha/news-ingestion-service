package com.tispace.dataingestion.service;

import com.tispace.common.entity.Article;
import com.tispace.common.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArticlePersistenceService {
	
	private final ArticleRepository articleRepository;
	
	/**
	 * @param articles List of articles to save
	 * @return Number of successfully saved articles
	 */
	@Transactional
	public int saveArticles(List<Article> articles) {
		return saveArticlesInBatches(articles);
	}
	
	/**
	 * Saves articles in batches, skipping duplicates based on unique title constraint.
	 * This method is thread-safe when used with unique index on title column.
	 */
	private int saveArticlesInBatches(List<Article> articles) {
		int savedCount = 0;
		final int batchSize = 50; // Match hibernate.jdbc.batch_size
		
		// Process in batches
		for (int i = 0; i < articles.size(); i += batchSize) {
			int end = Math.min(i + batchSize, articles.size());
			List<Article> batch = articles.subList(i, end);
			
			try {
				// Try batch insert first
				List<Article> saved = articleRepository.saveAll(batch);
				savedCount += saved.size();
				log.debug("Saved batch of {} articles (batch {})", saved.size(), (i / batchSize) + 1);
			} catch (DataIntegrityViolationException e) {
				// If batch fails due to duplicate, save individually
				log.debug("Batch insert failed due to duplicate, saving individually");
				savedCount += saveArticlesIndividually(batch);
			}
		}
		
		return savedCount;
	}
	
	/**
	 * Saves articles one by one, skipping duplicates.
	 * Thread-safe: unique index will prevent duplicate inserts even in multi-threaded environment.
	 */
	private int saveArticlesIndividually(List<Article> articles) {
		int savedCount = 0;
		for (Article article : articles) {
			try {
				articleRepository.save(article);
				savedCount++;
			} catch (DataIntegrityViolationException e) {
				// Skip duplicate - unique index on title prevents insertion
				log.debug("Skipping duplicate article with title: {}", article.getTitle());
			}
		}
		return savedCount;
	}
}

