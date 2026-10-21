package com.iiitb.spe.calculator.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Spring MVC integration tests: full application context, requests driven through MockMvc.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CalculatorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void addReturnsJsonPayload() throws Exception {
        mockMvc.perform(get("/api/v1/add").param("a", "2").param("b", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operation").value("add"))
                .andExpect(jsonPath("$.a").value(2.0))
                .andExpect(jsonPath("$.b").value(3.0))
                .andExpect(jsonPath("$.result").value(5.0));
    }

    @Test
    void subtractReturnsJsonPayload() throws Exception {
        mockMvc.perform(get("/api/v1/subtract").param("a", "9").param("b", "4.5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operation").value("subtract"))
                .andExpect(jsonPath("$.result").value(4.5));
    }

    @Test
    void multiplyReturnsJsonPayload() throws Exception {
        mockMvc.perform(get("/api/v1/multiply").param("a", "6").param("b", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(42.0));
    }

    @Test
    void divideReturnsJsonPayload() throws Exception {
        mockMvc.perform(get("/api/v1/divide").param("a", "10").param("b", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(2.5));
    }

    @Test
    @DisplayName("GET /api/v1/divide?b=0 -> 400 with structured error body")
    void divideByZeroReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/divide").param("a", "10").param("b", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Division by zero"))
                .andExpect(jsonPath("$.path").value("/api/v1/divide"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void missingParameterReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/add").param("a", "2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Missing parameter"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("'b'")));
    }

    @Test
    void nonNumericParameterReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/multiply").param("a", "abc").param("b", "2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid parameter"));
    }

    @Test
    @DisplayName("Actuator health probe reports UP for Compose health checks")
    void actuatorHealthReportsUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
