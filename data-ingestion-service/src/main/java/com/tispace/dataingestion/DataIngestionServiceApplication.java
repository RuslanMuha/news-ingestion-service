package com.tispace.dataingestion;

import com.tispace.dataingestion.config.InternalSecurityProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.tispace.dataingestion", "com.tispace.common"})
@EntityScan(basePackages = "com.tispace.dataingestion.domain.entity")
@EnableJpaRepositories(basePackages = "com.tispace.dataingestion.infrastructure.repository")
@EnableConfigurationProperties(InternalSecurityProperties.class)
@EnableScheduling
public class DataIngestionServiceApplication {
	
	public static void main(String[] args) {
		SpringApplication.run(DataIngestionServiceApplication.class, args);
	}
}



