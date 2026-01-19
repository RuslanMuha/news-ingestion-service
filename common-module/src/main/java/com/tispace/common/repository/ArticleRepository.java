package com.tispace.common.repository;

import com.tispace.common.entity.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {
	
	Page<Article> findByCategory(String category, Pageable pageable);
	
	Optional<Article> findById(Long id);
	
	Optional<Article> findByTitle(String title);
	
	@Query("SELECT a FROM Article a WHERE a.publishedAt BETWEEN :startDate AND :endDate")
	Page<Article> findByPublishedAtBetween(
		@Param("startDate") LocalDateTime startDate,
		@Param("endDate") LocalDateTime endDate,
		Pageable pageable
	);
	
	Optional<Article> findTop1ByOrderByCreatedAtDesc();
}

