package com.tispace.dataingestion.config;

import com.tispace.dataingestion.constants.ApiConstants;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(ClientHttpRequestFactory factory, RestTemplateBuilder builder) {
        return builder
                .requestFactory(() -> factory)
                .build();
    }
	
	@Bean
	public ClientHttpRequestFactory clientHttpRequestFactory() {
		PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
		connectionManager.setMaxTotal(ApiConstants.MAX_TOTAL_CONNECTIONS);
		connectionManager.setDefaultMaxPerRoute(ApiConstants.MAX_CONNECTIONS_PER_ROUTE);
		ConnectionConfig connectionConfig = ConnectionConfig.custom()
				.setConnectTimeout(Timeout.ofMilliseconds(ApiConstants.CONNECT_TIMEOUT_MS))
				.build();
		connectionManager.setDefaultConnectionConfig(connectionConfig);

		RequestConfig requestConfig = RequestConfig.custom()
				.setResponseTimeout(Timeout.ofMilliseconds(ApiConstants.READ_TIMEOUT_MS))
				.setConnectionRequestTimeout(Timeout.ofMilliseconds(ApiConstants.CONNECTION_REQUEST_TIMEOUT_MS))
				.build();

		CloseableHttpClient httpClient = HttpClients.custom()
				.setConnectionManager(connectionManager)
				.setDefaultRequestConfig(requestConfig)
				.evictExpiredConnections()
				.build();

		return new HttpComponentsClientHttpRequestFactory(httpClient);
	}
}

