package com.tispace.queryservice.controller;

import com.tispace.common.contract.ArticleDTO;
import com.tispace.common.contract.SummaryDTO;
import com.tispace.queryservice.controller.docs.SummaryApiDoc;
import com.tispace.queryservice.service.ArticleSummaryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/internal/summary")
@RequiredArgsConstructor
@Slf4j
@Validated
public class SummaryController implements SummaryApiDoc {
	
	private final ArticleSummaryService articleSummaryService;
	
	@PostMapping("/{articleId}")
	@Override
	public ResponseEntity<SummaryDTO> generateOrGetSummary(
		@PathVariable
		@NotNull(message = "Article ID is required")
		UUID articleId,
		@RequestBody
		@Valid
		@NotNull(message = "Article body is required")
		ArticleDTO article) {

		log.debug("Fetching summary for article with id: {}", articleId);
		var summary = articleSummaryService.getSummary(articleId, article);
		return ResponseEntity.ok(summary);
	}
}

