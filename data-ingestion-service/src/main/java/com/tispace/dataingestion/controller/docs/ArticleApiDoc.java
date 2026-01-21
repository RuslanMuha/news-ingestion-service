package com.tispace.dataingestion.controller.docs;

import com.tispace.common.contract.ArticleDTO;
import com.tispace.common.contract.SummaryDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

@Tag(name = "Articles", description = "Public API for querying news articles and generating AI-powered summaries")
public interface ArticleApiDoc {
	
	@Operation(
		summary = "Get paginated list of articles",
		description = "Retrieves a paginated list of articles with optional filtering by category. Results are sorted by published date in descending order by default. Rate limited to prevent abuse."
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "Successfully retrieved articles",
			content = @Content(schema = @Schema(implementation = Page.class))
		),
		@ApiResponse(
			responseCode = "400",
			description = "Invalid request parameters"
		),
		@ApiResponse(
			responseCode = "429",
			description = "Rate limit exceeded. Default limit: 100 requests per minute per endpoint. Retry after the rate limit period expires."
		),
		@ApiResponse(
			responseCode = "500",
			description = "Internal server error"
		)
	})
	ResponseEntity<Page<ArticleDTO>> getArticles(
		@Parameter(
			description = "Page number (0-indexed). Default: 0",
			example = "0"
		)
		@Min(value = 0, message = "Page number must be non-negative")
		Integer page,
		@Parameter(
			description = "Page size. Default: 20",
			example = "20"
		)
		@Min(value = 1, message = "Page size must be at least 1")
		@Max(value = 100, message = "Page size cannot exceed 100")
		Integer size,
		@Parameter(
			description = "Sort field and direction (format: 'field,direction'). Default: 'publishedAt,desc'. Example: 'publishedAt,desc' or 'title,asc'",
			example = "publishedAt,desc"
		)
		@Size(max = 50)
		String sort,
		@Parameter(
			description = "Filter articles by category (optional)",
			example = "technology"
		)
		@Size(max = 100, message = "Category cannot exceed 100 characters")
		String category
	);
	
	@Operation(
		summary = "Get article by ID",
		description = "Retrieves a single article by its unique identifier. " +
			"Rate limited to 100 requests per minute. " +
			"Supports correlation ID via X-Correlation-ID header."
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "Successfully retrieved article",
			content = @Content(schema = @Schema(implementation = ArticleDTO.class))
		),
		@ApiResponse(
			responseCode = "404",
			description = "Article not found"
		),
		@ApiResponse(
			responseCode = "400",
			description = "Invalid article ID"
		),
		@ApiResponse(
			responseCode = "429",
			description = "Rate limit exceeded"
		),
		@ApiResponse(
			responseCode = "500",
			description = "Internal server error"
		)
	})
	ResponseEntity<ArticleDTO> getArticleById(
		@Parameter(
			description = "Unique identifier of the article",
			required = true,
			example = "01234567-89ab-7def-0123-456789abcdef"
		)
		@NotNull(message = "Article ID is required")
		UUID id
	);
	
	@Operation(
		summary = "Get AI-generated article summary",
		description = "Retrieves an AI-generated summary of an article using ChatGPT. " +
			"The summary is cached for 24 hours with TTL jitter to prevent cache stampede. " +
			"First request generates the summary, subsequent requests return the cached version. " +
			"Uses single-flight pattern to prevent concurrent generation for the same article. " +
			"Rate limited to 100 requests per minute. " +
			"Supports correlation ID via X-Correlation-ID header."
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "Successfully retrieved article summary",
			content = @Content(schema = @Schema(implementation = SummaryDTO.class))
		),
		@ApiResponse(
			responseCode = "404",
			description = "Article not found"
		),
		@ApiResponse(
			responseCode = "400",
			description = "Invalid article ID"
		),
		@ApiResponse(
			responseCode = "429",
			description = "Rate limit exceeded"
		),
		@ApiResponse(
			responseCode = "500",
			description = "Internal server error or ChatGPT API error"
		)
	})
	ResponseEntity<SummaryDTO> getArticleSummary(
		@Parameter(
			description = "Unique identifier of the article",
			required = true,
			example = "01234567-89ab-7def-0123-456789abcdef"
		)
		@NotNull(message = "Article ID is required")
		UUID id
	);
}

