# ==============================================================================
# TrustPay Guardian — Local Development Startup Script (PowerShell)
# Launches Backend and Frontend in separate jobs or processes
# ==============================================================================

Write-Host "======================================================" -ForegroundColor Cyan
Write-Host "  TrustPay Guardian: Fraud-Aware Digital Banking Platform" -ForegroundColor Cyan
Write-Host "======================================================" -ForegroundColor Cyan

# 1. Check MySQL Connectivity
Write-Host "[1/3] Checking local MySQL status on port 3306..." -ForegroundColor Yellow
$mysqlCheck = Test-NetConnection -ComputerName 127.0.0.1 -Port 3306 -WarningAction SilentlyContinue
if ($mysqlCheck.TcpTestSucceeded) {
    Write-Host "  MySQL is active and listening on port 3306." -ForegroundColor Green
} else {
    Write-Host "  Warning: MySQL is not responding on port 3306. Ensure MySQL84 service is running." -ForegroundColor Red
}

# 2. Check Backend
Write-Host "[2/3] Preparing backend (Spring Boot)..." -ForegroundColor Yellow
Write-Host "  To run backend: cd backend; .\mvnw.cmd spring-boot:run" -ForegroundColor Gray

# 3. Check Frontend
Write-Host "[3/3] Preparing frontend (React + Vite)..." -ForegroundColor Yellow
Write-Host "  To run frontend: cd frontend; npm run dev" -ForegroundColor Gray

Write-Host "Ready for development!" -ForegroundColor Green
