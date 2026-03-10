# QuickServ Startup Script for PowerShell
# For BNY Mellon Code Divas Competition

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  QuickServ - Service Booking Platform" -ForegroundColor Cyan
Write-Host "  BNY Mellon Code Divas" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

# Check Java
Write-Host "[1] Checking Java installation..." -ForegroundColor Yellow
try {
    $javaVersion = java -version 2>&1
    Write-Host "✓ Java found: $($javaVersion[0])" -ForegroundColor Green
} catch {
    Write-Host "✗ ERROR: Java 17+ not found" -ForegroundColor Red
    Write-Host "Install from: https://www.oracle.com/java/technologies/downloads/" -ForegroundColor Yellow
    Read-Host "Press Enter to exit"
    exit 1
}

# Check MySQL
Write-Host "[2] Checking MySQL connection..." -ForegroundColor Yellow
Write-Host "⚠ Ensure MySQL is running on localhost:3306" -ForegroundColor Yellow
Write-Host "  Database: quickserv" -ForegroundColor Yellow
Write-Host ""

# Build
Write-Host "[3] Building QuickServ..." -ForegroundColor Yellow
& .\mvnw.cmd clean package -DskipTests
if ($LASTEXITCODE -ne 0) {
    Write-Host "✗ Build failed!" -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit 1
}

Write-Host "✓ Build successful" -ForegroundColor Green
Write-Host ""

# Start
Write-Host "[4] Starting application..." -ForegroundColor Yellow
Write-Host "Waiting for server startup..." -ForegroundColor Cyan
Start-Sleep -Seconds 3

Write-Host ""
Write-Host "============================================" -ForegroundColor Green
Write-Host "  ✓ QuickServ is running!" -ForegroundColor Green
Write-Host "" -ForegroundColor Green
Write-Host "  Open your browser and go to:" -ForegroundColor Green
Write-Host "  http://localhost:8080" -ForegroundColor Cyan
Write-Host "" -ForegroundColor Green
Write-Host "  Press CTRL+C to stop the server" -ForegroundColor Yellow
Write-Host "============================================" -ForegroundColor Green
Write-Host ""

& java -jar target\quickserv-0.0.1-SNAPSHOT.jar

