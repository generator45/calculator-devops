package com.iiitb.spe.calculator.exception;

/**
 * Raised when an operand is NaN/Infinity, or when an otherwise valid operation
 * overflows to a non-finite result. Jackson cannot encode those as JSON numbers
 * (it emits the string "Infinity"), which would silently change the type of the
 * {@code result} field, so they are rejected with HTTP 400 instead.
 */
public class NonFiniteValueException extends RuntimeException {

    public static NonFiniteValueException forOperand(String name, double value) {
        return new NonFiniteValueException(
                "Parameter '%s' must be a finite number but was '%s'".formatted(name, value));
    }

    public static NonFiniteValueException forResult(String operation, double a, double b) {
        return new NonFiniteValueException(
                "%s(%s, %s) overflows the range of a finite double".formatted(operation, a, b));
    }

    private NonFiniteValueException(String message) {
        super(message);
    }
}
