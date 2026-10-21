package com.iiitb.spe.calculator.model;

/**
 * Successful calculation payload, e.g.
 * { "operation": "add", "a": 2.0, "b": 3.0, "result": 5.0 }
 */
public record CalculationResponse(String operation, double a, double b, double result) {
}
