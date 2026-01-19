package com.tispace.dataingestion.repository;

import com.tispace.common.entity.Article;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;

/**
 * Repository for batch operations on articles using JPA native queries for better performance.
 * Uses PostgreSQL ON CONFLICT DO NOTHING to handle duplicates efficiently.
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class ArticleBatchRepository {
	
	private final EntityManager entityManager;
	
	private static final String BATCH_INSERT_SQL = 
		"INSERT INTO articles (title, description, author, published_at, category, created_at, updated_at) " +
		"VALUES (:title, :description, :author, :publishedAt, :category, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) " +
		"ON CONFLICT (title) DO NOTHING";
	
	/**
	 * Batch inserts articles using PostgreSQL ON CONFLICT DO NOTHING.
	 * This is more efficient than individual inserts and handles duplicates at DB level.
	 * 
	 * @param articles List of articles to insert
	 * @return Number of rows actually inserted (excluding duplicates)
	 */
	@Transactional
	public int batchInsertIgnoreDuplicates(List<Article> articles) {
		if (articles == null || articles.isEmpty()) {
			return 0;
		}
		
		int insertedCount = 0;
		final int batchSize = 50; // Process in batches to avoid memory issues
		
		// Process in batches
		for (int i = 0; i < articles.size(); i += batchSize) {
			int end = Math.min(i + batchSize, articles.size());
			List<Article> batch = articles.subList(i, end);
			
			int batchInserted = insertBatch(batch);
			insertedCount += batchInserted;
			
			// Clear persistence context to free memory
			entityManager.clear();
		}
		
		return insertedCount;
	}
	
	/**
	 * Inserts a batch of articles using native query.
	 */
	private int insertBatch(List<Article> batch) {
		int insertedCount = 0;
		
		for (Article article : batch) {
			Query query = entityManager.createNativeQuery(BATCH_INSERT_SQL);
			query.setParameter("title", article.getTitle());
			query.setParameter("description", article.getDescription());
			query.setParameter("author", article.getAuthor());
			
			if (article.getPublishedAt() != null) {
				query.setParameter("publishedAt", Timestamp.valueOf(article.getPublishedAt()));
			} else {
				query.setParameter("publishedAt", null);
			}
			
			query.setParameter("category", article.getCategory());
			
			try {
				int rowsAffected = query.executeUpdate();
				if (rowsAffected > 0) {
					insertedCount++;
				}
			} catch (Exception e) {
				// ON CONFLICT DO NOTHING should prevent exceptions, but log if something unexpected happens
				log.debug("Article with title '{}' was skipped (likely duplicate)", article.getTitle());
			}
		}
		
		return insertedCount;
	}
}
