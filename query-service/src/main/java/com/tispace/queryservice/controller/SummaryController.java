package com.tispace.queryservice.controller;

import com.tispace.common.dto.ArticleDTO;
import com.tispace.common.dto.SummaryDTO;
import com.tispace.queryservice.service.ArticleSummaryService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/summary")
@RequiredArgsConstructor
@Slf4j
@Hidden // Internal API - not exposed in OpenAPI documentation
public class SummaryController {
	
	private final ArticleSummaryService articleSummaryService;
	
	@PostMapping("/{articleId}")
	public ResponseEntity<SummaryDTO> getArticleSummary(
		@PathVariable Long articleId,
		@RequestBody ArticleDTO article) {
		log.debug("Fetching summary for article with id: {}", articleId);
		
		SummaryDTO summary = articleSummaryService.getSummary(articleId, article);
		return ResponseEntity.ok(summary);
	}
}

