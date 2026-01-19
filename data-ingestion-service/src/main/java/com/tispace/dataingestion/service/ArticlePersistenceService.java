package com.tispace.dataingestion.service;

import com.tispace.common.entity.Article;
import com.tispace.dataingestion.repository.ArticleBatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for persisting articles to database.
 * Uses batch UPSERT operations with PostgreSQL ON CONFLICT DO NOTHING
 * for efficient handling of duplicates in multi-instance environment.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ArticlePersistenceService {
	
	private final ArticleBatchRepository articleBatchRepository;
	
	private static final int BATCH_SIZE = 50; // Match hibernate.jdbc.batch_size
	
	/**
	 * Saves articles using batch UPSERT with ON CONFLICT DO NOTHING.
	 * This approach is efficient and handles race conditions when multiple instances
	 * try to insert the same articles concurrently.
	 * 
	 * @param articles List of articles to save
	 * @return Number of successfully saved articles (excluding duplicates)
	 */
	@Transactional(isolation = Isolation.READ_COMMITTED, timeout = 300)
	public int saveArticles(List<Article> articles) {
		if (articles == null || articles.isEmpty()) {
			return 0;
		}
		
		int totalSaved = 0;
		final int totalBatches = (articles.size() + BATCH_SIZE - 1) / BATCH_SIZE;
		
		// Process in batches to avoid memory issues and respect DB batch size limits
		for (int i = 0; i < articles.size(); i += BATCH_SIZE) {
			int end = Math.min(i + BATCH_SIZE, articles.size());
			List<Article> batch = new ArrayList<>(articles.subList(i, end));
			
			int batchSaved = articleBatchRepository.batchInsertIgnoreDuplicates(batch);
			totalSaved += batchSaved;
			
			int batchNumber = (i / BATCH_SIZE) + 1;
			log.debug("Processed batch {}/{}: {} articles inserted, {} skipped (duplicates)", 
				batchNumber, totalBatches, batchSaved, batch.size() - batchSaved);
		}
		
		return totalSaved;
	}
}

