#!/usr/bin/env bash
# ==============================================================================
# TrustPay Guardian — Local Development Environment Launcher
# Starts MySQL and MailHog in Docker, then runs migrations and instructions
# ==============================================================================
set -euo pipefail

echo "======================================================================"
echo " TrustPay Guardian: Fraud-Aware Digital Banking Platform "
echo " Local Development Environment Launcher "
echo "======================================================================"

# 1. Check Docker status
if command -v docker >/dev/null 2>&1; then
    echo "[1/3] Starting container infrastructure (MySQL & MailHog)..."
    docker compose up -d mysql mailhog
    echo "[✓] MySQL and MailHog containers are starting in the background."
else
    echo "[!] Docker not found. Assuming local MySQL (port 3306) and MailHog (port 1025) are active."
fi

# 2. Check Java & Node
echo "[2/3] Checking runtimes..."
java -version 2>&1 | head -n 1
node -v

echo "[3/3] Ready for local development!"
echo "----------------------------------------------------------------------"
echo " Options:"
echo "   Full Docker Stack: docker compose up --build"
echo "   Backend Standalone: cd backend && ./mvnw spring-boot:run"
echo "   Frontend Standalone: cd frontend && npm run dev"
echo "----------------------------------------------------------------------"
echo " URLs:"
echo "   Frontend UI:      http://localhost:5173 (dev) or http://localhost (docker)"
echo "   Backend API:      http://localhost:8080/api/v1"
echo "   Swagger Docs:     http://localhost:8080/swagger-ui.html"
echo "   MailHog Inbox:    http://localhost:8025"
echo "======================================================================"
