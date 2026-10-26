# TrustPay Guardian — Complete Project Structure & Codebase Map

**Document Version:** 1.0.0  
**Target Codebase:** TrustPay Guardian Repository Root  
**Last Updated:** 2026-10-26  

---

## 1. High-Level Repository Layout

```
TrustPay-Guardian/
├── .github/                   # GitHub Actions CI/CD workflows and issue templates
│   └── workflows/ci.yml       # Multi-job CI pipeline (Java 25 verify, Node 24 lint/test/build)
├── backend/                   # Spring Boot 4.1.1 Java 25 REST API application
│   ├── src/main/java/         # Application source code (modular architecture)
│   ├── src/main/resources/    # Configs (application.yml, Flyway migrations, logback)
│   ├── src/test/java/         # Comprehensive JUnit 5, Mockito & MockMvc tests
│   ├── Dockerfile             # Multi-stage Temurin JDK 25 -> JRE 25 Dockerfile
│   └── pom.xml                # Maven POM with Spring Boot, Flyway, Bucket4j, JaCoCo
├── frontend/                  # React 19 + TypeScript + Vite 6 Single Page Application
│   ├── e2e/                   # Playwright end-to-end smoke test specs
│   ├── src/api/               # In-memory JWT Axios client with silent refresh
│   ├── src/components/        # Layout, home sections, common modals, and UI widgets
│   ├── src/context/           # React Context providers (Auth, Theme, Toast)
│   ├── src/pages/             # Feature pages (Landing, Auth, Customer, Analyst, Admin)
│   ├── src/styles/            # Design tokens, Bootstrap 5.3 variables, and dark mode CSS
│   ├── src/__tests__/         # Vitest & React Testing Library test suites
│   ├── Dockerfile             # Multi-stage Node 24 -> Nginx Alpine Dockerfile
│   ├── nginx.conf             # Nginx reverse proxy with security headers & SPA routing
│   └── package.json           # Frontend dependencies, scripts, and dev tools
├── docs/                      # Complete 16-document architectural & technical documentation
├── scripts/                   # Developer automation, startup, and database reset scripts
├── compose.yaml               # Full-stack Docker Compose (MySQL 9.7, Backend, Frontend, MailHog)
├── compose.prod.yml           # Production Docker Compose specification (hardened secrets)
├── docker-compose.yml         # Backward-compatible alias for legacy CLI tools
├── Jenkinsfile                # Declarative 7-stage Jenkins enterprise CI/CD pipeline
└── README.md                  # Comprehensive root documentation and quick start guide
```

---

## 2. Backend Package Architecture (`backend/src/main/java/com/trustpay/guardian/`)

