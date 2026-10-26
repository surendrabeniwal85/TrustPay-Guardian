package com.trustpay.guardian.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {

    private String accessToken;
    private String tokenType = "Bearer";
    private long expiresIn;
    private UserSummaryDto user;
    private String rawRefreshToken; // only passed to controller to set cookie, not serialized to body if ignored

    public AuthResponse() {
    }

    public AuthResponse(String accessToken, long expiresIn, UserSummaryDto user) {
        this.accessToken = accessToken;
        this.tokenType = "Bearer";
        this.expiresIn = expiresIn;
        this.user = user;
    }

    public AuthResponse(String accessToken, long expiresIn, UserSummaryDto user, String rawRefreshToken) {
        this(accessToken, expiresIn, user);
        this.rawRefreshToken = rawRefreshToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public UserSummaryDto getUser() {
        return user;
    }

    public void setUser(UserSummaryDto user) {
        this.user = user;
    }

    public String getRawRefreshToken() {
        return rawRefreshToken;
    }

    public String getRefreshToken() {
        return rawRefreshToken;
    }

    public void setRawRefreshToken(String rawRefreshToken) {
        this.rawRefreshToken = rawRefreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.rawRefreshToken = refreshToken;
    }
}
