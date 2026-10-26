package com.trustpay.guardian.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
class GlobalExceptionHandlerMockMvcTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private com.trustpay.guardian.common.web.RequestIdFilter requestIdFilter;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilters(requestIdFilter)
                .build();
    }

    @Test
    @DisplayName("Validation failure returns RFC 7807 ProblemDetail with fieldErrors and requestId")
    void validationErrorReturnsProblemDetail() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .header("X-Request-Id", "test-req-12345"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("X-Request-Id", "test-req-12345"))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.title", is("Validation Error")))
                .andExpect(jsonPath("$.requestId", is("test-req-12345")))
                .andExpect(jsonPath("$.fieldErrors", notNullValue()))
                .andExpect(jsonPath("$.fieldErrors.email", notNullValue()))
                .andExpect(jsonPath("$.fieldErrors.password", notNullValue()))
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }

    @Test
    @DisplayName("Malformed JSON returns 400 ProblemDetail without stack trace")
    void malformedJsonReturnsProblemDetail() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": invalid}")
                        .header("X-Request-Id", "malformed-req-999"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("X-Request-Id", "malformed-req-999"))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.requestId", is("malformed-req-999")))
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }

    @Test
    @DisplayName("Swagger UI and OpenAPI endpoints respond cleanly")
    void swaggerUiIsAccessible() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi", notNullValue()))
                .andExpect(jsonPath("$.info.title", is("TrustPay Guardian API")));
    }
}
