package com.tispace.dataingestion.service;

import com.tispace.common.contract.ArticleDTO;
import com.tispace.dataingestion.domain.entity.Article;
import com.tispace.common.exception.NotFoundException;
import com.tispace.dataingestion.application.mapper.ArticleMapper;
import com.tispace.dataingestion.infrastructure.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ArticleQueryService {
	
	private final ArticleRepository articleRepository;
	private final ArticleMapper articleMapper;
	
	@Retry(name = "database")
	public Page<Article> getArticles(Pageable pageable, String category) {
		if (StringUtils.isNotEmpty(category)) {
			return articleRepository.findByCategory(category, pageable);
		}
		return articleRepository.findAll(pageable);
	}
	
	public Page<ArticleDTO> getArticlesDTO(Pageable pageable, String category) {
		return getArticles(pageable, category)
			.map(articleMapper::toDTO);
	}
	
	@Retry(name = "database")
	public Article getArticleById(UUID id) {
		return articleRepository.findById(id)
			.orElseThrow(() -> new NotFoundException("Article", id));
	}
	
	public ArticleDTO getArticleDTOById(UUID id) {
		Article article = getArticleById(id);
		return articleMapper.toDTO(article);
	}
}

