package com.iiitb.spe.calculator.controller;

import com.iiitb.spe.calculator.model.CalculationResponse;
import com.iiitb.spe.calculator.service.CalculatorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1", produces = MediaType.APPLICATION_JSON_VALUE)
public class CalculatorController {

    private static final Logger log = LoggerFactory.getLogger(CalculatorController.class);

    private final CalculatorService calculator;

    public CalculatorController(CalculatorService calculator) {
        this.calculator = calculator;
    }

    @GetMapping("/add")
    public CalculationResponse add(@RequestParam double a, @RequestParam double b) {
        return respond("add", a, b, calculator.add(a, b));
    }

    @GetMapping("/subtract")
    public CalculationResponse subtract(@RequestParam double a, @RequestParam double b) {
        return respond("subtract", a, b, calculator.subtract(a, b));
    }

    @GetMapping("/multiply")
    public CalculationResponse multiply(@RequestParam double a, @RequestParam double b) {
        return respond("multiply", a, b, calculator.multiply(a, b));
    }

    @GetMapping("/divide")
    public CalculationResponse divide(@RequestParam double a, @RequestParam double b) {
        return respond("divide", a, b, calculator.divide(a, b));
    }

    private CalculationResponse respond(String operation, double a, double b, double result) {
        log.info("{}({}, {}) = {}", operation, a, b, result);
        return new CalculationResponse(operation, a, b, result);
    }
}
