#!/usr/bin/env bash
# ==============================================================================
# TrustPay Guardian — Reset Development Database Script
# Drops and recreates the dev database, wiping all transient simulator data
# ==============================================================================
set -euo pipefail

DB_NAME="${MYSQL_DATABASE:-trustpay_guardian}"
DB_USER="${MYSQL_USER:-root}"
DB_PASS="${MYSQL_ROOT_PASSWORD:-rootpassword}"

echo "======================================================================"
echo " TrustPay Guardian: Reset Development Database"
echo " Target DB: ${DB_NAME}"
echo "======================================================================"

if command -v docker >/dev/null 2>&1 && docker ps --format '{{.Names}}' | grep -q 'trustpay-mysql'; then
    echo "Resetting database inside running Docker container 'trustpay-mysql'..."
    docker exec -i trustpay-mysql mysql -u"${DB_USER}" -p"${DB_PASS}" -e "
        DROP DATABASE IF EXISTS \`${DB_NAME}\`;
        CREATE DATABASE \`${DB_NAME}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
    "
    echo "[✓] Database ${DB_NAME} reset inside Docker."
else
    echo "Running local MySQL reset..."
    mysql -u"${DB_USER}" -p"${DB_PASS}" -e "
        DROP DATABASE IF EXISTS \`${DB_NAME}\`;
        CREATE DATABASE \`${DB_NAME}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
    " || {
        echo "[!] Local mysql command failed. Please verify MySQL service is running."
        exit 1
    }
    echo "[✓] Local database ${DB_NAME} reset successfully."
fi

echo "Start backend to automatically re-apply Flyway migrations and seed data:"
echo "  cd backend && ./mvnw spring-boot:run"
