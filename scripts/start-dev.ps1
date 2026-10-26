# ==============================================================================
# TrustPay Guardian — Local Development Environment Launcher (PowerShell)
# Starts MySQL and MailHog in Docker, then outputs quick start instructions
# ==============================================================================
$ErrorActionPreference = "Stop"

Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host " TrustPay Guardian: Fraud-Aware Digital Banking Platform " -ForegroundColor Cyan
Write-Host " Local Development Environment Launcher (PowerShell) " -ForegroundColor Cyan
Write-Host "======================================================================" -ForegroundColor Cyan

# 1. Check Docker status
if (Get-Command docker -ErrorAction SilentlyContinue) {
    Write-Host "[1/3] Starting container infrastructure (MySQL & MailHog)..." -ForegroundColor Yellow
    docker compose up -d mysql mailhog
    Write-Host "[✓] MySQL and MailHog containers are starting in the background." -ForegroundColor Green
} else {
    Write-Host "[!] Docker not found. Assuming local MySQL 8.4/9.7 (port 3306) and MailHog (port 1025) are running." -ForegroundColor DarkYellow
}

# 2. Check Java and Node runtimes
Write-Host "[2/3] Checking runtimes..." -ForegroundColor Yellow
if (Get-Command java -ErrorAction SilentlyContinue) {
    java -version
}
if (Get-Command node -ErrorAction SilentlyContinue) {
    Write-Host "Node version: $(node -v)" -ForegroundColor Green
}

Write-Host "[3/3] Ready for local development!" -ForegroundColor Green
Write-Host "----------------------------------------------------------------------" -ForegroundColor DarkGray
Write-Host " Options:"
Write-Host "   Full Docker Stack:   docker compose up --build" -ForegroundColor Cyan
Write-Host "   Backend Standalone:  cd backend; .\mvnw.cmd spring-boot:run" -ForegroundColor Cyan
Write-Host "   Frontend Standalone: cd frontend; npm run dev" -ForegroundColor Cyan
Write-Host "----------------------------------------------------------------------" -ForegroundColor DarkGray
Write-Host " Endpoints:"
Write-Host "   Frontend UI:   http://localhost:5173 (dev) or http://localhost (docker)" -ForegroundColor White
Write-Host "   Backend API:   http://localhost:8080/api/v1" -ForegroundColor White
Write-Host "   Swagger Docs:  http://localhost:8080/swagger-ui.html" -ForegroundColor White
Write-Host "   MailHog Inbox: http://localhost:8025" -ForegroundColor White
Write-Host "======================================================================" -ForegroundColor Cyan
