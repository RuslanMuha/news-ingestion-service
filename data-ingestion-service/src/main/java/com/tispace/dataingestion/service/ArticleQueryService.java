package com.tispace.dataingestion.service;

import com.tispace.common.dto.ArticleDTO;
import com.tispace.common.entity.Article;
import com.tispace.common.exception.NotFoundException;
import com.tispace.common.mapper.ArticleMapper;
import com.tispace.common.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ArticleQueryService {
	
	private final ArticleRepository articleRepository;
	private final ArticleMapper articleMapper;
	
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
	
	public Article getArticleById(Long id) {
		return articleRepository.findById(id)
			.orElseThrow(() -> new NotFoundException("Article", id));
	}
	
	public ArticleDTO getArticleDTOById(Long id) {
		Article article = getArticleById(id);
		return articleMapper.toDTO(article);
	}
}

