package com.tispace.dataingestion.web.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.tispace.common.contract.ErrorResponseDTO;
import com.tispace.common.exception.BusinessException;
import com.tispace.common.exception.CacheException;
import com.tispace.common.exception.ExternalApiException;
import com.tispace.common.exception.NotFoundException;
import com.tispace.common.exception.RateLimitExceededException;
import com.tispace.common.exception.SerializationException;
import com.tispace.common.util.SensitiveDataFilter;
import org.apache.commons.lang3.StringUtils;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@RestControllerAdvice(basePackages = "com.tispace.dataingestion.controller")
@Order(Ordered.LOWEST_PRECEDENCE)
@Hidden
@Slf4j
public class GlobalExceptionHandler {
	
	private static final String UNKNOWN_PATH = "unknown";
	private static final String FAVICON_PATH = "favicon.ico";
	private static final String SPRINGDOC_API_DOCS_PATH = "/v3/api-docs";
	private static final String SPRINGDOC_SWAGGER_UI_PATH = "/swagger-ui";
	private static final String SPRINGDOC_SWAGGER_UI_HTML_PATH = "/swagger-ui.html";
	
	private static final String VALIDATION_FAILED_MESSAGE = "Validation failed";
	private static final String UNEXPECTED_ERROR_MESSAGE = "An unexpected error occurred";
	private static final String INVALID_ARGUMENT_MESSAGE = "Invalid argument provided";
	private static final String EXTERNAL_API_ERROR_MESSAGE = "External API service error occurred";
	private static final String SERIALIZATION_ERROR_MESSAGE = "Failed to process data";
	
	private static final String ERROR_CODE_NOT_FOUND = "NOT_FOUND";
	private static final String ERROR_CODE_EXTERNAL_API_ERROR = "EXTERNAL_API_ERROR";
	private static final String ERROR_CODE_CONNECTION_ERROR = "CONNECTION_ERROR";
	private static final String ERROR_CODE_DATA_INTEGRITY_ERROR = "DATA_INTEGRITY_ERROR";
	private static final String ERROR_CODE_DUPLICATE_ENTRY = "DUPLICATE_ENTRY";
	private static final String ERROR_CODE_REFERENTIAL_INTEGRITY_ERROR = "REFERENTIAL_INTEGRITY_ERROR";
	private static final String ERROR_CODE_SERIALIZATION_ERROR = "SERIALIZATION_ERROR";
	private static final String ERROR_CODE_CACHE_UNAVAILABLE = "CACHE_UNAVAILABLE";
	private static final String ERROR_CODE_RATE_LIMIT_EXCEEDED = "RATE_LIMIT_EXCEEDED";
	private static final String ERROR_CODE_BUSINESS_ERROR = "BUSINESS_ERROR";
	private static final String ERROR_CODE_VALIDATION_ERROR = "VALIDATION_ERROR";
	private static final String ERROR_CODE_INVALID_ARGUMENT = "INVALID_ARGUMENT";
	private static final String ERROR_CODE_INTERNAL_ERROR = "INTERNAL_ERROR";
	private static final String ERROR_CODE_UNAUTHORIZED = "UNAUTHORIZED";
	private static final String ERROR_CODE_FORBIDDEN = "FORBIDDEN";
	private static final String ERROR_CODE_BAD_REQUEST = "BAD_REQUEST";
	private static final String ERROR_CODE_CONFLICT = "CONFLICT";
	
	private static final String PATTERN_DUPLICATE = "duplicate";
	private static final String PATTERN_UNIQUE_CONSTRAINT = "unique constraint";
	private static final String PATTERN_FOREIGN_KEY = "foreign key";
	private static final String PATTERN_REFERENTIAL_INTEGRITY = "referential integrity";
	
	private static final String PATTERN_TIMEOUT = "timeout";
	private static final String PATTERN_CONNECTION_REFUSED = "Connection refused";
	
	private static final int MAX_MESSAGE_LENGTH = 200;
	private static final String MESSAGE_TRUNCATION_SUFFIX = "...";
	
