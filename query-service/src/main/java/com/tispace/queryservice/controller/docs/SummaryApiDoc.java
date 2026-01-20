package com.tispace.queryservice.controller.docs;

import com.tispace.common.dto.ArticleDTO;
import com.tispace.common.dto.SummaryDTO;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;

@Hidden // Internal API - not exposed in OpenAPI documentation
public interface SummaryApiDoc {
	
	ResponseEntity<SummaryDTO> generateOrGetSummary(
		@NotNull(message = "Article ID is required")
		@Positive(message = "Article ID must be a positive number")
		Long articleId,
		@Valid
		@NotNull(message = "Article body is required")
		ArticleDTO article
	);
}

