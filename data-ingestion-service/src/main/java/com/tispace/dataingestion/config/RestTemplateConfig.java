package com.tispace.dataingestion.config;

import com.tispace.dataingestion.constants.ApiConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Configuration for RestTemplate and thread pools (bulkhead pattern).
 * Provides isolation between different external services using separate thread pools.
 */
@Configuration
public class RestTemplateConfig {
	
	@Value("${bulkhead.newsapi.core-pool-size:5}")
	private int newsApiCorePoolSize;
	
	@Value("${bulkhead.newsapi.max-pool-size:10}")
	private int newsApiMaxPoolSize;
	
	@Value("${bulkhead.newsapi.queue-capacity:100}")
	private int newsApiQueueCapacity;
	
	@Value("${bulkhead.queryservice.core-pool-size:5}")
	private int queryServiceCorePoolSize;
	
	@Value("${bulkhead.queryservice.max-pool-size:10}")
	private int queryServiceMaxPoolSize;
	
	@Value("${bulkhead.queryservice.queue-capacity:100}")
	private int queryServiceQueueCapacity;
	
	@Bean
	@Primary
	public RestTemplate restTemplate() {
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.setRequestFactory(clientHttpRequestFactory());
		return restTemplate;
	}
	
	@Bean
	public ClientHttpRequestFactory clientHttpRequestFactory() {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(ApiConstants.CONNECT_TIMEOUT_MS);
		factory.setReadTimeout(ApiConstants.READ_TIMEOUT_MS);
		return factory;
	}
	
	/**
	 * Thread pool executor for NewsAPI calls.
	 * Provides isolation from other external service calls.
	 */
	@Bean(name = "newsApiExecutor")
	public Executor newsApiExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(newsApiCorePoolSize);
		executor.setMaxPoolSize(newsApiMaxPoolSize);
		executor.setQueueCapacity(newsApiQueueCapacity);
		executor.setThreadNamePrefix("newsapi-");
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
		executor.setWaitForTasksToCompleteOnShutdown(true);
		executor.setAwaitTerminationSeconds(60);
		executor.initialize();
		return executor;
	}
	
	/**
	 * Thread pool executor for QueryService calls.
	 * Provides isolation from other external service calls.
	 */
	@Bean(name = "queryServiceExecutor")
	public Executor queryServiceExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(queryServiceCorePoolSize);
		executor.setMaxPoolSize(queryServiceMaxPoolSize);
		executor.setQueueCapacity(queryServiceQueueCapacity);
		executor.setThreadNamePrefix("queryservice-");
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
		executor.setWaitForTasksToCompleteOnShutdown(true);
		executor.setAwaitTerminationSeconds(60);
		executor.initialize();
		return executor;
	}
}