	@ExceptionHandler(NotFoundException.class)
	public ResponseEntity<ErrorResponseDTO> handleNotFoundException(NotFoundException ex, HttpServletRequest request) {
		String safeMessage = ex.getMessage() != null ? ex.getMessage() : "Resource not found";
		log.warn("Resource not found: {}", safeMessage);
		return buildErrorResponse(ERROR_CODE_NOT_FOUND, safeMessage, HttpStatus.NOT_FOUND, request);
	}
	
	@ExceptionHandler(ExternalApiException.class)
	public ResponseEntity<ErrorResponseDTO> handleExternalApiException(ExternalApiException ex, HttpServletRequest request) {
		String sanitizedMessage = sanitizeMessage(ex.getMessage());
		log.error("External API error: {}", sanitizedMessage);
		return buildErrorResponse(ERROR_CODE_EXTERNAL_API_ERROR, EXTERNAL_API_ERROR_MESSAGE, HttpStatus.SERVICE_UNAVAILABLE, request);
	}
	
	@ExceptionHandler(HttpClientErrorException.class)
	public ResponseEntity<ErrorResponseDTO> handleHttpClientErrorException(HttpClientErrorException ex, HttpServletRequest request) {
		HttpStatusCode statusCode = ex.getStatusCode();
		HttpStatus status = HttpStatus.resolve(statusCode.value());
		if (status == null) {
			status = HttpStatus.INTERNAL_SERVER_ERROR;
		}
		String errorCode = determineErrorCode(status);
		String message = determineHttpErrorMessage(status);
		
		String sanitizedMessage = sanitizeMessage(ex.getMessage());
		if (status == HttpStatus.NOT_FOUND) {
			log.warn("HTTP client error [{}]: {}", status.value(), sanitizedMessage);
		} else {
			log.error("HTTP client error [{}]: {}", status.value(), sanitizedMessage);
		}
		return buildErrorResponse(errorCode, message, status, request);
	}
	
	@ExceptionHandler(ResourceAccessException.class)
	public ResponseEntity<ErrorResponseDTO> handleResourceAccessException(ResourceAccessException ex, HttpServletRequest request) {
		String exceptionMessage = ex.getMessage();
		String message = "Connection timeout or network error occurred";
		
		if (exceptionMessage != null) {
			String lowerMessage = exceptionMessage.toLowerCase();
			if (lowerMessage.contains(PATTERN_TIMEOUT)) {
				message = "Request timeout. Please try again later.";
			} else if (exceptionMessage.contains(PATTERN_CONNECTION_REFUSED)) {
				message = "Service is currently unavailable. Please try again later.";
			}
		}
		
		String sanitizedMessage = sanitizeMessage(exceptionMessage);
		log.error("Resource access error: {}", sanitizedMessage);
		return buildErrorResponse(ERROR_CODE_CONNECTION_ERROR, message, HttpStatus.SERVICE_UNAVAILABLE, request);
	}
	
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ErrorResponseDTO> handleDataIntegrityViolationException(DataIntegrityViolationException ex, HttpServletRequest request) {
		String message = "Data integrity violation occurred";
		String errorCode = ERROR_CODE_DATA_INTEGRITY_ERROR;
		
		String exceptionMessage = ex.getMessage();
		if (exceptionMessage != null) {
			String lowerMessage = exceptionMessage.toLowerCase();
			if (lowerMessage.contains(PATTERN_DUPLICATE) || lowerMessage.contains(PATTERN_UNIQUE_CONSTRAINT)) {
				message = "Duplicate entry detected. The resource already exists.";
				errorCode = ERROR_CODE_DUPLICATE_ENTRY;
			} else if (lowerMessage.contains(PATTERN_FOREIGN_KEY) || lowerMessage.contains(PATTERN_REFERENTIAL_INTEGRITY)) {
				message = "Referential integrity violation. Related resource does not exist.";
				errorCode = ERROR_CODE_REFERENTIAL_INTEGRITY_ERROR;
			}
		}
		
		String sanitizedMessage = sanitizeMessage(exceptionMessage);
		log.error("Data integrity violation: {}", sanitizedMessage, ex);
		return buildErrorResponse(errorCode, message, HttpStatus.CONFLICT, request);
	}
	
