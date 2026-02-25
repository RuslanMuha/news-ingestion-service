package com.tispace.dataingestion.web.exception;

import com.tispace.common.web.exception.AbstractGlobalExceptionHandler;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.tispace.dataingestion.controller")
@Order(Ordered.LOWEST_PRECEDENCE)
@Hidden
public class GlobalExceptionHandler extends AbstractGlobalExceptionHandler {
}
