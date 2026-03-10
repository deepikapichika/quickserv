@echo off
REM QuickServ Startup Script for Windows
REM This script builds and runs QuickServ for Code Divas judges

echo.
echo ============================================
echo   QuickServ - Service Booking Platform
echo   BNY Mellon Code Divas
echo ============================================
echo.

REM Check if Java is installed
java -version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Java 17+ is not installed or not in PATH
    echo Please install Java JDK 17 or higher from: https://www.oracle.com/java/technologies/downloads/
    pause
    exit /b 1
)

echo [1] Checking Java version...
java -version

echo.
echo [2] Building QuickServ...
call mvnw.cmd clean package -DskipTests
if errorlevel 1 (
    echo ERROR: Build failed. Check MySQL connection in application.properties
    pause
    exit /b 1
)

echo.
echo [3] Starting QuickServ...
echo Waiting for startup...
timeout /t 5

echo.
echo ============================================
echo   QuickServ is starting...
echo   Open browser: http://localhost:8080
echo   Press CTRL+C to stop
echo ============================================
echo.

java -jar target\quickserv-0.0.1-SNAPSHOT.jar

pause

