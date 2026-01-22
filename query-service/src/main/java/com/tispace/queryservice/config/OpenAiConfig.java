package com.tispace.queryservice.config;

import com.theokanning.openai.service.OpenAiService;
import org.apache.commons.lang3.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@Slf4j
public class OpenAiConfig {
	
	@Bean
	public OpenAiService openAiService(@Value("${openai.api-key:}") String apiKey,
	                                   @Value("${openai.timeout:60}") int timeoutSeconds) {
		if (StringUtils.isEmpty(apiKey)) {
			log.warn("OpenAI API key is not configured. ChatGptService will not work.");
			return null;
		}
		
		Duration timeout = Duration.ofSeconds(timeoutSeconds);
		log.info("Creating OpenAiService with timeout: {} seconds ({} ms)", timeoutSeconds, timeout.toMillis());
		return new OpenAiService(apiKey, timeout);
	}
}

