# TrustPay Guardian — Known Issues, Limitations & Future Architectural Improvements

**Document Version:** 1.0.0  
**Classification:** Engineering Roadmap & Honest Disclosure  
**Last Updated:** 2026-10-26  

---

## 1. Current Architectural Limitations & Honest Disclosures

### 1.1 Single-Node In-Memory Caching & Rate Limiting
- **Current Behavior:** Bucket4j rate limiting (token buckets) and `FraudRuleConfigService` active rule parameters are cached in JVM heap memory (`ConcurrentHashMap`).
- **Limitation:** If the backend is scaled horizontally across multiple container replicas without sticky sessions, rate limits apply per container rather than globally across the cluster, and rule cache invalidation requires broadcasting across nodes.
- **Production Roadmap:** Introduce an external **Redis** instance using `bucket4j-redis` and Spring Cache Redis for distributed token buckets and pub/sub cache invalidation.

### 1.2 Polling-Based In-App Notifications
- **Current Behavior:** The frontend checks for new notifications and unread counts using a Redux Toolkit polling interval set to 30 seconds (`notificationSlice.ts`).
- **Limitation:** Increases baseline HTTP traffic and introduces an average 15-second latency before a customer sees that a held transfer has been released by an analyst.
- **Production Roadmap:** Migrate from polling to **Server-Sent Events (SSE)** or **WebSockets (STOMP over SockJS)** for instantaneous server-pushed notification delivery.

### 1.3 Local Testcontainers Host Dependency
- **Current Behavior:** The 5 live MySQL integration tests in `MySqlSchemaAndMigrationIntegrationTest` require a local Docker daemon running on the host machine.
- **Limitation:** On bare-metal or CI environments without Docker privileges, these 5 tests are automatically skipped, falling back to H2 for standard automated test runs.
- **Production Roadmap:** Provision a dedicated remote Testcontainers cloud runner or maintain an always-available CI MySQL test instance.

### 1.4 Two-Factor Authentication (2FA / MFA)
- **Current Behavior:** High-risk transfers trigger the Fraud Strategy Engine and are routed to the fraud analyst triage queue for manual review.
- **Limitation:** There is no self-service step-up authentication (such as SMS OTP, TOTP authenticator app, or FIDO2/WebAuthn hardware key) allowing a trusted customer to clear medium-risk holds immediately.
- **Production Roadmap:** Implement RFC 6238 TOTP (Google Authenticator / Authy) step-up verification for transactions scoring between 40 and 60.

---

## 2. Planned Future Enhancements

### 2.1 Fraud Engine Backtesting & Shadow Execution
- Allow administrators to run "what-if" simulations against historical transaction datasets before applying new rule points or thresholds to live traffic.
- Display projected false-positive and false-negative rates for rule parameter changes.

### 2.2 Machine Learning Anomaly Detection (Hybrid Model)
- While preserving the 100% explainable Strategy Pattern rules as a hard baseline, introduce a secondary unsupervised Isolation Forest or Autoencoder model running asynchronously in a sidecar container to detect novel, emerging fraud topologies.

### 2.3 Automated Multi-Currency Clearing (FX Simulation)
- Expand the current single-currency INR (₹) simulation to support multi-currency accounts (USD, EUR, GBP) with simulated real-time exchange rates and foreign transaction risk rules.
