package com.tispace.dataingestion.service;

import com.tispace.dataingestion.domain.entity.Article;

import java.util.List;

public interface ExternalApiClient {
	
	List<Article> fetchArticles(String keyword, String category);
	
	String getApiName();
}



