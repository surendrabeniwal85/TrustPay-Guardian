# TrustPay Guardian — Entity-Relationship Diagram (ERD)

**System:** TrustPay Guardian: Fraud-Aware Digital Banking Platform  
**Target Engine:** MySQL Community Server 8.4.11 LTS  
**Schema Version:** Flyway V1–V6  
**Last Synchronized:** 2026-10-17  

---

## 1. Visual Entity-Relationship Diagram

```mermaid
erDiagram
    users ||--o{ accounts : "owns (1..3)"
    users ||--o{ beneficiaries : "manages"
    users ||--o{ refresh_tokens : "owns"
    users ||--o{ notifications : "receives"
    users ||--o{ audit_logs : "acts in (nullable)"
    users ||--o{ fraud_alerts : "assigned to (nullable)"
    users ||--o{ system_settings : "updated by (nullable)"
    accounts ||--o{ beneficiaries : "whitelisted as recipient"
    accounts ||--o{ transactions : "source account (nullable for deposit)"
    accounts ||--o{ transactions : "destination account"
    transactions ||--o| fraud_alerts : "flags (1..0/1)"
    fraud_rules ||--o{ fraud_alert_hits : "evaluated in"
    fraud_alerts ||--o{ fraud_alert_hits : "contains hit breakdown"

    users {
        BIGINT id PK
        VARCHAR(80) full_name
        VARCHAR(150) email UK
        VARCHAR(15) phone_number
        VARCHAR(100) password_hash
        VARCHAR(20) role
        VARCHAR(20) status
        INT failed_login_attempts
        DATETIME(6) lockout_until
        DATETIME(6) last_login_at
        DATETIME(6) created_at
        DATETIME(6) updated_at
    }

    accounts {
        BIGINT id PK
        BIGINT user_id FK
        VARCHAR(20) account_number UK
        VARCHAR(20) account_type
        VARCHAR(3) currency
        DECIMAL(19_2) balance
        VARCHAR(20) status
        DECIMAL(19_2) daily_limit
        BIGINT version
        DATETIME(6) created_at
        DATETIME(6) updated_at
    }

    beneficiaries {
        BIGINT id PK
        BIGINT user_id FK
        BIGINT recipient_account_id FK
        VARCHAR(50) nickname
        DATETIME(6) created_at
    }

    transactions {
        BIGINT id PK
        VARCHAR(20) reference_id UK
        VARCHAR(64) idempotency_key UK
        BIGINT source_account_id FK
        BIGINT destination_account_id FK
        DECIMAL(19_2) amount
        VARCHAR(3) currency
        VARCHAR(20) type
        VARCHAR(25) status
        INT risk_score
        VARCHAR(255) failure_reason
        DATETIME(6) created_at
        DATETIME(6) completed_at
    }

    fraud_rules {
        BIGINT id PK
        VARCHAR(30) rule_code UK
        VARCHAR(100) name
        VARCHAR(255) description
        INT points
        BOOLEAN is_enabled
        TEXT parameters_json
        DATETIME(6) updated_at
    }

    fraud_alerts {
        BIGINT id PK
        BIGINT transaction_id FK_UK
        INT risk_score
        VARCHAR(15) risk_level
        VARCHAR(25) status
        BIGINT assigned_to FK
        TEXT decision_notes
        DATETIME(6) created_at
        DATETIME(6) resolved_at
    }

    fraud_alert_hits {
        BIGINT id PK
        BIGINT alert_id FK
        VARCHAR(30) rule_code FK
        INT score_contribution
        VARCHAR(255) reason_explanation
        DATETIME(6) created_at
    }

    refresh_tokens {
        BIGINT id PK
        BIGINT user_id FK
        VARCHAR(64) token_hash UK
        DATETIME(6) expires_at
        DATETIME(6) revoked_at
        VARCHAR(64) replaced_by_hash
        DATETIME(6) created_at
    }

    notifications {
        BIGINT id PK
        BIGINT user_id FK
        VARCHAR(30) type
        VARCHAR(100) title
        TEXT message
        DATETIME(6) read_at
        DATETIME(6) created_at
    }

    audit_logs {
        BIGINT id PK
        BIGINT actor_user_id FK
        VARCHAR(50) action
        VARCHAR(30) target_type
        VARCHAR(64) target_id
        TEXT details_json
        VARCHAR(45) ip_address
        VARCHAR(64) request_id
        DATETIME(6) created_at
    }

    system_settings {
        BIGINT id PK
        VARCHAR(50) setting_key UK
        VARCHAR(100) setting_value
        VARCHAR(255) description
        DATETIME(6) updated_at
        BIGINT updated_by FK
    }
```

