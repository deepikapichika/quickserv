o@echo off
REM QuickServ Startup Script for Windows

echo ===============================================
echo QuickServ Application Startup Script
echo ===============================================
echo.

REM Check MySQL status
echo Checking MySQL status...
netstat -ano | findstr :3306 > nul
if %errorlevel% neq 0 (
    echo.
    echo [!] MySQL is NOT running
    echo.
    echo Starting MySQL service...
    net start mysql80
    if %errorlevel% equ 0 (
        echo [✓] MySQL started successfully
        timeout /t 3
    ) else (
        echo [!] Failed to start MySQL service
        echo.
        echo Try one of these alternatives:
        echo 1. Open XAMPP Control Panel and click "Start" for MySQL
        echo 2. Or run: net start mysql
        echo 3. Or check MySQL Workbench
        pause
        exit /b 1
    )
) else (
    echo [✓] MySQL is already running
)

echo.
echo Creating database...
mysql -u root -p root -e "CREATE DATABASE IF NOT EXISTS quickserv;" 2>nul
if %errorlevel% equ 0 (
    echo [✓] Database created/verified
) else (
    echo [!] Could not create database
    echo Continuing anyway...
)

echo.
echo ===============================================
echo Starting QuickServ Application
echo ===============================================
echo.
echo Navigating to project directory...
cd /d "C:\Users\MOHAN\OneDrive\Desktop\project-service\quickserv"

echo.
echo Running: mvn spring-boot:run
echo.
echo Please wait while the application starts...
echo When you see "Started QuickservApplication", the app is ready!
echo.
timeout /t 2

mvn spring-boot:run

pause

