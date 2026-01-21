package com.tispace.dataingestion.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Configuration
@Slf4j
public class QueryServiceRestTemplateConfig {

    @Bean
    public ClientHttpRequestInterceptor internalTokenInterceptor(
            @Value("${security.internal.header:X-Internal-Token}") String headerName,
            @Value("${query-service.internal-token:}") String token
    ) {
        if (!StringUtils.hasText(token)) {
            log.error("INTERNAL_API_TOKEN is not configured! Service-to-service calls to query-service will fail with 401/403.");
            throw new IllegalStateException("INTERNAL_API_TOKEN is required but not configured. Set query-service.internal-token property.");
        }
        
        return (request, body, execution) -> {
            request.getHeaders().set(headerName, token);
            return execution.execute(request, body);
        };
    }

    @Bean
    public RestTemplate queryServiceRestTemplate(
            RestTemplateBuilder builder,
            ClientHttpRequestInterceptor internalTokenInterceptor,
            ClientHttpRequestFactory factory
    ) {
        return builder.additionalInterceptors(internalTokenInterceptor)
                .requestFactory(() -> factory)
                .build();
    }
}
