package com.iiitb.spe.calculator.service;

import com.iiitb.spe.calculator.exception.DivisionByZeroException;
import org.springframework.stereotype.Service;

/**
 * Pure arithmetic core. Kept free of web concerns so it is unit-testable on its own.
 */
@Service
public class CalculatorService {

    public double add(double a, double b) {
        return a + b;
    }

    public double subtract(double a, double b) {
        return a - b;
    }

    public double multiply(double a, double b) {
        return a * b;
    }

    public double divide(double a, double b) {
        if (b == 0.0d) {
            throw new DivisionByZeroException();
        }
        return a / b;
    }
}
