package com.tispace.queryservice.service;

import com.tispace.common.dto.ArticleDTO;
import org.apache.commons.lang3.StringUtils;
import com.tispace.queryservice.constants.ChatGptConstants;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public final class PromptBuilder {
	
	public static String buildSummaryPrompt(ArticleDTO article) {
		if (article == null) {
			throw new IllegalArgumentException("Article cannot be null");
		}
		
		StringBuilder prompt = new StringBuilder();
		prompt.append(ChatGptConstants.PROMPT_PREFIX);
		
		String title = article.getTitle();
		if (StringUtils.isNotEmpty(title)) {
			prompt.append(ChatGptConstants.PROMPT_TITLE_PREFIX)
				.append(title)
				.append("\n\n");
		} else {
			prompt.append(ChatGptConstants.PROMPT_TITLE_PREFIX)
				.append("(No title available)")
				.append("\n\n");
		}
		
		if (StringUtils.isNotEmpty(article.getDescription())) {
			prompt.append(ChatGptConstants.PROMPT_DESCRIPTION_PREFIX)
				.append(article.getDescription())
				.append("\n");
		}
		
		prompt.append(ChatGptConstants.PROMPT_SUMMARY_SUFFIX);
		return prompt.toString();
	}
}

