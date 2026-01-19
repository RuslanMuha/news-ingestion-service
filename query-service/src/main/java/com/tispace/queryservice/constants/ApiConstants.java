package com.tispace.queryservice.constants;

public final class ApiConstants {
	
	private ApiConstants() {
		// Utility class - prevent instantiation
	}
	
	public static final String ARTICLES_API_PATH = "/api/articles";
	public static final String DATA_INGESTION_SERVICE_NAME = "Data Ingestion Service";
	
	// RestTemplate timeouts (in milliseconds)
	public static final int CONNECT_TIMEOUT_MS = 5000;
	public static final int READ_TIMEOUT_MS = 10000;
}


