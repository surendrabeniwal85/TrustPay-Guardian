package com.trustpay.guardian.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setStatus(HttpStatus.UNAUTHORIZED.value());

        String requestId = MDC.get("requestId");
        if (requestId == null || requestId.isBlank()) {
            requestId = request.getHeader("X-Request-Id");
        }
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                "Full authentication is required to access this resource"
        );
        problem.setTitle("Unauthorized");
        problem.setType(URI.create("https://trustpay.guardian/errors/unauthorized"));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("requestId", requestId);
        problem.setProperty("timestamp", Instant.now().toString());

        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
