package com.tispace.common.contract;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Article data transfer object containing article information")
public class ArticleDTO {
	
	@jakarta.validation.constraints.NotNull(message = "Article ID is required")
	@Schema(description = "Unique identifier of the article", example = "01234567-89ab-7def-0123-456789abcdef")
	private UUID id;
	
	@NotBlank(message = "Article title is required and cannot be empty")
	@Size(max = 500, message = "Article title cannot exceed 500 characters")
	@Schema(description = "Title of the article", example = "Breaking: New Technology Released", requiredMode = Schema.RequiredMode.REQUIRED)
	private String title;
	
	@Size(max = 10000, message = "Article description cannot exceed 10000 characters")
	@Schema(description = "Description or content of the article", example = "A revolutionary new technology that promises to transform the industry")
	private String description;
	
	@Size(max = 255, message = "Author name cannot exceed 255 characters")
	@Schema(description = "Author of the article", example = "John Doe")
	private String author;
	
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
	@Schema(description = "Publication date and time of the article", example = "2025-01-18T10:00:00")
	private LocalDateTime publishedAt;
	
	@Size(max = 100, message = "Category cannot exceed 100 characters")
	@Schema(description = "Category of the article", example = "technology")
	private String category;
	
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
	@Schema(description = "Timestamp when the article was created in the system", example = "2025-01-18T12:00:00.000")
	private LocalDateTime createdAt;
	
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
	@Schema(description = "Timestamp when the article was last updated", example = "2025-01-18T12:00:00.000")
	private LocalDateTime updatedAt;
}

