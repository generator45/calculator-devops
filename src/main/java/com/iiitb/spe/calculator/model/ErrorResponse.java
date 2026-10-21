package com.iiitb.spe.calculator.model;

import java.time.Instant;

/**
 * Structured error body returned with every 4xx/5xx response.
 */
public record ErrorResponse(Instant timestamp, int status, String error, String message, String path) {

    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(Instant.now(), status, error, message, path);
    }
}