	@ExceptionHandler({JsonProcessingException.class, SerializationException.class})
	public ResponseEntity<ErrorResponseDTO> handleSerializationException(Exception ex, HttpServletRequest request) {
		String sanitizedMessage = sanitizeMessage(ex.getMessage());
		log.error("Serialization error: {}", sanitizedMessage, ex);
		return buildErrorResponse(ERROR_CODE_SERIALIZATION_ERROR, SERIALIZATION_ERROR_MESSAGE, HttpStatus.INTERNAL_SERVER_ERROR, request);
	}
	
	@ExceptionHandler(CacheException.class)
	public ResponseEntity<ErrorResponseDTO> handleCacheException(CacheException ex, HttpServletRequest request) {
		String sanitizedMessage = sanitizeMessage(ex.getMessage());
		log.warn("Cache error: {}", sanitizedMessage);
		return buildErrorResponse(ERROR_CODE_CACHE_UNAVAILABLE, "Cache service is temporarily unavailable", HttpStatus.SERVICE_UNAVAILABLE, request);
	}
	
	@ExceptionHandler(RateLimitExceededException.class)
	public ResponseEntity<ErrorResponseDTO> handleRateLimitExceededException(RateLimitExceededException ex, HttpServletRequest request) {
		String safeMessage = ex.getMessage() != null ? ex.getMessage() : "Rate limit exceeded";
		log.warn("Rate limit exceeded: {}", safeMessage);
		return buildErrorResponse(ERROR_CODE_RATE_LIMIT_EXCEEDED, safeMessage, HttpStatus.TOO_MANY_REQUESTS, request);
	}
	
	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ErrorResponseDTO> handleBusinessException(BusinessException ex, HttpServletRequest request) {
		String safeMessage = ex.getMessage() != null ? ex.getMessage() : "Business error occurred";
		log.error("Business error: {}", safeMessage);
		return buildErrorResponse(ERROR_CODE_BUSINESS_ERROR, safeMessage, HttpStatus.BAD_REQUEST, request);
	}
	
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponseDTO> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
		String message = ex.getBindingResult().getFieldErrors().stream()
			.map(error -> String.format("%s: %s", error.getField(), error.getDefaultMessage()))
			.reduce((a, b) -> String.format("%s, %s", a, b))
			.orElse(VALIDATION_FAILED_MESSAGE);
		
		if (VALIDATION_FAILED_MESSAGE.equals(message)) {
			message = VALIDATION_FAILED_MESSAGE;
		}
		
