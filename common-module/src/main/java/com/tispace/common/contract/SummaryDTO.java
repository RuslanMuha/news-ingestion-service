package com.tispace.common.contract;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI-generated summary of an article")
public class SummaryDTO {
	
	@NotNull(message = "Article ID is required")
	@Schema(description = "Unique identifier of the article", example = "01234567-89ab-7def-0123-456789abcdef", requiredMode = Schema.RequiredMode.REQUIRED)
	private UUID articleId;
	
	@NotBlank(message = "Summary text is required and cannot be empty")
	@Schema(
		description = "AI-generated summary text of the article",
		example = "This article discusses a revolutionary new technology that promises to transform the industry. The key innovations include advanced AI integration and cloud-native architecture.",
		requiredMode = Schema.RequiredMode.REQUIRED
	)
	private String summary;
	
	@Schema(
		description = "Indicates whether the summary was retrieved from cache (true) or newly generated (false)",
		example = "false"
	)
	private Boolean cached;
}

