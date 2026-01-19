package com.tispace.queryservice.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tispace.common.dto.ArticleDTO;
import com.tispace.common.exception.ExternalApiException;
import com.tispace.common.exception.NotFoundException;
import org.apache.commons.lang3.StringUtils;
import com.tispace.queryservice.constants.ApiConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataIngestionServiceClient {

    @Value("${services.data-ingestion.url:http://data-ingestion-service:8081}")
    private String ingestionServiceUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public Page<ArticleDTO> getArticles(Pageable pageable, String category) {
        try {
            String url = buildArticlesUrl(pageable, category);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return deserializePageResponse(response.getBody(), pageable);
            }

            log.warn("Unexpected response from data-ingestion-service: {}", response.getStatusCode());
            return new PageImpl<>(new ArrayList<>());

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.warn("Articles not found: {}", e.getMessage());
                return new PageImpl<>(new ArrayList<>());
            }
            throw e;
        } catch (Exception e) {
            handleHttpException("articles", e);
            return new PageImpl<>(new ArrayList<>()); // unreachable, but required for compilation
        }
    }

    public ArticleDTO getArticleById(Long id) {
        try {
            String url = buildArticleUrl(id);

            ResponseEntity<ArticleDTO> response = restTemplate.getForEntity(url, ArticleDTO.class);

            if (isSuccessResponse(response)) {
                return response.getBody();
            }

            throw new NotFoundException("Article", id);

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.warn("Article not found with id: {}", id);
                throw new NotFoundException("Article", id);
            }
            throw e;
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            handleHttpException("article id: " + id, e);
            throw new NotFoundException("Article", id); // unreachable, but required for compilation
        }
    }

    private String buildArticlesUrl(Pageable pageable, String category) {
        String baseUrl = String.format("%s%s", ingestionServiceUrl, ApiConstants.ARTICLES_API_PATH);

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl)
                .queryParam("page", pageable.getPageNumber())
                .queryParam("size", pageable.getPageSize());

        if (pageable.getSort().isSorted()) {
            String sort = pageable.getSort().toString()
                    .replace(": ", ",")
                    .replace(" ", ",");
            builder.queryParam("sort", sort);
        }

        if (StringUtils.isNotEmpty(category)) {
            builder.queryParam("category", category);
        }

        return builder.toUriString();
    }

    private String buildArticleUrl(Long id) {
        return ingestionServiceUrl + ApiConstants.ARTICLES_API_PATH + "/" + id;
    }

    @SuppressWarnings("unchecked")
    private Page<ArticleDTO> deserializePageResponse(String jsonBody, Pageable pageable) {
        try {
            Map<String, Object> pageMap = objectMapper.readValue(jsonBody, new TypeReference<Map<String, Object>>() {});
            
            List<Map<String, Object>> contentList = (List<Map<String, Object>>) pageMap.get("content");
            List<ArticleDTO> articles = objectMapper.convertValue(contentList, 
                new TypeReference<List<ArticleDTO>>() {});
            
            int totalElements = (Integer) pageMap.getOrDefault("totalElements", 0);
            int number = (Integer) pageMap.getOrDefault("number", pageable.getPageNumber());
            int size = (Integer) pageMap.getOrDefault("size", pageable.getPageSize());
            
            Pageable resultPageable = PageRequest.of(number, size, pageable.getSort());
            return new PageImpl<>(articles, resultPageable, totalElements);
            
        } catch (Exception e) {
            log.error("Error deserializing page response", e);
            throw new ExternalApiException("Failed to deserialize page response: " + e.getMessage(), e);
        }
    }

    private boolean isSuccessResponse(ResponseEntity<?> response) {
        return response.getStatusCode() == HttpStatus.OK;
    }

    private void handleHttpException(String context, Exception e) {
        log.error("Error calling data-ingestion-service for {}", context, e);
        throw new ExternalApiException(ApiConstants.DATA_INGESTION_SERVICE_NAME + ": " + e.getMessage(), e);
    }
}

