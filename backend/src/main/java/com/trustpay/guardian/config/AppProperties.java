package com.trustpay.guardian.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String name = "TrustPay Guardian: Fraud-Aware Digital Banking Platform";
    private boolean simulationMode = true;
    private final Security security = new Security();
    private final Limits limits = new Limits();
    private final Seed seed = new Seed();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isSimulationMode() {
        return simulationMode;
    }

    public void setSimulationMode(boolean simulationMode) {
        this.simulationMode = simulationMode;
    }

    public Security getSecurity() {
        return security;
    }

    public Limits getLimits() {
        return limits;
    }

    public Seed getSeed() {
        return seed;
    }

    public static class Security {
        private final Jwt jwt = new Jwt();
        private final Cookie cookie = new Cookie();
        private final Cors cors = new Cors();

        public Jwt getJwt() {
            return jwt;
        }

        public Cookie getCookie() {
            return cookie;
        }

        public Cors getCors() {
            return cors;
        }

        public static class Jwt {
            private String secret = "TrustPayGuardianSecureJwtSecretKeyMustBeAtLeast32BytesLongForHS256Algorithm";
            private long expirationMs = 900000L; // 15 mins
            private long refreshExpirationMs = 604800000L; // 7 days

            public String getSecret() {
                return secret;
            }

            public void setSecret(String secret) {
                this.secret = secret;
            }

            public long getExpirationMs() {
                return expirationMs;
            }

            public void setExpirationMs(long expirationMs) {
                this.expirationMs = expirationMs;
            }

            public long getRefreshExpirationMs() {
                return refreshExpirationMs;
            }

            public void setRefreshExpirationMs(long refreshExpirationMs) {
                this.refreshExpirationMs = refreshExpirationMs;
            }
        }

        public static class Cookie {
            private boolean secure = false;
            private String sameSite = "Strict";

            public boolean isSecure() {
                return secure;
            }

            public void setSecure(boolean secure) {
                this.secure = secure;
            }

            public String getSameSite() {
                return sameSite;
            }

            public void setSameSite(String sameSite) {
                this.sameSite = sameSite;
            }
        }

        public static class Cors {
            private String allowedOrigin = "http://localhost:5173";

            public String getAllowedOrigin() {
                return allowedOrigin;
            }

            public void setAllowedOrigin(String allowedOrigin) {
                this.allowedOrigin = allowedOrigin;
            }
        }
    }

    public static class Limits {
        private BigDecimal maxTransferAmount = new BigDecimal("200000.00");
        private BigDecimal maxDepositAmount = new BigDecimal("50000.00");
        private BigDecimal defaultDailyLimit = new BigDecimal("100000.00");
        private BigDecimal demoOpeningBalance = new BigDecimal("10000.00");
        private int reviewThreshold = 40;
        private int blockThreshold = 70;

        public BigDecimal getMaxTransferAmount() {
            return maxTransferAmount;
        }

        public void setMaxTransferAmount(BigDecimal maxTransferAmount) {
            this.maxTransferAmount = maxTransferAmount;
        }

        public BigDecimal getMaxDepositAmount() {
            return maxDepositAmount;
        }

        public void setMaxDepositAmount(BigDecimal maxDepositAmount) {
            this.maxDepositAmount = maxDepositAmount;
        }

        public BigDecimal getDefaultDailyLimit() {
            return defaultDailyLimit;
        }

        public void setDefaultDailyLimit(BigDecimal defaultDailyLimit) {
            this.defaultDailyLimit = defaultDailyLimit;
        }

        public BigDecimal getDemoOpeningBalance() {
            return demoOpeningBalance;
        }

        public void setDemoOpeningBalance(BigDecimal demoOpeningBalance) {
            this.demoOpeningBalance = demoOpeningBalance;
        }

        public int getReviewThreshold() {
            return reviewThreshold;
        }

        public void setReviewThreshold(int reviewThreshold) {
            this.reviewThreshold = reviewThreshold;
        }

        public int getBlockThreshold() {
            return blockThreshold;
        }

        public void setBlockThreshold(int blockThreshold) {
            this.blockThreshold = blockThreshold;
        }
    }

    public static class Seed {
        private String password = "DemoSeedPassword123!";

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}
