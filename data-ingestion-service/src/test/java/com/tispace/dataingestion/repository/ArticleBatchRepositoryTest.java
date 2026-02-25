package com.tispace.dataingestion.repository;

import com.tispace.dataingestion.domain.entity.Article;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArticleBatchRepositoryTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    void batchInsertIgnoreDuplicates_countsSuccessNoInfoAndSkipsFailed() {
        ArticleBatchRepository repository = new ArticleBatchRepository(jdbcTemplate);
        List<Article> articles = buildArticles(5);

        when(jdbcTemplate.batchUpdate(eq(
                "INSERT INTO articles (id, title, description, author, published_at, category, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) " +
                        "ON CONFLICT (title, published_at) DO NOTHING"),
                any(BatchPreparedStatementSetter.class)))
                .thenReturn(new int[]{1, 0, Statement.SUCCESS_NO_INFO, 2, Statement.EXECUTE_FAILED});

        int inserted = repository.batchInsertIgnoreDuplicates(articles);

        assertEquals(4, inserted);
        verify(jdbcTemplate, times(1)).batchUpdate(any(String.class), any(BatchPreparedStatementSetter.class));
    }

    @Test
    void batchInsertIgnoreDuplicates_aggregatesResultsAcrossBatches() {
        ArticleBatchRepository repository = new ArticleBatchRepository(jdbcTemplate);
        List<Article> articles = buildArticles(60);

        when(jdbcTemplate.batchUpdate(any(String.class), any(BatchPreparedStatementSetter.class)))
                .thenReturn(new int[]{Statement.SUCCESS_NO_INFO, 1})
                .thenReturn(new int[]{2, 0, Statement.EXECUTE_FAILED});

        int inserted = repository.batchInsertIgnoreDuplicates(articles);

        assertEquals(4, inserted);
        verify(jdbcTemplate, times(2)).batchUpdate(any(String.class), any(BatchPreparedStatementSetter.class));
    }

    @Test
    void batchInsertIgnoreDuplicates_whenInputEmptyOrNull_returnsZero() {
        ArticleBatchRepository repository = new ArticleBatchRepository(jdbcTemplate);

        assertEquals(0, repository.batchInsertIgnoreDuplicates(List.of()));
        assertEquals(0, repository.batchInsertIgnoreDuplicates(null));
        verify(jdbcTemplate, never()).batchUpdate(any(String.class), any(BatchPreparedStatementSetter.class));
    }

    @Test
    void batchInsertIgnoreDuplicates_assignsIdInRepositoryWhenMissing() {
        ArticleBatchRepository repository = new ArticleBatchRepository(jdbcTemplate);
        List<Article> articles = buildArticles(2);
        UUID preSetId = UUID.fromString("01234567-89ab-7def-0123-456789abcdef");
        articles.get(1).setId(preSetId);

        when(jdbcTemplate.batchUpdate(any(String.class), any(BatchPreparedStatementSetter.class)))
                .thenReturn(new int[]{1, 1});

        repository.batchInsertIgnoreDuplicates(articles);

        assertNotNull(articles.get(0).getId());
        assertEquals(preSetId, articles.get(1).getId());
    }

    private List<Article> buildArticles(int count) {
        List<Article> articles = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Article article = new Article();
            article.setTitle("title-" + i);
            article.setDescription("description-" + i);
            article.setAuthor("author-" + i);
            article.setPublishedAt(LocalDateTime.of(2025, 1, 1, 0, 0).plusMinutes(i));
            article.setCategory("technology");
            articles.add(article);
        }
        return articles;
    }
}
