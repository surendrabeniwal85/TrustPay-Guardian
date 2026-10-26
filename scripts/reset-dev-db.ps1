# ==============================================================================
# TrustPay Guardian — Reset Development Database Script (PowerShell)
# Drops and recreates the dev database, wiping all transient simulator data
# ==============================================================================
$ErrorActionPreference = "Stop"

$DB_NAME = if ($env:MYSQL_DATABASE) { $env:MYSQL_DATABASE } else { "trustpay_guardian" }
$DB_USER = if ($env:MYSQL_USER) { $env:MYSQL_USER } else { "root" }
$DB_PASS = if ($env:MYSQL_ROOT_PASSWORD) { $env:MYSQL_ROOT_PASSWORD } else { "rootpassword" }

Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host " TrustPay Guardian: Reset Development Database" -ForegroundColor Cyan
Write-Host " Target DB: $DB_NAME" -ForegroundColor Yellow
Write-Host "======================================================================" -ForegroundColor Cyan

$resetSql = "DROP DATABASE IF EXISTS `$DB_NAME; CREATE DATABASE `$DB_NAME CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

if (Get-Command docker -ErrorAction SilentlyContinue) {
    $containerRunning = docker ps --format '{{.Names}}' | Select-String "trustpay-mysql"
    if ($containerRunning) {
        Write-Host "Resetting database inside running Docker container 'trustpay-mysql'..." -ForegroundColor Yellow
        docker exec -i trustpay-mysql mysql -u"$DB_USER" -p"$DB_PASS" -e "DROP DATABASE IF EXISTS \`$DB_NAME\`; CREATE DATABASE \`$DB_NAME\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
        Write-Host "[✓] Database $DB_NAME reset successfully in Docker container." -ForegroundColor Green
        return
    }
}

if (Get-Command mysql -ErrorAction SilentlyContinue) {
    Write-Host "Resetting database via local MySQL CLI..." -ForegroundColor Yellow
    mysql -u"$DB_USER" -p"$DB_PASS" -e "DROP DATABASE IF EXISTS \`$DB_NAME\`; CREATE DATABASE \`$DB_NAME\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
    Write-Host "[✓] Database $DB_NAME reset successfully via local MySQL." -ForegroundColor Green
} else {
    Write-Host "[!] Neither Docker nor local MySQL CLI found in PATH." -ForegroundColor Red
}

Write-Host "Launch backend to re-run Flyway migrations and synthetic seeder:" -ForegroundColor White
Write-Host "  cd backend; .\mvnw.cmd spring-boot:run" -ForegroundColor Cyan