---

## 2. Relational Keys and Integrity Rules

| Table | Primary Key | Foreign Keys & Actions | Unique Constraints | Engine-Level CHECK Constraints |
| :--- | :--- | :--- | :--- | :--- |
| `users` | `id` | None | `uk_users_email (email)` | `role IN ('CUSTOMER', 'FRAUD_ANALYST', 'ADMIN')`<br>`status IN ('ACTIVE', 'SUSPENDED', 'DEACTIVATED')`<br>`failed_login_attempts >= 0` |
| `accounts` | `id` | `user_id -> users(id) ON DELETE RESTRICT` | `uk_accounts_number (account_number)` | `balance >= 0.00`<br>`daily_limit >= 0.00`<br>`currency = 'INR'`<br>`account_type IN ('SAVINGS', 'CURRENT')`<br>`status IN ('ACTIVE', 'FROZEN', 'CLOSED')` |
| `beneficiaries` | `id` | `user_id -> users(id) ON DELETE CASCADE`<br>`recipient_account_id -> accounts(id) ON DELETE RESTRICT` | `uk_beneficiaries_user_recipient (user_id, recipient_account_id)` | None (cross-table ownership validated in service layer) |
| `transactions` | `id` | `source_account_id -> accounts(id) ON DELETE RESTRICT`<br>`destination_account_id -> accounts(id) ON DELETE RESTRICT` | `uk_tx_reference (reference_id)`<br>`uk_tx_idempotency (idempotency_key)` | `amount > 0.00`<br>`currency = 'INR'`<br>`type IN ('TRANSFER', 'DEPOSIT')`<br>`status IN ('PENDING_REVIEW', 'APPROVED_SIMULATED', 'BLOCKED', 'REJECTED', 'CANCELLED')`<br>`risk_score IS NULL OR (risk_score >= 0 AND risk_score <= 100)`<br>`(type = 'TRANSFER' AND source_account_id IS NOT NULL) OR (type = 'DEPOSIT' AND source_account_id IS NULL)`<br>`source_account_id IS NULL OR source_account_id <> destination_account_id` |
| `fraud_rules` | `id` | None | `uk_fraud_rules_code (rule_code)` | `points >= 0 AND points <= 100` |
| `fraud_alerts` | `id` | `transaction_id -> transactions(id) ON DELETE RESTRICT`<br>`assigned_to -> users(id) ON DELETE SET NULL` | `uk_fraud_alerts_tx (transaction_id)` (1-to-1) | `risk_score >= 0 AND risk_score <= 100`<br>`risk_level IN ('LOW', 'MEDIUM', 'HIGH')`<br>`status IN ('OPEN', 'IN_REVIEW', 'RESOLVED_RELEASED', 'RESOLVED_BLOCKED')` |
| `fraud_alert_hits` | `id` | `alert_id -> fraud_alerts(id) ON DELETE CASCADE`<br>`rule_code -> fraud_rules(rule_code) ON DELETE RESTRICT ON UPDATE CASCADE` | None | `score_contribution >= 0` |
| `refresh_tokens` | `id` | `user_id -> users(id) ON DELETE CASCADE` | `uk_refresh_tokens_hash (token_hash)` | None |
| `notifications` | `id` | `user_id -> users(id) ON DELETE CASCADE` | None | None |
| `audit_logs` | `id` | `actor_user_id -> users(id) ON DELETE SET NULL` | None | None (immutability enforced via DB permissions & entity `@Column(updatable = false)`) |
| `system_settings` | `id` | `updated_by -> users(id) ON DELETE SET NULL` | `uk_system_settings_key (setting_key)` | None |
