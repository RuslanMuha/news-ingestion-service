package com.tispace.dataingestion.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Dedicated executor for the scheduled ingestion job so that timeout can
 * trigger cancellation and the task does not run on the common fork-join pool.
 */
@Configuration
@ConditionalOnProperty(name = "scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulerExecutorConfig {

	public static final String INGESTION_JOB_EXECUTOR_BEAN = "scheduledIngestionExecutor";

	@Bean(name = INGESTION_JOB_EXECUTOR_BEAN)
	public Executor scheduledIngestionExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setThreadNamePrefix("scheduled-ingestion-");
		executor.setCorePoolSize(1);
		executor.setMaxPoolSize(1);
		executor.setQueueCapacity(0);
		executor.setAllowCoreThreadTimeOut(true);
		executor.initialize();
		return executor;
	}
}
