package com.tispace.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Article data transfer object containing article information")
public class ArticleDTO {
	
	@Schema(description = "Unique identifier of the article", example = "1")
	private Long id;
	
	@Schema(description = "Title of the article", example = "Breaking: New Technology Released")
	private String title;
	
	@Schema(description = "Description or content of the article", example = "A revolutionary new technology that promises to transform the industry")
	private String description;
	
	@Schema(description = "Author of the article", example = "John Doe")
	private String author;
	
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
	@Schema(description = "Publication date and time of the article", example = "2025-01-18T10:00:00")
	private LocalDateTime publishedAt;
	
	@Schema(description = "Category of the article", example = "technology")
	private String category;
	
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
	@Schema(description = "Timestamp when the article was created in the system", example = "2025-01-18T12:00:00.000")
	private LocalDateTime createdAt;
	
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
	@Schema(description = "Timestamp when the article was last updated", example = "2025-01-18T12:00:00.000")
	private LocalDateTime updatedAt;
}


