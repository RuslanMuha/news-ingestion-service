package com.tispace.common.exception;

import com.tispace.common.dto.ErrorResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
	
	private static final String URI_PREFIX = "uri=";
	private static final String VALIDATION_FAILED_MESSAGE = "Validation failed";
	private static final String UNEXPECTED_ERROR_MESSAGE = "An unexpected error occurred";
	
	@ExceptionHandler(NotFoundException.class)
	public ResponseEntity<ErrorResponseDTO> handleNotFoundException(NotFoundException ex, WebRequest request) {
		log.error("Resource not found: {}", ex.getMessage());
		return buildErrorResponse("NOT_FOUND", ex.getMessage(), HttpStatus.NOT_FOUND, request);
	}
	
	@ExceptionHandler(ExternalApiException.class)
	public ResponseEntity<ErrorResponseDTO> handleExternalApiException(ExternalApiException ex, WebRequest request) {
		log.error("External API error: {}", ex.getMessage(), ex);
		return buildErrorResponse("EXTERNAL_API_ERROR", ex.getMessage(), HttpStatus.SERVICE_UNAVAILABLE, request);
	}
	
	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ErrorResponseDTO> handleBusinessException(BusinessException ex, WebRequest request) {
		log.error("Business error: {}", ex.getMessage());
		return buildErrorResponse("BUSINESS_ERROR", ex.getMessage(), HttpStatus.BAD_REQUEST, request);
	}
	
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponseDTO> handleValidationException(MethodArgumentNotValidException ex, WebRequest request) {
		log.error("Validation error: {}", ex.getMessage());
		String message = ex.getBindingResult().getFieldErrors().stream()
			.map(error -> error.getField() + ": " + error.getDefaultMessage())
			.reduce((a, b) -> a + ", " + b)
			.orElse(VALIDATION_FAILED_MESSAGE);
		
		return buildErrorResponse("VALIDATION_ERROR", message, HttpStatus.BAD_REQUEST, request);
	}
	
	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<?> handleNoResourceFoundException(NoResourceFoundException ex, WebRequest request) {
		String path = extractPath(request);
		
		// Ignore favicon.ico requests - they are automatically requested by browsers
		if (path.contains("favicon.ico")) {
			log.debug("Favicon not found: {}", path);
			return ResponseEntity.notFound().build();
		}
		
		log.warn("Resource not found: {}", path);
		return buildErrorResponse("NOT_FOUND", ex.getMessage(), HttpStatus.NOT_FOUND, request);
	}
	
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponseDTO> handleGenericException(Exception ex, WebRequest request) {
		log.error("Unexpected error: {}", ex.getMessage(), ex);
		return buildErrorResponse("INTERNAL_ERROR", UNEXPECTED_ERROR_MESSAGE, HttpStatus.INTERNAL_SERVER_ERROR, request);
	}
	
	private ResponseEntity<ErrorResponseDTO> buildErrorResponse(String errorCode, String message, 
	                                                             HttpStatus status, WebRequest request) {
		ErrorResponseDTO error = ErrorResponseDTO.builder()
			.errorCode(errorCode)
			.message(message)
			.timestamp(LocalDateTime.now())
			.path(extractPath(request))
			.build();
		return new ResponseEntity<>(error, status);
	}
	
	private String extractPath(WebRequest request) {
		return request.getDescription(false).replace(URI_PREFIX, "");
	}
}

