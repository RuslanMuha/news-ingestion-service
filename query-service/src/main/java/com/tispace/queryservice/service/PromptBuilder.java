package com.tispace.queryservice.service;

import com.tispace.common.dto.ArticleDTO;
import org.apache.commons.lang3.StringUtils;
import com.tispace.queryservice.constants.ChatGptConstants;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public final class PromptBuilder {
	
	public static String buildSummaryPrompt(ArticleDTO article) {
		StringBuilder prompt = new StringBuilder();
		prompt.append(ChatGptConstants.PROMPT_PREFIX);
		prompt.append(ChatGptConstants.PROMPT_TITLE_PREFIX)
			.append(article.getTitle())
			.append("\n\n");
		
		if (StringUtils.isNotEmpty(article.getDescription())) {
			prompt.append(ChatGptConstants.PROMPT_DESCRIPTION_PREFIX)
				.append(article.getDescription())
				.append("\n");
		}
		
		prompt.append(ChatGptConstants.PROMPT_SUMMARY_SUFFIX);
		return prompt.toString();
	}
}

