package com.tispace.common.contract;

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
@Schema(description = "Error response containing error details")
public class ErrorResponseDTO {
	
	@Schema(description = "Error code identifying the type of error", example = "ARTICLE_NOT_FOUND")
	private String errorCode;
	
	@Schema(description = "Human-readable error message", example = "Article with id 1 not found")
	private String message;
	
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
	@Schema(description = "Timestamp when the error occurred", example = "2025-01-18T12:00:00.000")
	private LocalDateTime timestamp;
	
	@Schema(description = "API path where the error occurred", example = "/api/articles/1")
	private String path;
}

