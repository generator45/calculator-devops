package com.iiitb.spe.calculator;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CalculatorApplicationTests {

    @Autowired
    private ApplicationContext context;

    @Test
    void contextLoadsWithControllerAndService() {
        assertThat(context.containsBean("calculatorController")).isTrue();
        assertThat(context.containsBean("calculatorService")).isTrue();
    }
}
