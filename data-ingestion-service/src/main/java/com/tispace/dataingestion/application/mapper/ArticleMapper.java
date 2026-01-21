package com.tispace.dataingestion.application.mapper;

import com.tispace.common.contract.ArticleDTO;
import com.tispace.dataingestion.domain.entity.Article;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ArticleMapper {
	
	ArticleDTO toDTO(Article article);
}

