package com.trustpay.guardian.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.security.Principal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory rate limiting filter implemented with Bucket4j.
 * 
 * <p><strong>Production Architecture Note (ADR-016):</strong>
 * These Bucket4j rate limits are stored in-memory per JVM application instance (10 login req/min/IP, 
 * 20 transfer req/min/user). In a clustered, multi-node production deployment behind a load balancer, 
 * a distributed state store (e.g., Redis via bucket4j-redis or Hazelcast) is required to enforce 
 * cluster-wide rate limits across all nodes.</p>
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final Map<String, Bucket> loginBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> transferBuckets = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private Bucket createLoginBucket() {
        Bandwidth limit = Bandwidth.classic(10, Refill.greedy(10, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    private Bucket createTransferBucket() {
        Bandwidth limit = Bandwidth.classic(20, Refill.greedy(20, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        String method = request.getMethod();

        // 1. Rate limit login attempts: 10 per minute per IP
        if ("POST".equalsIgnoreCase(method) && path.endsWith("/auth/login")) {
            String clientIp = resolveClientIp(request);
            Bucket bucket = loginBuckets.computeIfAbsent(clientIp, k -> createLoginBucket());

            if (!bucket.tryConsume(1)) {
                log.warn("Rate limit exceeded for login attempts from IP: {}", clientIp);
                sendRateLimitResponse(request, response, "Login rate limit exceeded. Maximum 10 attempts per minute allowed.");
                return;
            }
        }

        // 2. Rate limit transfers: 20 per minute per user (or IP if unauthenticated)
        if ("POST".equalsIgnoreCase(method) && path.contains("/transactions/transfer")) {
            Principal userPrincipal = request.getUserPrincipal();
            String userKey = (userPrincipal != null && userPrincipal.getName() != null)
                    ? userPrincipal.getName()
                    : resolveClientIp(request);

            Bucket bucket = transferBuckets.computeIfAbsent(userKey, k -> createTransferBucket());

            if (!bucket.tryConsume(1)) {
                log.warn("Rate limit exceeded for transfers from user: {}", userKey);
                sendRateLimitResponse(request, response, "Transfer rate limit exceeded. Maximum 20 transfers per minute allowed.");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private void sendRateLimitResponse(HttpServletRequest request,
                                       HttpServletResponse response,
                                       String detail) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", "60");
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

        String requestId = MDC.get("requestId");
        if (requestId == null || requestId.isBlank()) {
            requestId = request.getHeader("X-Request-Id");
        }
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, detail);
        problem.setTitle("Too Many Requests");
        problem.setType(URI.create("https://trustpay.guardian/errors/rate-limit-exceeded"));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("requestId", requestId);
        problem.setProperty("timestamp", Instant.now().toString());

        objectMapper.writeValue(response.getOutputStream(), problem);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }
}