```
com.trustpay.guardian/
├── TrustPayGuardianApplication.java       # Spring Boot main entrypoint & scheduling enablement
│
├── account/                               # Customer Account & Statement Domain
│   ├── Account.java                       # JPA Entity (balance, status, optimistic version)
│   ├── AccountController.java             # REST Controller (/api/v1/accounts)
│   ├── AccountNumberGenerator.java        # Secure collision-resistant account number generator
│   ├── AccountRepository.java             # Spring Data JPA repository with row-locking queries
│   ├── AccountService.java                # Business service enforcing 3-account limit & IDOR guards
│   └── dto/                               # DTOs: AccountResponse, CreateAccountRequest, StatementResponse
│
├── admin/                                 # Staff & Executive Governance Domain
│   ├── AdminController.java               # REST Controller (/api/v1/admin/*)
│   ├── AdminService.java                  # Executive analytics, user governance, rule tuning
│   └── dto/                               # DTOs: AdminStatsResponse, UserManagementRequest, etc.
│
├── audit/                                 # Append-Only Immutable Compliance Audit
│   ├── AuditAction.java                   # Enum of auditable action types
│   ├── AuditLog.java                      # JPA Entity (immutable snapshot, before/after JSON)
│   ├── AuditLogRepository.java            # Filterable repository for compliance inspection
│   └── AuditService.java                  # Service logging actions with caller ID, IP, and Request ID
│
├── auth/                                  # Identity, Authentication & Password Domain
│   ├── AuthController.java                # REST Controller (/api/v1/auth/*)
│   ├── AuthService.java                   # BCrypt hashing, 5-strike lockout, refresh rotation
│   ├── RefreshToken.java                  # JPA Entity (hashed token, expiry, revoked status)
│   ├── RefreshTokenRepository.java        # Repository for token lookup and revocation
│   ├── User.java                          # JPA Entity (email, password hash, role, status)
│   ├── UserRepository.java                # Repository for user identity resolution
│   └── dto/                               # DTOs: LoginRequest, RegisterRequest, AuthResponse
│
├── beneficiary/                           # Beneficiary Whitelist & Verification Domain
│   ├── Beneficiary.java                   # JPA Entity (unique compound key: user_id + recipient)
│   ├── BeneficiaryController.java         # REST Controller (/api/v1/beneficiaries)
│   ├── BeneficiaryRepository.java         # Repository for recipient whitelisting
│   ├── BeneficiaryService.java            # Service for live lookup, self-add guard, duplicate guard
│   └── dto/                               # DTOs: BeneficiaryRequest, BeneficiaryResponse, LookupResponse
│
├── common/                                # Cross-Cutting Foundation Utilities
│   ├── exception/                         # Custom exceptions: BusinessRuleException, ConflictException, etc.
│   ├── format/                            # MaskingUtils (PII masking), MoneyUtils (Indian numbering)
│   ├── model/                             # PageResponse<T>, ApiResponse<T> standardized wrappers
│   └── web/                               # GlobalExceptionHandler (RFC 7807), RequestIdFilter (MDC)
│
├── config/                                # Spring Configuration Beans
│   ├── AppProperties.java                 # Strongly-typed @ConfigurationProperties
│   ├── CorsConfig.java                    # Explicit CORS configuration mapping
│   ├── OpenApiConfig.java                 # OpenAPI 3.0 Swagger specification bean
│   └── SchedulingConfig.java              # Background task scheduling enablement
│
├── fraud/                                 # Heuristic Fraud Risk Strategy Engine
│   ├── FraudAlert.java                    # JPA Entity (risk score, triage status, forensic notes)
│   ├── FraudAlertRepository.java          # Severity-sorted triage queue repository
│   ├── FraudAnalystController.java        # REST Controller (/api/v1/fraud/*)
│   ├── FraudAnalystService.java           # Case review, release, and confirm-block decisions
│   ├── FraudRuleConfig.java               # JPA Entity (rule enabled status, threshold, points)
│   ├── FraudRuleConfigRepository.java     # Repository for rule configurations
│   ├── FraudRuleConfigService.java        # In-memory cached provider of active rule parameters
│   ├── FraudScoringService.java           # Facade orchestrating engine evaluation
│   ├── engine/                            # Strategy Pattern implementation
│   │   ├── FraudEngine.java               # Clamped 0–100 scoring orchestrator
│   │   ├── FraudRule.java                 # Strategy interface (code, evaluate)
│   │   ├── RuleResult.java                # Record representing single rule hit
│   │   └── TransactionContext.java        # Context record passed into all rules
│   └── rules/                             # Isolated heuristic rule strategies
│       ├── R1_HighAmountRuleStrategy.java # Evaluates transfer amount vs threshold
│       ├── R2_RapidVelocityRuleStrategy.java # Evaluates transfer count in 5m window
│       ├── R3_NewBeneficiaryRuleStrategy.java # Detects first-time unverified recipients
│       ├── R4_DormantAccountRuleStrategy.java # Flags transfers on accounts idle >60d
│       ├── R5_HighBalanceDepletionRuleStrategy.java # Flags single debit draining >80%
│       ├── R6_RoundFigureRuleStrategy.java # Flags round sums (multiples of ₹10k)
│       └── R7_RecentFailedLoginsRuleStrategy.java # Flags accounts with >=3 recent failed logins
│
├── notification/                          # In-App & Email Notification Domain
│   ├── EmailService.java                  # JavaMailSender service sending simulated alert emails
│   ├── Notification.java                  # JPA Entity (user_id, category, message, read flag)
│   ├── NotificationController.java        # REST Controller (/api/v1/notifications)
│   ├── NotificationRepository.java        # Repository for unread counts and feeds
│   └── NotificationService.java           # Service dispatching sanitized security notices
│
├── security/                              # Spring Security & Cryptography Infrastructure
│   ├── AppUserDetails.java                # Spring Security UserDetails adapter
│   ├── AppUserDetailsService.java         # UserDetailsService resolving users from database
│   ├── CurrentUser.java                   # Custom injection annotation for authenticated user
│   ├── JwtAuthenticationFilter.java       # Filter extracting and validating bearer tokens
│   ├── JwtService.java                    # HMAC-SHA512 signing, verification, claim parsing
│   ├── RateLimitFilter.java               # Bucket4j token bucket rate limiting filter
│   ├── RestAccessDeniedHandler.java       # RFC 7807 403 Forbidden handler
│   ├── RestAuthenticationEntryPoint.java  # RFC 7807 401 Unauthorized handler
│   └── SecurityConfig.java                # SecurityFilterChain, CSRF, Session, CORS definition
│
└── transaction/                           # Money Movement, Ledger & Concurrency Domain
    ├── Transaction.java                   # JPA Entity (reference, amounts, status, idempotency)
    ├── TransactionController.java         # REST Controller (/api/v1/transactions/*)
    ├── TransactionReferenceGenerator.java # Secure random TXN-12 format generator
    ├── TransactionRepository.java         # Repository with pessimistic lock & hold calculations
    ├── TransferService.java               # Atomic transfer workflow, ascending locks, idempotency
    └── dto/                               # DTOs: TransferRequest, DepositRequest, TransactionResponse
```

