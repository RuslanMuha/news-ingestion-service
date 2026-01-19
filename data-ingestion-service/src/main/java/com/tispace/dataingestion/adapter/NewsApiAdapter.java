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
	private Integer totalResults;
	private List<ArticleResponse> articles;
	
	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ArticleResponse {
		private Source source;
		private String author;
		private String title;
		private String description;
		
		@JsonProperty("publishedAt")
		private String publishedAt;
		
		private String url;
		private String urlToImage;
		private String content;
		
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
	
	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Source {
		private String id;
		private String name;
	}
}


