package com.iiitb.spe.calculator.exception;

import com.iiitb.spe.calculator.model.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseEntity.BodyBuilder;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Set;

/**
 * Turns every failure mode into the same structured JSON error body.
 *
 * <p>Note that the framework's own exceptions (unknown path, wrong HTTP method) are handled
 * explicitly. A {@code @RestControllerAdvice} is consulted before Spring's
 * {@code DefaultHandlerExceptionResolver}, so without these the catch-all below would turn
 * every 404/405 into a 500.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DivisionByZeroException.class)
    public ResponseEntity<ErrorResponse> handleDivisionByZero(DivisionByZeroException ex,
                                                              HttpServletRequest request) {
        return badRequest("Division by zero", ex.getMessage(), request);
    }

    @ExceptionHandler(NonFiniteValueException.class)
    public ResponseEntity<ErrorResponse> handleNonFiniteValue(NonFiniteValueException ex,
                                                              HttpServletRequest request) {
        return badRequest("Invalid parameter", ex.getMessage(), request);
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

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoResourceFoundException ex,
                                                        HttpServletRequest request) {
        return respond(HttpStatus.NOT_FOUND, "Not Found",
                "No endpoint %s %s".formatted(request.getMethod(), request.getRequestURI()), request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex,
                                                                HttpServletRequest request) {
        Set<HttpMethod> allowed = ex.getSupportedHttpMethods();
        String message = "Method %s is not supported for %s; supported methods are %s"
                .formatted(ex.getMethod(), request.getRequestURI(), allowed);
        ErrorResponse body = ErrorResponse.of(HttpStatus.METHOD_NOT_ALLOWED.value(),
                "Method Not Allowed", message, request.getRequestURI());
        log.warn("405 Method Not Allowed on {}: {}", request.getRequestURI(), message);

        // RFC 9110 §15.5.6 requires an Allow header on every 405.
        BodyBuilder response = ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED);
        if (allowed != null && !allowed.isEmpty()) {
            response.allow(allowed.toArray(HttpMethod[]::new));
        }
        return response.body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled error on {}", request.getRequestURI(), ex);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "An unexpected error occurred", request);
    }

    private ResponseEntity<ErrorResponse> badRequest(String error, String message, HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, error, message, request);
    }

    private ResponseEntity<ErrorResponse> respond(HttpStatus status, String error, String message,
                                                  HttpServletRequest request) {
        if (status.is4xxClientError()) {
            log.warn("{} {} on {}: {}", status.value(), error, request.getRequestURI(), message);
        }
        ErrorResponse body = ErrorResponse.of(status.value(), error, message, request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
