package com.tispace.common.mapper;

import com.tispace.common.dto.ArticleDTO;
import com.tispace.common.entity.Article;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ArticleMapper {
	
	ArticleDTO toDTO(Article article);
	
	Article toEntity(ArticleDTO dto);
}

