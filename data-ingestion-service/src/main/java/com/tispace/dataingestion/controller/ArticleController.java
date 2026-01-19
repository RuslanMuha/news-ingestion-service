package com.tispace.dataingestion.controller;

import com.tispace.common.dto.ArticleDTO;
import com.tispace.dataingestion.service.ArticleQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
@Slf4j
public class ArticleController {
	
	private final ArticleQueryService articleQueryService;
	
	@GetMapping
	public ResponseEntity<Page<ArticleDTO>> getArticles(
		@PageableDefault(size = 20, sort = "publishedAt", direction = Sort.Direction.DESC) Pageable pageable,
		@RequestParam(required = false) String category) {
		
		log.debug("Fetching articles: pageable={}, category={}", pageable, category);
		
		Page<ArticleDTO> articles = articleQueryService.getArticlesDTO(pageable, category);
		return ResponseEntity.ok(articles);
	}
	
	@GetMapping("/{id}")
	public ResponseEntity<ArticleDTO> getArticleById(@PathVariable Long id) {
		log.debug("Fetching article with id: {}", id);
		
		ArticleDTO article = articleQueryService.getArticleDTOById(id);
		return ResponseEntity.ok(article);
	}
}


