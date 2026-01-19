package com.tispace.dataingestion.controller;

import com.tispace.common.dto.ArticleDTO;
import com.tispace.common.dto.SummaryDTO;
import com.tispace.common.exception.BusinessException;
import com.tispace.dataingestion.client.QueryServiceClient;
import com.tispace.dataingestion.service.ArticleQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Articles", description = "Public API for querying news articles and generating AI-powered summaries")
public class ArticleController {
	
	private static final Set<String> VALID_SORT_FIELDS = Set.of(
		"id", "title", "description", "author", "publishedAt", "category", "createdAt", "updatedAt"
	);
	
	private final ArticleQueryService articleQueryService;
	private final QueryServiceClient queryServiceClient;
	
	@GetMapping
	@Operation(
		summary = "Get paginated list of articles",
		description = "Retrieves a paginated list of articles with optional filtering by category. Results are sorted by published date in descending order by default."
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
			responseCode = "500",
			description = "Internal server error"
		)
	})
	public ResponseEntity<Page<ArticleDTO>> getArticles(
		@Parameter(
			description = "Page number (0-indexed). Default: 0",
			example = "0"
		)
		@RequestParam(required = false, defaultValue = "0")
		@Min(value = 0, message = "Page number must be non-negative")
		Integer page,
		@Parameter(
			description = "Page size. Default: 20",
			example = "20"
		)
		@RequestParam(required = false, defaultValue = "20")
		@Min(value = 1, message = "Page size must be at least 1")
		@Max(value = 100, message = "Page size cannot exceed 100")
		Integer size,
		@Parameter(
			description = "Sort field and direction (format: 'field,direction'). Default: 'publishedAt,desc'. Example: 'publishedAt,desc' or 'title,asc'",
			example = "publishedAt,desc"
		)
		@RequestParam(required = false, defaultValue = "publishedAt,desc")
		@Pattern(regexp = "^[a-zA-Z]+,(asc|desc)$", message = "Sort parameter must be in format 'field,direction' where direction is 'asc' or 'desc'")
		String sort,
		@Parameter(
			description = "Filter articles by category (optional)",
			example = "technology"
		)
		@RequestParam(required = false)
		@Size(max = 100, message = "Category cannot exceed 100 characters")
		String category) {
		
		// Parse sort string into Sort object
		Sort sortObj = parseSortString(sort);
		Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size, sortObj);
		
		log.debug("Fetching articles: pageable={}, category={}", pageable, category);
		
		Page<ArticleDTO> articles = articleQueryService.getArticlesDTO(pageable, category);
		return ResponseEntity.ok(articles);
	}
	
	private Sort parseSortString(String sortString) {
		if (sortString == null || sortString.isEmpty()) {
			return Sort.by(Sort.Direction.DESC, "publishedAt");
		}
		
		String[] parts = sortString.split(",");
		if (parts.length != 2) {
			throw new BusinessException("Sort parameter must be in format 'field,direction' (e.g., 'publishedAt,desc')");
		}
		
		String field = parts[0].trim();
		String direction = parts[1].trim().toLowerCase();
		
		// Validate sort field
		if (field.isEmpty()) {
			throw new BusinessException("Sort field cannot be empty");
		}
		
		if (!VALID_SORT_FIELDS.contains(field)) {
			throw new BusinessException(String.format("Invalid sort field: %s. Valid fields are: %s", 
				field, String.join(", ", VALID_SORT_FIELDS)));
		}
		
		// Validate sort direction
		if (direction.isEmpty()) {
			throw new BusinessException("Sort direction cannot be empty");
		}
		
		Sort.Direction sortDirection;
		if ("asc".equals(direction)) {
			sortDirection = Sort.Direction.ASC;
		} else if ("desc".equals(direction)) {
			sortDirection = Sort.Direction.DESC;
		} else {
			throw new BusinessException(String.format("Invalid sort direction: %s. Must be 'asc' or 'desc'", direction));
		}
		
		return Sort.by(sortDirection, field);
	}
	
	@GetMapping("/{id}")
	@Operation(
		summary = "Get article by ID",
		description = "Retrieves a single article by its unique identifier"
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
			responseCode = "500",
			description = "Internal server error"
		)
	})
	public ResponseEntity<ArticleDTO> getArticleById(
		@Parameter(
			description = "Unique identifier of the article",
			required = true,
			example = "1"
		)
		@PathVariable
		@jakarta.validation.constraints.NotNull(message = "Article ID is required")
		@jakarta.validation.constraints.Positive(message = "Article ID must be a positive number")
		Long id) {
		
		log.debug("Fetching article with id: {}", id);
		
		ArticleDTO article = articleQueryService.getArticleDTOById(id);
		return ResponseEntity.ok(article);
	}
	
	@GetMapping("/{id}/summary")
	@Operation(
		summary = "Get AI-generated article summary",
		description = "Retrieves an AI-generated summary of an article using ChatGPT. The summary is cached for 24 hours. First request generates the summary, subsequent requests return the cached version."
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
			responseCode = "500",
			description = "Internal server error or ChatGPT API error"
		)
	})
	public ResponseEntity<SummaryDTO> getArticleSummary(
		@Parameter(
			description = "Unique identifier of the article",
			required = true,
			example = "1"
		)
		@PathVariable
		@jakarta.validation.constraints.NotNull(message = "Article ID is required")
		@jakarta.validation.constraints.Positive(message = "Article ID must be a positive number")
		Long id) {
		
		log.debug("Fetching summary for article with id: {}", id);
		
		// Get article first
		ArticleDTO article = articleQueryService.getArticleDTOById(id);
		
		// Get summary from query-service
		SummaryDTO summary = queryServiceClient.getArticleSummary(id, article);
		return ResponseEntity.ok(summary);
	}
}


