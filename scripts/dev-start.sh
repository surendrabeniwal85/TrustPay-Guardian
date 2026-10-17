#!/usr/bin/env bash
# ==============================================================================
# TrustPay Guardian — Local Development Startup Script (Bash)
# ==============================================================================
set -e

echo "=== TrustPay Guardian: Fraud-Aware Digital Banking Platform ==="
echo "Checking environment..."

java -version
mvn -version
node -v
npm -v

echo "Ready for development!"
echo "Backend: cd backend && ./mvnw spring-boot:run"
echo "Frontend: cd frontend && npm run dev"