---

## 3. Frontend Architecture (`frontend/src/`)

```
frontend/src/
├── api/
│   └── axiosClient.ts                     # In-memory JWT storage, 401 silent refresh, 429/5xx toasts
├── components/
│   ├── common/                            # Reusable widgets: ConfirmModal, EmptyState, Skeleton, ErrorBoundary
│   ├── home/                              # Landing page sections: Hero, Counters, Pipeline, FAQ, etc.
│   └── layout/                            # Shells: AppShell, PublicNavbar, Footer, NotificationDropdown
├── context/
│   ├── AuthContext.tsx                    # User state, silent refresh on mount, quick persona logins
│   ├── ThemeContext.tsx                   # Dark/light theme management, persistence, anti-flash
│   └── ToastContext.tsx                   # Centralized non-blocking toast notifications
├── hooks/
│   ├── useCountUp.ts                      # Animated numeric counter hook (supports reduced-motion)
│   └── useInView.ts                       # Intersection Observer hook for viewport detection
├── pages/
│   ├── LandingPage.tsx                    # Public animated home page (all 10 PRD sections)
│   ├── auth/                              # LoginPage.tsx, RegisterPage.tsx (Hook Form + Zod)
│   ├── customer/                          # CustomerDashboardPage, AccountsPage, TransferPage, etc.
│   ├── errors/                            # ForbiddenPage (/403), NotFoundPage (/404)
│   ├── AnalystDashboard.tsx               # Fraud Analyst Console (KPIs, triage queue, forensics)
│   └── AdminDashboard.tsx                 # Administrator Console (analytics, users, rules, audit)
├── routes/
│   ├── AppRoutes.tsx                      # Lazy-loaded route groups
│   ├── ProtectedRoute.tsx                 # Authentication barrier redirecting to /login
│   └── RoleRoute.tsx                      # Role-based barrier redirecting to /403
├── store/
│   ├── index.ts                           # Redux Toolkit store configuration
│   └── slices/notificationSlice.ts        # Polling unread count every 30 seconds
├── styles/
│   ├── variables.css                      # Design tokens (colors, font families, shadows)
│   ├── theme.css                          # Bootstrap 5.3 data-bs-theme overrides
│   └── custom.css                         # Animations, gauges, forensic meters, scrollbars
├── types/
│   └── index.ts                           # Full TypeScript interfaces for API requests/responses
└── utils/
    └── formatters.ts                      # formatCurrency (₹ INR), maskAccountNumber, maskHolderName
```
