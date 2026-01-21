package com.tispace.dataingestion.infrastructure.repository;

import com.tispace.dataingestion.domain.entity.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ArticleRepository extends JpaRepository<Article, UUID> {
	
	Page<Article> findByCategory(String category, Pageable pageable);
	
	Optional<Article> findTop1ByOrderByCreatedAtDesc();

}

