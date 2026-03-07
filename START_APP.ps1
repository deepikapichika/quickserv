# QuickServ Application Startup Script for PowerShell
# Run with: powershell -ExecutionPolicy Bypass -File START_APP.ps1

Write-Host "===============================================" -ForegroundColor Cyan
Write-Host "QuickServ Application Startup Script" -ForegroundColor Cyan
Write-Host "===============================================" -ForegroundColor Cyan
Write-Host ""

# Check if running as administrator
$isAdmin = ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)

if (-not $isAdmin) {
    Write-Host "[!] This script should be run as Administrator" -ForegroundColor Yellow
    Write-Host "Re-running with elevated privileges..." -ForegroundColor Yellow
    Start-Process powershell -Verb RunAs -ArgumentList "-ExecutionPolicy Bypass -File `"$PSCommandPath`""
    exit
}

# Check MySQL status
Write-Host "Checking MySQL status..." -ForegroundColor Yellow
$mysqlRunning = $false
$connectionTest = Test-NetConnection -ComputerName localhost -Port 3306 -InformationLevel Quiet -WarningAction SilentlyContinue
if ($connectionTest) {
    $mysqlRunning = $true
    Write-Host "[✓] MySQL is already running on port 3306" -ForegroundColor Green
} else {
    Write-Host "[!] MySQL is NOT running" -ForegroundColor Red
    Write-Host ""
    Write-Host "Attempting to start MySQL service..." -ForegroundColor Yellow

    # Try to start MySQL service
    try {
        Start-Service mysql80 -ErrorAction SilentlyContinue
        Start-Service mysql -ErrorAction SilentlyContinue
        Start-Sleep -Seconds 3

        # Check again
        $connectionTest = Test-NetConnection -ComputerName localhost -Port 3306 -InformationLevel Quiet -WarningAction SilentlyContinue
        if ($connectionTest) {
            Write-Host "[✓] MySQL started successfully" -ForegroundColor Green
        } else {
            Write-Host "[!] MySQL service could not be started" -ForegroundColor Red
            Write-Host ""
            Write-Host "Please try one of these alternatives:" -ForegroundColor Yellow
            Write-Host "1. Open XAMPP Control Panel and click 'Start' for MySQL" -ForegroundColor Cyan
            Write-Host "2. Or start MySQL from Services (services.msc)" -ForegroundColor Cyan
            Write-Host "3. Or open MySQL Workbench and click the play button" -ForegroundColor Cyan
            Write-Host ""
            Read-Host "Press Enter to exit"
            exit 1
        }
    } catch {
        Write-Host "[!] Error checking MySQL: $_" -ForegroundColor Red
    }
}

# Try to create database
Write-Host ""
Write-Host "Creating/verifying database..." -ForegroundColor Yellow
try {
    $mysqlPath = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
    if (-not (Test-Path $mysqlPath)) {
        $mysqlPath = "mysql.exe"  # Use system PATH
    }

    & $mysqlPath -u root -proot -e "CREATE DATABASE IF NOT EXISTS quickserv;" 2>$null
    Write-Host "[✓] Database quickserv is ready" -ForegroundColor Green
} catch {
    Write-Host "[!] Could not verify database (will try to create on startup)" -ForegroundColor Yellow
}

# Start the application
Write-Host ""
Write-Host "===============================================" -ForegroundColor Cyan
Write-Host "Starting QuickServ Application" -ForegroundColor Cyan
Write-Host "===============================================" -ForegroundColor Cyan
Write-Host ""

$projectPath = "C:\Users\MOHAN\OneDrive\Desktop\project-service\quickserv"
Set-Location -Path $projectPath

Write-Host "Project directory: $projectPath" -ForegroundColor Cyan
Write-Host ""
Write-Host "Running: mvn spring-boot:run" -ForegroundColor Cyan
Write-Host ""
Write-Host "⏳ Please wait while the application starts..." -ForegroundColor Yellow
Write-Host "   When you see 'Started QuickservApplication', the app is ready!" -ForegroundColor Yellow
Write-Host "   Then open: http://localhost:8080" -ForegroundColor Cyan
Write-Host ""
Write-Host "Starting in 2 seconds..." -ForegroundColor Gray
Start-Sleep -Seconds 2

# Run Maven
& mvn spring-boot:run

Write-Host ""
Read-Host "Press Enter to close this window"

