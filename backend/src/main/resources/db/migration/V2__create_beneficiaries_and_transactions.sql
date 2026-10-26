-- V2: Create Beneficiaries and Transactions Tables
-- Target Engine: MySQL Community Server 8.4.11 LTS

CREATE TABLE beneficiaries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    recipient_account_id BIGINT NOT NULL,
    nickname VARCHAR(50) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_beneficiaries_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_beneficiaries_recipient FOREIGN KEY (recipient_account_id) REFERENCES accounts(id) ON DELETE RESTRICT,
    CONSTRAINT uk_beneficiaries_user_recipient UNIQUE (user_id, recipient_account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_beneficiaries_user_id ON beneficiaries (user_id);
CREATE INDEX idx_beneficiaries_created_at ON beneficiaries (created_at);

CREATE TABLE transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reference_id VARCHAR(20) NOT NULL,
    idempotency_key VARCHAR(64) NOT NULL,
    source_account_id BIGINT NULL,
    destination_account_id BIGINT NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    type VARCHAR(20) NOT NULL,
    status VARCHAR(25) NOT NULL,
    risk_score INT NULL,
    failure_reason VARCHAR(255) NULL,
    created_at DATETIME(6) NOT NULL,
    completed_at DATETIME(6) NULL,
    CONSTRAINT fk_tx_source FOREIGN KEY (source_account_id) REFERENCES accounts(id) ON DELETE RESTRICT,
    CONSTRAINT fk_tx_destination FOREIGN KEY (destination_account_id) REFERENCES accounts(id) ON DELETE RESTRICT,
    CONSTRAINT uk_tx_reference UNIQUE (reference_id),
    CONSTRAINT uk_tx_idempotency UNIQUE (idempotency_key),
    CONSTRAINT chk_tx_amount_positive CHECK (amount > 0.00),
    CONSTRAINT chk_tx_type CHECK (type IN ('TRANSFER', 'DEPOSIT')),
    CONSTRAINT chk_tx_status CHECK (status IN ('PENDING_REVIEW', 'APPROVED_SIMULATED', 'BLOCKED', 'REJECTED', 'CANCELLED')),
    CONSTRAINT chk_tx_currency CHECK (currency = 'INR'),
    CONSTRAINT chk_tx_risk_score CHECK (risk_score IS NULL OR (risk_score >= 0 AND risk_score <= 100)),
    CONSTRAINT chk_tx_type_source CHECK ((type = 'TRANSFER' AND source_account_id IS NOT NULL) OR (type = 'DEPOSIT' AND source_account_id IS NULL)),
    CONSTRAINT chk_tx_distinct_accounts CHECK (source_account_id IS NULL OR source_account_id <> destination_account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_tx_source ON transactions (source_account_id);
CREATE INDEX idx_tx_destination ON transactions (destination_account_id);
CREATE INDEX idx_tx_status ON transactions (status);
CREATE INDEX idx_tx_created_at ON transactions (created_at);
CREATE INDEX idx_tx_source_created ON transactions (source_account_id, created_at);