		log.warn("Validation error: {}", message);
		return buildErrorResponse(ERROR_CODE_VALIDATION_ERROR, message, HttpStatus.BAD_REQUEST, request);
	}
	
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ErrorResponseDTO> handleIllegalArgumentException(IllegalArgumentException ex, HttpServletRequest request) {
		String safeMessage = ex.getMessage() != null ? ex.getMessage() : INVALID_ARGUMENT_MESSAGE;
		log.warn("Illegal argument: {}", safeMessage);
		return buildErrorResponse(ERROR_CODE_INVALID_ARGUMENT, safeMessage, HttpStatus.BAD_REQUEST, request);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponseDTO> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
		log.warn("Malformed request body: {}", sanitizeMessage(ex.getMessage()));
		return buildErrorResponse(ERROR_CODE_BAD_REQUEST, "Malformed JSON request body", HttpStatus.BAD_REQUEST, request);
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ErrorResponseDTO> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
		log.warn("Invalid request parameter type: {}", ex.getName());
		return buildErrorResponse(ERROR_CODE_BAD_REQUEST, "Invalid request parameter type", HttpStatus.BAD_REQUEST, request);
	}
	
	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<?> handleNoResourceFoundException(NoResourceFoundException ex, HttpServletRequest request) {
		String path = extractPath(request);
		
		if (path.contains(FAVICON_PATH)) {
			log.debug("Favicon not found: {}", path);
			return ResponseEntity.notFound().build();
		}
		
		if (isSpringDocPath(path)) {
			log.debug("SpringDoc path not found: {}", path);
			return ResponseEntity.notFound().build();
		}
		
		log.warn("Resource not found: {}", path);
		return buildErrorResponse(ERROR_CODE_NOT_FOUND, "Resource not found", HttpStatus.NOT_FOUND, request);
	}
	
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponseDTO> handleGenericException(Exception ex, HttpServletRequest request) {
		String sanitizedMessage = sanitizeMessage(ex.getMessage());
		log.error("Unexpected error: {}", sanitizedMessage != null ? sanitizedMessage : "Unknown error", ex);
		return buildErrorResponse(ERROR_CODE_INTERNAL_ERROR, UNEXPECTED_ERROR_MESSAGE, HttpStatus.INTERNAL_SERVER_ERROR, request);
	}
	
	private ResponseEntity<ErrorResponseDTO> buildErrorResponse(String errorCode, String message, 
	                                                             HttpStatus status, HttpServletRequest request) {
		// TODO: Consider migrating ErrorResponseDTO.timestamp to Instant for UTC-friendly timestamps
		ErrorResponseDTO error = ErrorResponseDTO.builder()
			.errorCode(errorCode)
			.message(message)
			.timestamp(LocalDateTime.now())
			.path(extractPath(request))
			.build();
		return new ResponseEntity<>(error, status);
	}
	
	private String extractPath(HttpServletRequest request) {
		if (request == null) {
			return UNKNOWN_PATH;
		}
		String requestURI = request.getRequestURI();
		return StringUtils.isNotEmpty(requestURI) ? requestURI : UNKNOWN_PATH;
	}
	
	private boolean isSpringDocPath(String path) {
		return path.contains(SPRINGDOC_API_DOCS_PATH) 
			|| path.contains(SPRINGDOC_SWAGGER_UI_PATH) 
			|| path.contains(SPRINGDOC_SWAGGER_UI_HTML_PATH);
	}
	
	private String truncateMessage(String message) {
		if (message == null) {
			return null;
		}
		if (message.length() > MAX_MESSAGE_LENGTH) {
			return message.substring(0, MAX_MESSAGE_LENGTH) + MESSAGE_TRUNCATION_SUFFIX;
		}
		return message;
	}
	
	private String determineErrorCode(HttpStatus status) {
		if (status == null) {
			return ERROR_CODE_INTERNAL_ERROR;
		}
		return switch (status) {
			case NOT_FOUND -> ERROR_CODE_NOT_FOUND;
			case UNAUTHORIZED -> ERROR_CODE_UNAUTHORIZED;
			case FORBIDDEN -> ERROR_CODE_FORBIDDEN;
			case BAD_REQUEST -> ERROR_CODE_BAD_REQUEST;
			case CONFLICT -> ERROR_CODE_CONFLICT;
			case TOO_MANY_REQUESTS -> ERROR_CODE_RATE_LIMIT_EXCEEDED;
			default -> ERROR_CODE_INTERNAL_ERROR;
		};
	}
	
	private String determineHttpErrorMessage(HttpStatus status) {
		if (status == null) {
			return "HTTP error occurred";
		}
		return switch (status) {
			case UNAUTHORIZED -> "Authentication failed. Please check your credentials.";
			case FORBIDDEN -> "Access forbidden. You don't have permission to access this resource.";
			case NOT_FOUND -> "Resource not found.";
			case TOO_MANY_REQUESTS -> "Rate limit exceeded. Please try again later.";
			case CONFLICT -> "Resource conflict. The resource already exists or is in use.";
			case BAD_REQUEST -> "Invalid request. Please check your input.";
			default -> "HTTP error occurred";
		};
	}
	
	private String sanitizeMessage(String message) {
		if (message == null) {
			return null;
		}
		String masked = SensitiveDataFilter.maskSensitiveData(message);
		return truncateMessage(masked);
	}
}
