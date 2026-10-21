package com.iiitb.spe.calculator.service;

import com.iiitb.spe.calculator.exception.DivisionByZeroException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CalculatorServiceTest {

    private static final double TOLERANCE = 1e-9;

    private final CalculatorService service = new CalculatorService();

    @ParameterizedTest(name = "add({0}, {1}) = {2}")
    @CsvSource({
            "2, 3, 5",
            "-4, 1.5, -2.5",
            "0, 0, 0",
            "1e10, 1e10, 2e10"
    })
    void addsTwoNumbers(double a, double b, double expected) {
        assertThat(service.add(a, b)).isCloseTo(expected, org.assertj.core.data.Offset.offset(TOLERANCE));
    }

    @ParameterizedTest(name = "subtract({0}, {1}) = {2}")
    @CsvSource({
            "5, 3, 2",
            "3, 5, -2",
            "-2.5, -2.5, 0"
    })
    void subtractsTwoNumbers(double a, double b, double expected) {
        assertThat(service.subtract(a, b)).isCloseTo(expected, org.assertj.core.data.Offset.offset(TOLERANCE));
    }

    @ParameterizedTest(name = "multiply({0}, {1}) = {2}")
    @CsvSource({
            "4, 2.5, 10",
            "-3, 3, -9",
            "0, 12345, 0"
    })
    void multipliesTwoNumbers(double a, double b, double expected) {
        assertThat(service.multiply(a, b)).isCloseTo(expected, org.assertj.core.data.Offset.offset(TOLERANCE));
    }

    @ParameterizedTest(name = "divide({0}, {1}) = {2}")
    @CsvSource({
            "10, 4, 2.5",
            "-9, 3, -3",
            "7, 7, 1"
    })
    void dividesTwoNumbers(double a, double b, double expected) {
        assertThat(service.divide(a, b)).isCloseTo(expected, org.assertj.core.data.Offset.offset(TOLERANCE));
    }

    @Test
    @DisplayName("divide by zero raises DivisionByZeroException instead of returning Infinity")
    void rejectsDivisionByZero() {
        assertThatThrownBy(() -> service.divide(5, 0))
                .isInstanceOf(DivisionByZeroException.class)
                .hasMessageContaining("Division by zero");
    }

    @Test
    @DisplayName("0 / 0 is also rejected rather than producing NaN")
    void rejectsZeroDividedByZero() {
        assertThatThrownBy(() -> service.divide(0, 0))
                .isInstanceOf(DivisionByZeroException.class);
    }
}
