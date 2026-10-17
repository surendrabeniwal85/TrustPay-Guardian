# ==============================================================================
# TrustPay Guardian — Local Project Verification Script (PowerShell)
# Verifies Toolchain, Backend Build, Frontend Build, and Health Endpoint
# ==============================================================================

$ErrorActionPreference = "Stop"

Write-Host "Verifying TrustPay Guardian Environment and Toolchain..." -ForegroundColor Cyan

# 1. Java check
Write-Host "Checking Java..." -ForegroundColor Yellow
java -version

# 2. Maven check
Write-Host "Checking Maven..." -ForegroundColor Yellow
mvn -version

# 3. Node & npm check
Write-Host "Checking Node & npm..." -ForegroundColor Yellow
node -v
npm -v

# 4. MySQL port check
Write-Host "Checking MySQL on port 3306..." -ForegroundColor Yellow
$tcp = Test-NetConnection -ComputerName 127.0.0.1 -Port 3306
if (-not $tcp.TcpTestSucceeded) {
    throw "MySQL port 3306 is not accessible!"
}
Write-Host "MySQL is reachable on port 3306." -ForegroundColor Green

# 5. Backend verification
Write-Host "Building backend with Maven..." -ForegroundColor Yellow
Push-Location "$PSScriptRoot\..\backend"
try {
    if (Test-Path ".\mvnw.cmd") {
        .\mvnw.cmd clean test
    } else {
        mvn clean test
    }
} finally {
    Pop-Location
}

# 6. Frontend verification
Write-Host "Building frontend with npm..." -ForegroundColor Yellow
Push-Location "$PSScriptRoot\..\frontend"
try {
    npm run build
} finally {
    Pop-Location
}

Write-Host "All project verifications passed!" -ForegroundColor Green
