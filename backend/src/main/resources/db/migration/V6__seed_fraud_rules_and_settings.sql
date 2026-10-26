-- V6: Seed Fraud Rules and System Settings
-- Target Engine: MySQL Community Server 8.4.11 LTS
-- Idempotent seed statements: will not overwrite runtime-managed values or delete historical data

INSERT INTO system_settings (setting_key, setting_value, description, updated_at)
VALUES
  ('REVIEW_THRESHOLD', '40', 'Composite risk score threshold to hold transfer for review', CURRENT_TIMESTAMP(6)),
  ('BLOCK_THRESHOLD', '70', 'Composite risk score threshold to automatically block transfer', CURRENT_TIMESTAMP(6)),
  ('DEMO_OPENING_BALANCE', '10000.00', 'Initial demo opening balance for newly registered customer accounts', CURRENT_TIMESTAMP(6)),
  ('MAX_TRANSFER_AMOUNT', '200000.00', 'Maximum allowable single simulated transfer amount in INR', CURRENT_TIMESTAMP(6)),
  ('MAX_DEPOSIT_AMOUNT', '50000.00', 'Maximum allowable single simulated deposit amount in INR', CURRENT_TIMESTAMP(6)),
  ('DEFAULT_DAILY_LIMIT', '100000.00', 'Default daily outgoing transfer limit per account in INR', CURRENT_TIMESTAMP(6))
ON DUPLICATE KEY UPDATE
  description = VALUES(description);

INSERT INTO fraud_rules (rule_code, name, description, points, is_enabled, parameters_json, updated_at)
VALUES
  ('R1', 'HIGH_AMOUNT', 'Flags transactions with transfer amount >= threshold', 30, TRUE, '{"threshold_amount": 50000.00}', CURRENT_TIMESTAMP(6)),
  ('R2', 'VELOCITY', 'Flags rapid repeated transfers from same source account', 25, TRUE, '{"window_minutes": 10, "max_transfers": 4}', CURRENT_TIMESTAMP(6)),
  ('R3', 'UNUSUAL_HOUR', 'Flags transfers initiated between 00:00 and 04:59 Asia/Kolkata', 15, TRUE, '{"start_hour": 0, "end_hour": 4, "timezone": "Asia/Kolkata"}', CURRENT_TIMESTAMP(6)),
  ('R4', 'NEW_BENEFICIARY_LARGE', 'Flags high-value transfers to newly added beneficiaries (< 24 hrs)', 30, TRUE, '{"window_hours": 24, "threshold_amount": 20000.00}', CURRENT_TIMESTAMP(6)),
  ('R5', 'AMOUNT_SPIKE', 'Flags transfers exceeding 5x 30-day historical average', 20, TRUE, '{"multiplier": 5.0, "window_days": 30, "min_history_count": 3}', CURRENT_TIMESTAMP(6)),
  ('R6', 'DAILY_LIMIT_PROXIMITY', 'Flags transfers causing daily cumulative total to exceed 90% of daily limit', 10, TRUE, '{"proximity_ratio": 0.90}', CURRENT_TIMESTAMP(6)),
  ('R7', 'RECENT_FAILED_LOGINS', 'Flags transfers originating from users with >= 3 failed logins in last 30 mins', 15, TRUE, '{"window_minutes": 30, "min_failed_attempts": 3}', CURRENT_TIMESTAMP(6))
ON DUPLICATE KEY UPDATE
  description = VALUES(description);
