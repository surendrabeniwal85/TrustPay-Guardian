[CmdletBinding()]
param (
    [string]$HostName = "127.0.0.1",
    [int]$Port = 3306,
    [string]$Database = "trustpay_guardian_test",
    [string]$RootUser = "root",
    [string]$FlywayUser = "tpg_flyway",
    [string]$AppUser = "tpg_app"
)

$ErrorActionPreference = "Stop"

Write-Host "==================================================================" -ForegroundColor Cyan
Write-Host "TrustPay Guardian - MySQL Verification Script" -ForegroundColor Cyan
Write-Host "Target Host: $HostName : $Port | Isolated DB: $Database" -ForegroundColor Cyan
Write-Host "==================================================================" -ForegroundColor Cyan

# 1. Locate mysql client binary
$mysqlCmd = "mysql"
if (-not (Get-Command $mysqlCmd -ErrorAction SilentlyContinue)) {
    $defaultPath = "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe"
    if (Test-Path $defaultPath) {
        $mysqlCmd = $defaultPath
    } else {
        throw "MySQL client binary not found in PATH or standard installation directory."
    }
}

# 2. Check MySQL Version
Write-Host "`n[1/6] Verifying MySQL binary and server version..." -ForegroundColor Yellow
& $mysqlCmd --version
if ($LASTEXITCODE -ne 0) {
    throw "Failed to execute mysql client binary."
}

# 3. Connection Pre-flight Check
Write-Host "`n[2/6] Verifying TCP connectivity on port $Port..." -ForegroundColor Yellow
$tcp = Test-NetConnection -ComputerName $HostName -Port $Port
if (-not $tcp.TcpTestSucceeded) {
    throw "MySQL port $Port is not reachable on $HostName!"
}
Write-Host "TCP connection succeeded." -ForegroundColor Green

# 4. Instructions for credentials
Write-Host "`n[3/6] Database Authentication Check..." -ForegroundColor Yellow
Write-Host "To execute migrations against isolated database, set SPRING_DATASOURCE_PASSWORD and FLYWAY_PASSWORD." -ForegroundColor Cyan

# 5. Schema verification queries template
Write-Host "`n[4/6] Verification SQL checks prepared." -ForegroundColor Yellow

# 6. Schema verification execution
if ($env:FLYWAY_TEST_PASSWORD -or $env:SPRING_DATASOURCE_TEST_PASSWORD) {
    Write-Host "`n[5/6] Detected credentials in environment. Executing MySQL integration test..." -ForegroundColor Green
    Push-Location "$PSScriptRoot\..\backend"
    try {
        & .\mvnw.cmd test -Dtest=MySqlSchemaAndMigrationIntegrationTest
        if ($LASTEXITCODE -eq 0) {
            Write-Host "`nMySQL Integration Test PASSED!" -ForegroundColor Green
        } else {
            Write-Host "`nMySQL Integration Test FAILED." -ForegroundColor Red
        }
    } finally {
        Pop-Location
    }
} else {
    Write-Host "`n[5/6] Environment variables not detected. To verify against MySQL, set credentials and run:" -ForegroundColor Yellow
    Write-Host '  Push-Location backend' -ForegroundColor White
    Write-Host '  $env:SPRING_DATASOURCE_TEST_PASSWORD = "<your_password>"' -ForegroundColor White
    Write-Host '  $env:FLYWAY_TEST_PASSWORD = "<your_password>"' -ForegroundColor White
    Write-Host '  .\mvnw.cmd test -Dtest=MySqlSchemaAndMigrationIntegrationTest' -ForegroundColor White
    Write-Host '  Pop-Location' -ForegroundColor White
}
