-- ==============================================================================
-- TrustPay Guardian — Least-Privilege Grant Correction & Immutability Triggers
-- Target Database: trustpay_guardian_test
-- Execute as MySQL root or administrative user (MySQL Community Server 8.4 LTS)
-- ==============================================================================

USE trustpay_guardian_test;

-- ------------------------------------------------------------------------------
-- 1. Enforce Table-Level Least-Privilege Grants on 'tpg_app'
-- Eliminates schema-wide UPDATE/DELETE in mysql.db that overrides table-level revokes
-- ------------------------------------------------------------------------------

-- Step 1A: Remove existing schema-wide privileges
REVOKE ALL PRIVILEGES ON trustpay_guardian_test.* FROM 'tpg_app'@'localhost';

-- Step 1B: Grant base read and insert across the schema
GRANT SELECT, INSERT ON trustpay_guardian_test.* TO 'tpg_app'@'localhost';

-- Step 1C: Grant UPDATE only on mutable tables
GRANT UPDATE ON trustpay_guardian_test.users TO 'tpg_app'@'localhost';
GRANT UPDATE ON trustpay_guardian_test.accounts TO 'tpg_app'@'localhost';
GRANT UPDATE ON trustpay_guardian_test.beneficiaries TO 'tpg_app'@'localhost';
GRANT UPDATE ON trustpay_guardian_test.transactions TO 'tpg_app'@'localhost';
GRANT UPDATE ON trustpay_guardian_test.fraud_rules TO 'tpg_app'@'localhost';
GRANT UPDATE ON trustpay_guardian_test.fraud_alerts TO 'tpg_app'@'localhost';
GRANT UPDATE ON trustpay_guardian_test.refresh_tokens TO 'tpg_app'@'localhost';
GRANT UPDATE ON trustpay_guardian_test.notifications TO 'tpg_app'@'localhost';
GRANT UPDATE ON trustpay_guardian_test.system_settings TO 'tpg_app'@'localhost';

-- Step 1D: Grant DELETE only where deletion is an intended business operation
GRANT DELETE ON trustpay_guardian_test.beneficiaries TO 'tpg_app'@'localhost';
GRANT DELETE ON trustpay_guardian_test.refresh_tokens TO 'tpg_app'@'localhost';

-- Verification: 'audit_logs' and 'fraud_alert_hits' have SELECT and INSERT only.
-- Absolutely NO UPDATE or DELETE grants exist on audit_logs for tpg_app.

FLUSH PRIVILEGES;

-- ------------------------------------------------------------------------------
-- 2. Storage Engine Defense-in-Depth: Immutable Triggers on 'audit_logs'
-- Enforces immutability at the InnoDB engine level regardless of grant elevation
-- ------------------------------------------------------------------------------

DELIMITER //

DROP TRIGGER IF EXISTS trg_audit_logs_prevent_update//
CREATE TRIGGER trg_audit_logs_prevent_update
BEFORE UPDATE ON audit_logs
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
    SET MESSAGE_TEXT = 'AuditLogImmutableException: UPDATE operations on audit_logs are forbidden by database security policy.';
END//

DROP TRIGGER IF EXISTS trg_audit_logs_prevent_delete//
CREATE TRIGGER trg_audit_logs_prevent_delete
BEFORE DELETE ON audit_logs
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
    SET MESSAGE_TEXT = 'AuditLogImmutableException: DELETE operations on audit_logs are forbidden by database security policy.';
END//

DELIMITER ;
