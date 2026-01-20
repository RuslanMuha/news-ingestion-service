package com.tispace.common.exception;

import java.util.UUID;

public class NotFoundException extends BusinessException {
	
	public NotFoundException(String message) {
		super(message);
	}
	
	public NotFoundException(String resource, UUID id) {
		super(String.format("%s with id %s not found", resource, id));
	}
}



