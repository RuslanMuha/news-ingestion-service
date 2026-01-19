package com.tispace.queryservice.service;

import com.tispace.common.dto.ArticleDTO;
import com.tispace.queryservice.client.DataIngestionServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArticleQueryService {
	
	private final DataIngestionServiceClient dataIngestionServiceClient;
	
	public Page<ArticleDTO> getArticlesDTO(Pageable pageable, String category) {
		log.debug("Getting articles from data-ingestion-service: pageable={}, category={}", pageable, category);
		return dataIngestionServiceClient.getArticles(pageable, category);
	}
	
	public ArticleDTO getArticleDTOById(Long id) {
		log.debug("Getting article by id from data-ingestion-service: id={}", id);
		return dataIngestionServiceClient.getArticleById(id);
	}
}

