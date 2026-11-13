@echo off
setlocal enabledelayedexpansion

echo ==================================================================
echo 🚆 RailStay IRCTC-Grade Database Setup & Importer
echo ==================================================================

REM Locate PostgreSQL bin directory
set "PG_BIN="
where psql >nul 2>nul
if %errorlevel% equ 0 (
    for /f "delims=" %%i in ('where psql') do (
        set "PG_BIN=%%~dpi"
        goto :found_pg
    )
)

REM Check common installation directories
for %%v in (18 17 16 15 14 13) do (
    if exist "C:\Program Files\PostgreSQL\%%v\bin\psql.exe" (
        set "PG_BIN=C:\Program Files\PostgreSQL\%%v\bin\"
        goto :found_pg
    )
)

:found_pg
if "%PG_BIN%"=="" (
    echo [ERROR] PostgreSQL installation was not found!
    echo Please make sure PostgreSQL is installed, or add its bin folder to PATH.
    echo Download from: https://www.postgresql.org/download/windows/
    pause
    exit /b 1
)

echo [*] Found PostgreSQL utilities at: %PG_BIN%

REM Port detection
set "PG_PORT=5432"
netstat -ano | findstr ":7777 " >nul 2>nul
if %errorlevel% equ 0 (
    set "PG_PORT=7777"
)

echo [*] Target PostgreSQL Port: %PG_PORT%
echo [*] Database Name: railstay_db

echo.
echo [*] Creating database railstay_db if it does not exist...
"%PG_BIN%createdb.exe" -p %PG_PORT% -U postgres railstay_db 2>nul
if %errorlevel% equ 0 (
    echo [+] Database 'railstay_db' created successfully.
) else (
    echo [*] Note: 'railstay_db' might already exist or password is required. Continuing to import...
)

if not exist "data\railstay_full_backup.sql" (
    echo [ERROR] Dump file 'data\railstay_full_backup.sql' was not found!
    pause
    exit /b 1
)

echo.
echo [*] Importing 14,346 Trains, 8,990 Stations & Halts (this takes ~10 seconds)...
"%PG_BIN%psql.exe" -p %PG_PORT% -U postgres -d railstay_db -f "data\railstay_full_backup.sql"

if %errorlevel% equ 0 (
    echo.
    echo ==================================================================
    echo [SUCCESS] Database railstay_db successfully loaded!
    echo All 14,346 trains and 8,990 stations are ready.
    echo You can now double-click 'run.bat' to start the application!
    echo ==================================================================
) else (
    echo.
    echo [!] Import encountered an issue. If PostgreSQL prompted for a password,
    echo     please verify your password or set it in config.properties.
)

pause
