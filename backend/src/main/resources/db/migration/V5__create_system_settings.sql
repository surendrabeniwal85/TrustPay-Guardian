-- V5: Create System Settings Table
-- Target Engine: MySQL Community Server 8.4.11 LTS

CREATE TABLE system_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    setting_key VARCHAR(50) NOT NULL,
    setting_value VARCHAR(100) NOT NULL,
    description VARCHAR(255) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    updated_by BIGINT NULL,
    CONSTRAINT uk_system_settings_key UNIQUE (setting_key),
    CONSTRAINT fk_system_settings_user FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
