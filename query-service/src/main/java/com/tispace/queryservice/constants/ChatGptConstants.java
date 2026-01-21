package com.tispace.queryservice.constants;

public final class ChatGptConstants {
	
	private ChatGptConstants() {
		// Utility class - prevent instantiation
	}
	
	// OpenAI API parameters
	public static final int MAX_TOKENS = 200;
	public static final double TEMPERATURE = 0.7;
	
	// Prompt template constants
	public static final String SYSTEM_ROLE_MESSAGE = "You are a helpful assistant that summarizes articles concisely.";
	public static final String PROMPT_PREFIX = "Please provide a concise summary (2-3 sentences) of the following article:\n\n";
	public static final String PROMPT_TITLE_PREFIX = "Title: ";
	public static final String PROMPT_DESCRIPTION_PREFIX = "Description: ";
	public static final String PROMPT_SUMMARY_SUFFIX = "\n\nSummary:";
}



