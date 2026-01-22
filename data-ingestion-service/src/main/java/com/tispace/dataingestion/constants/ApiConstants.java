package com.tispace.dataingestion.constants;

public final class ApiConstants {
	
	private ApiConstants() {
		// Utility class - prevent instantiation
	}
	
	// RestTemplate timeouts (in milliseconds)
	public static final int CONNECT_TIMEOUT_MS = 5000;
	public static final int READ_TIMEOUT_MS = 30000;
}


