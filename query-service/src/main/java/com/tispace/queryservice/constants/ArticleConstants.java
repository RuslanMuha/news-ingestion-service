package com.tispace.queryservice.constants;

public final class ArticleConstants {
	
	private ArticleConstants() {
		// Utility class - prevent instantiation
	}
	
	public static final String CACHE_KEY_PREFIX = "article:summary:";
	public static final String DEFAULT_SORT_FIELD = "publishedAt";
	public static final int DEFAULT_PAGE_SIZE = 20;
	
	public static String buildCacheKey(Long articleId) {
		return CACHE_KEY_PREFIX + articleId;
	}
}


