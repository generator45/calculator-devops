package com.iiitb.spe.calculator.exception;

import com.iiitb.spe.calculator.model.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Turns every failure mode into the same structured JSON error body.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DivisionByZeroException.class)
    public ResponseEntity<ErrorResponse> handleDivisionByZero(DivisionByZeroException ex,
                                                              HttpServletRequest request) {
        return badRequest("Division by zero", ex.getMessage(), request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException ex,
                                                                HttpServletRequest request) {
        String message = "Required parameter '%s' of type %s is missing"
                .formatted(ex.getParameterName(), ex.getParameterType());
        return badRequest("Missing parameter", message, request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                            HttpServletRequest request) {
        String message = "Parameter '%s' must be a valid number but was '%s'"
                .formatted(ex.getName(), ex.getValue());
        return badRequest("Invalid parameter", message, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled error on {}", request.getRequestURI(), ex);
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "An unexpected error occurred",
                request.getRequestURI());
        return ResponseEntity.internalServerError().body(body);
    }

    private ResponseEntity<ErrorResponse> badRequest(String error, String message, HttpServletRequest request) {
        log.warn("400 {} on {}: {}", error, request.getRequestURI(), message);
        ErrorResponse body = ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), error, message, request.getRequestURI());
        return ResponseEntity.badRequest().body(body);
    }
}
