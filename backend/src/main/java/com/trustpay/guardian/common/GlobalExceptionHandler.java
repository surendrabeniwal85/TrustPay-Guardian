package com.trustpay.guardian.common;

import com.trustpay.guardian.common.exception.ApiException;
import com.trustpay.guardian.common.exception.BusinessRuleException;
import com.trustpay.guardian.common.exception.ConflictException;
import com.trustpay.guardian.common.exception.NotFoundException;
import com.trustpay.guardian.common.web.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private String resolveRequestId(HttpServletRequest request) {
        String requestId = (String) request.getAttribute(RequestIdFilter.MDC_KEY);
        if (requestId == null) {
            requestId = MDC.get(RequestIdFilter.MDC_KEY);
        }
        return requestId != null ? requestId : "N/A";
    }

    private ProblemDetail buildProblemDetail(HttpStatus status, String title, String detail, String instance, String requestId) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create("https://trustpay.internal/errors/" + status.value()));
        problem.setInstance(URI.create(instance));
        problem.setProperty("requestId", requestId);
        problem.setProperty("timestamp", Instant.now().toString());
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String requestId = resolveRequestId(request);
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        log.warn("Validation error [req: {}]: {}", requestId, fieldErrors);
        ProblemDetail problem = buildProblemDetail(HttpStatus.BAD_REQUEST, "Validation Error",
                "Validation failed for one or more fields", request.getRequestURI(), requestId);
        problem.setProperty("fieldErrors", fieldErrors);
        return problem;
    }

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail handleNotFoundException(NotFoundException ex, HttpServletRequest request) {
        String requestId = resolveRequestId(request);
        log.warn("Resource not found [req: {}]: {}", requestId, ex.getMessage());
        return buildProblemDetail(HttpStatus.NOT_FOUND, "Resource Not Found",
                ex.getMessage(), request.getRequestURI(), requestId);
    }

    @ExceptionHandler(ConflictException.class)
    public ProblemDetail handleConflictException(ConflictException ex, HttpServletRequest request) {
        String requestId = resolveRequestId(request);
        log.warn("Conflict detected [req: {}]: {}", requestId, ex.getMessage());
        return buildProblemDetail(HttpStatus.CONFLICT, "Conflict",
                ex.getMessage(), request.getRequestURI(), requestId);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ProblemDetail handleBusinessRuleException(BusinessRuleException ex, HttpServletRequest request) {
        String requestId = resolveRequestId(request);
        log.warn("Business rule violation [req: {}]: {}", requestId, ex.getMessage());
        return buildProblemDetail(HttpStatus.UNPROCESSABLE_ENTITY, "Business Rule Violation",
                ex.getMessage(), request.getRequestURI(), requestId);
    }

    @ExceptionHandler(ApiException.class)
    public ProblemDetail handleApiException(ApiException ex, HttpServletRequest request) {
        String requestId = resolveRequestId(request);
        log.warn("API Exception [req: {}] [{}]: {}", requestId, ex.getStatus(), ex.getMessage());
        return buildProblemDetail(ex.getStatus(), ex.getStatus().getReasonPhrase(),
                ex.getMessage(), request.getRequestURI(), requestId);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        String requestId = resolveRequestId(request);
        log.warn("Bad credentials [req: {}]", requestId);
        return buildProblemDetail(HttpStatus.UNAUTHORIZED, "Unauthorized",
                "Invalid email or password", request.getRequestURI(), requestId);
    }

    @ExceptionHandler(LockedException.class)
    public ProblemDetail handleLockedException(LockedException ex, HttpServletRequest request) {
        String requestId = resolveRequestId(request);
        log.warn("Account locked [req: {}]: {}", requestId, ex.getMessage());
        return buildProblemDetail(HttpStatus.LOCKED, "Account Locked",
                ex.getMessage(), request.getRequestURI(), requestId);
    }

    @ExceptionHandler(DisabledException.class)
    public ProblemDetail handleDisabledException(DisabledException ex, HttpServletRequest request) {
        String requestId = resolveRequestId(request);
        log.warn("Account disabled [req: {}]: {}", requestId, ex.getMessage());
        return buildProblemDetail(HttpStatus.FORBIDDEN, "Account Disabled",
                "Your account is inactive or disabled. Please contact support.", request.getRequestURI(), requestId);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        String requestId = resolveRequestId(request);
        log.warn("Access denied [req: {}]: {}", requestId, ex.getMessage());
        return buildProblemDetail(HttpStatus.FORBIDDEN, "Forbidden",
                "You do not have permission to access this resource", request.getRequestURI(), requestId);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleMalformedJson(HttpMessageNotReadableException ex, HttpServletRequest request) {
        String requestId = resolveRequestId(request);
        log.warn("Malformed JSON request [req: {}]: {}", requestId, ex.getMessage());
        return buildProblemDetail(HttpStatus.BAD_REQUEST, "Malformed JSON",
                "Malformed JSON request body", request.getRequestURI(), requestId);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex, HttpServletRequest request) {
        String requestId = resolveRequestId(request);
        log.error("Internal Server Error [req: {}]: {}", requestId, ex.getMessage(), ex);
        return buildProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "An unexpected internal error occurred. Please contact support with request ID: " + requestId,
                request.getRequestURI(), requestId);
    }
}
