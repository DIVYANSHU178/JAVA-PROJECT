@echo off
setlocal

echo ==================================================================
echo 🚆 Starting RailStay IRCTC-Grade Passenger Reservation Engine
echo ==================================================================

REM 1. Check Java JDK
where javac >nul 2>nul
if %errorlevel% neq 0 (
    echo [ERROR] JDK 'javac' compiler was not found in PATH!
    echo Please make sure Java JDK 17+ is installed and configured in PATH.
    pause
    exit /b 1
)

REM 2. Compile Java Source Files
echo [*] Compiling Java source files...
javac -cp ".;lib/postgresql-42.7.4.jar" *.java
if %errorlevel% neq 0 (
    echo [ERROR] Compilation failed!
    pause
    exit /b 1
)
echo [+] Compilation successful!

REM 3. Launch Browser in background
start "" http://localhost:8080

REM 4. Start Server
echo [*] Launching Reservation Server on http://localhost:8080 ...
java -cp ".;lib/postgresql-42.7.4.jar" ReservationServer 8080

pause
