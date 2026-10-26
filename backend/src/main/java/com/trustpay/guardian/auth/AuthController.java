package com.trustpay.guardian.auth;

import com.trustpay.guardian.auth.dto.AuthResponse;
import com.trustpay.guardian.auth.dto.ChangePasswordRequest;
import com.trustpay.guardian.auth.dto.LoginRequest;
import com.trustpay.guardian.auth.dto.RegisterRequest;
import com.trustpay.guardian.auth.dto.UserSummaryDto;
import com.trustpay.guardian.common.ApiResponse;
import com.trustpay.guardian.config.AppProperties;
import com.trustpay.guardian.security.CurrentUser;
import com.trustpay.guardian.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Authentication and session lifecycle APIs")
public class AuthController {

    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

    private final AuthService authService;
    private final AppProperties appProperties;

    public AuthController(AuthService authService, AppProperties appProperties) {
        this.authService = authService;
        this.appProperties = appProperties;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new customer account")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        String ipAddress = servletRequest.getRemoteAddr();
        String requestId = servletRequest.getHeader("X-Request-ID");

        AuthResponse authResponse = authService.register(request, ipAddress, requestId);
        addRefreshTokenCookie(servletResponse, authResponse.getRawRefreshToken());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration successful", sanitizeResponse(authResponse)));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user credentials and receive access token")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        String ipAddress = servletRequest.getRemoteAddr();
        String requestId = servletRequest.getHeader("X-Request-ID");

        AuthResponse authResponse = authService.login(request, ipAddress, requestId);
        addRefreshTokenCookie(servletResponse, authResponse.getRawRefreshToken());

        return ResponseEntity.ok(ApiResponse.success("Login successful", sanitizeResponse(authResponse)));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotate refresh token and issue a fresh access token")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @CookieValue(name = REFRESH_TOKEN_COOKIE_NAME, required = false) String cookieRefreshToken,
            @RequestBody(required = false) AuthResponse bodyRequest,
            HttpServletResponse servletResponse) {

        String tokenToUse = cookieRefreshToken;
        if (tokenToUse == null && bodyRequest != null) {
            tokenToUse = bodyRequest.getRawRefreshToken();
        }

        AuthResponse authResponse = authService.refreshToken(tokenToUse);
        addRefreshTokenCookie(servletResponse, authResponse.getRawRefreshToken());

        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", sanitizeResponse(authResponse)));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke current refresh token session")
    public ResponseEntity<ApiResponse<String>> logout(
            @CookieValue(name = REFRESH_TOKEN_COOKIE_NAME, required = false) String cookieRefreshToken,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        String ipAddress = servletRequest.getRemoteAddr();
        String requestId = servletRequest.getHeader("X-Request-ID");

        authService.logout(cookieRefreshToken, ipAddress, requestId);
        clearRefreshTokenCookie(servletResponse);
        return ResponseEntity.ok(ApiResponse.message("Logged out successfully"));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user profile summary", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<UserSummaryDto>> getCurrentUser(
            @CurrentUser UserPrincipal currentUser) {
        UserSummaryDto userSummary = authService.getCurrentUserSummary(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(userSummary));
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change account password and invalidate active sessions", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @CurrentUser UserPrincipal currentUser,
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        String ipAddress = servletRequest.getRemoteAddr();
        String requestId = servletRequest.getHeader("X-Request-ID");

        authService.changePassword(currentUser.getId(), request, ipAddress, requestId);
        clearRefreshTokenCookie(servletResponse);

        return ResponseEntity.ok(ApiResponse.message("Password changed successfully. Please log in again with your new credentials."));
    }

    private AuthResponse sanitizeResponse(AuthResponse response) {
        // Access token in body; raw refresh token delivered exclusively via HttpOnly SameSite=Strict cookie
        return new AuthResponse(
                response.getAccessToken(),
                response.getExpiresIn(),
                response.getUser()
        );
    }

    private void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        if (refreshToken != null) {
            boolean isSecure = appProperties.getSecurity().getCookie().isSecure();
            ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, refreshToken)
                    .httpOnly(true)
                    .secure(isSecure)
                    .path("/api/v1/auth")
                    .maxAge(7 * 24 * 60 * 60) // 7 days
                    .sameSite("Strict")
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        }
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        boolean isSecure = appProperties.getSecurity().getCookie().isSecure();
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(isSecure)
                .path("/api/v1/auth")
                .maxAge(0)
                .sameSite("Strict")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
