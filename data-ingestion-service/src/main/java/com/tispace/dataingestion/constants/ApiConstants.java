package com.tispace.dataingestion.constants;

public final class ApiConstants {
	
	private ApiConstants() {
		// Utility class - prevent instantiation
	}
	
	// RestTemplate timeouts (in milliseconds)
	public static final int CONNECT_TIMEOUT_MS = 5000;
	public static final int READ_TIMEOUT_MS = 30000;
	public static final int CONNECTION_REQUEST_TIMEOUT_MS = 5000;
	public static final int MAX_TOTAL_CONNECTIONS = 100;
	public static final int MAX_CONNECTIONS_PER_ROUTE = 20;
}


