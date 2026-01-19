package com.tispace.dataingestion.adapter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NewsApiAdapter {
	
	private String status;
	private List<ArticleResponse> articles;
	
	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ArticleResponse {
		private String author;
		private String title;
		private String description;
		
		@JsonProperty("publishedAt")
		private String publishedAt;
		
		public LocalDateTime getPublishedAtLocalDateTime() {
			if (publishedAt == null || publishedAt.isEmpty()) {
				return null;
			}
			try {
				return ZonedDateTime.parse(publishedAt).toLocalDateTime();
			} catch (Exception e) {
				return null;
			}
		}
	}
}


