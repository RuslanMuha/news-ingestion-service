package com.tispace.queryservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication(
	scanBasePackages = {"com.tispace.queryservice", "com.tispace.common"},
	exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class}
)
@EnableCaching
public class QueryServiceApplication {
	
	public static void main(String[] args) {
		SpringApplication.run(QueryServiceApplication.class, args);
	}
}

