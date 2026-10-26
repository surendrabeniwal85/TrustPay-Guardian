-- V3: Create Fraud Rules, Alerts, and Alert Hits Tables
-- Target Engine: MySQL Community Server 8.4.11 LTS

CREATE TABLE fraud_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_code VARCHAR(30) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255) NOT NULL,
    points INT NOT NULL,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    parameters_json TEXT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_fraud_rules_code UNIQUE (rule_code),
    CONSTRAINT chk_fraud_rules_points CHECK (points >= 0 AND points <= 100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_fraud_rules_enabled ON fraud_rules (is_enabled);

CREATE TABLE fraud_alerts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_id BIGINT NOT NULL,
    risk_score INT NOT NULL,
    risk_level VARCHAR(15) NOT NULL,
    status VARCHAR(25) NOT NULL DEFAULT 'OPEN',
    assigned_to BIGINT NULL,
    decision_notes TEXT NULL,
    created_at DATETIME(6) NOT NULL,
    resolved_at DATETIME(6) NULL,
    CONSTRAINT fk_fraud_alerts_tx FOREIGN KEY (transaction_id) REFERENCES transactions(id) ON DELETE RESTRICT,
    CONSTRAINT fk_fraud_alerts_user FOREIGN KEY (assigned_to) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT uk_fraud_alerts_tx UNIQUE (transaction_id),
    CONSTRAINT chk_fraud_alerts_score CHECK (risk_score >= 0 AND risk_score <= 100),
    CONSTRAINT chk_fraud_alerts_level CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT chk_fraud_alerts_status CHECK (status IN ('OPEN', 'IN_REVIEW', 'RESOLVED_RELEASED', 'RESOLVED_BLOCKED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_fraud_alerts_status ON fraud_alerts (status);
CREATE INDEX idx_fraud_alerts_created_at ON fraud_alerts (created_at);
CREATE INDEX idx_fraud_alerts_assigned ON fraud_alerts (assigned_to);

CREATE TABLE fraud_alert_hits (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    alert_id BIGINT NOT NULL,
    rule_code VARCHAR(30) NOT NULL,
    score_contribution INT NOT NULL,
    reason_explanation VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_alert_hits_alert FOREIGN KEY (alert_id) REFERENCES fraud_alerts(id) ON DELETE CASCADE,
    CONSTRAINT fk_alert_hits_rule FOREIGN KEY (rule_code) REFERENCES fraud_rules(rule_code) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_alert_hits_score CHECK (score_contribution >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_alert_hits_alert_id ON fraud_alert_hits (alert_id);
CREATE INDEX idx_alert_hits_rule_code ON fraud_alert_hits (rule_code);
