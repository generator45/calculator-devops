package com.iiitb.spe.calculator.service;

import com.iiitb.spe.calculator.exception.DivisionByZeroException;
import com.iiitb.spe.calculator.exception.NonFiniteValueException;
import org.springframework.stereotype.Service;

import java.util.function.DoubleBinaryOperator;

/**
 * Pure arithmetic core. Kept free of web concerns so it is unit-testable on its own.
 *
 * <p>Every operation guarantees a finite {@code double} result: non-finite operands and
 * results that overflow are rejected rather than leaking {@code NaN}/{@code Infinity} into
 * the JSON contract.
 */
@Service
public class CalculatorService {

    public double add(double a, double b) {
        return apply("add", a, b, Double::sum);
    }

    public double subtract(double a, double b) {
        return apply("subtract", a, b, (x, y) -> x - y);
    }

    public double multiply(double a, double b) {
        return apply("multiply", a, b, (x, y) -> x * y);
    }

    public double divide(double a, double b) {
        if (b == 0.0d) {
            throw new DivisionByZeroException();
        }
        return apply("divide", a, b, (x, y) -> x / y);
    }

    private double apply(String operation, double a, double b, DoubleBinaryOperator op) {
        requireFinite("a", a);
        requireFinite("b", b);
        double result = op.applyAsDouble(a, b);
        if (!Double.isFinite(result)) {
            throw NonFiniteValueException.forResult(operation, a, b);
        }
        return result;
    }

    private void requireFinite(String name, double value) {
        if (!Double.isFinite(value)) {
            throw NonFiniteValueException.forOperand(name, value);
        }
    }
}
