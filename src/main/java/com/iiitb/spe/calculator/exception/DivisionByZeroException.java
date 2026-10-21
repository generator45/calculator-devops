package com.iiitb.spe.calculator.exception;

/**
 * Raised when a divide request carries b = 0. Mapped to HTTP 400.
 */
public class DivisionByZeroException extends RuntimeException {

    public DivisionByZeroException() {
        super("Division by zero is not allowed: parameter 'b' must be non-zero");
    }
}
