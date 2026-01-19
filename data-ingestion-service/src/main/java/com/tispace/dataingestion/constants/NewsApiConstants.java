package com.tispace.dataingestion.constants;

public final class NewsApiConstants {
	
	private NewsApiConstants() {
		// Utility class - prevent instantiation
	}
	
	public static final String STATUS_OK = "ok";
	public static final String DEFAULT_CATEGORY = "general";
	public static final int DEFAULT_PAGE_SIZE = 100;
	public static final String DEFAULT_SORT_BY = "publishedAt";
	
	// URL query parameters
	public static final String PARAM_API_KEY = "apiKey";
	public static final String PARAM_QUERY = "q";
	public static final String PARAM_CATEGORY = "category";
	public static final String PARAM_PAGE_SIZE = "pageSize";
	public static final String PARAM_SORT_BY = "sortBy";
}


