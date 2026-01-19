package com.tispace.common.repository;

import com.tispace.common.entity.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {
	
	Page<Article> findByCategory(String category, Pageable pageable);
	
	Optional<Article> findTop1ByOrderByCreatedAtDesc();
	
	/**
	 * Finds existing article titles from the provided set.
	 * Used to filter out duplicates before batch insert.
	 */
	@Query("SELECT a.title FROM Article a WHERE a.title IN :titles")
	Set<String> findExistingTitles(@Param("titles") Set<String> titles);
}

