package com.tispace.dataingestion.mapper;

import com.tispace.common.entity.Article;
import com.tispace.dataingestion.adapter.NewsApiAdapter;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface NewsApiArticleMapper {
	
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	@Mapping(target = "category", ignore = true)
	@Mapping(target = "publishedAt", expression = "java(articleResponse.getPublishedAtLocalDateTime())")
	Article toArticle(NewsApiAdapter.ArticleResponse articleResponse);
	
	default void updateCategory(@MappingTarget Article article, String category) {
		if (org.apache.commons.lang3.StringUtils.isNotEmpty(category)) {
			article.setCategory(category);
		} else {
			article.setCategory(com.tispace.dataingestion.constants.NewsApiConstants.DEFAULT_CATEGORY);
		}
	}
}

