@echo off
REM ============================================
REM PayFlow Development Runner
REM 
REM Usage:
REM   run.bat           - Run everything (infra + local services)
REM   run.bat infra     - Run only infrastructure (docker)
REM   run.bat local     - Run only local services
REM   run.bat stop      - Stop all services
REM ============================================

setlocal

set PROJECT_ROOT=%~dp0
cd /d %PROJECT_ROOT%

if "%1"=="stop" goto stop
if "%1"=="infra" goto infra
if "%1"=="local" goto local
if "%1"=="" goto all
goto usage

:usage
echo Usage:
echo   run.bat           - Run everything (infra + local services)
echo   run.bat infra     - Run only infrastructure (docker)
echo   run.bat local     - Run only local services  
echo   run.bat stop      - Stop all services
goto :eof

:stop
echo [STOP] Stopping all services...
echo [STOP] Stopping Docker infrastructure...
docker compose down --remove-orphans 2>nul
echo [STOP] Stopping local services (press Ctrl+C to stop)...
echo [STOP] Done!
goto :eof

:infra
echo [INFRA] Starting Docker infrastructure...
docker compose up -d
echo [INFRA] Waiting for services to be ready...
timeout /t 10 /nobreak >nul
docker compose ps
echo [INFRA] Infrastructure ready!
echo.
echo To start local services, run: run.bat local
goto :eof

:local
echo [LOCAL] Starting local services...
echo [LOCAL] Make sure Docker infra is running first: run.bat infra
echo.
echo Starting services...
echo.
echo [1/7] Auth Service (8081)...
start "PayFlow-Auth" cmd /k "cd /d %PROJECT_ROOT% && mvn spring-boot:run -pl services/auth-service -Dspring-boot.run.arguments=--server.port=8081"
timeout /t 5 /nobreak >nul

echo [2/7] API Gateway (8080)...
start "PayFlow-Gateway" cmd /k "cd /d %PROJECT_ROOT% && mvn spring-boot:run -pl services/api-gateway -Dspring-boot.run.arguments=--server.port=8080"
timeout /t 5 /nobreak >nul

echo [3/7] Payment Service (8083)...
start "PayFlow-Payment" cmd /k "cd /d %PROJECT_ROOT% && mvn spring-boot:run -pl services/payment-service -Dspring-boot.run.arguments=--server.port=8083"
timeout /t 5 /nobreak >nul

echo [4/7] Order Service (8084)...
start "PayFlow-Order" cmd /k "cd /d %PROJECT_ROOT% && mvn spring-boot:run -pl services/order-service -Dspring-boot.run.arguments=--server.port=8084"
timeout /t 5 /nobreak >nul

echo [5/7] Notification Service (8085)...
start "PayFlow-Notification" cmd /k "cd /d %PROJECT_ROOT% && mvn spring-boot:run -pl services/notification-service -Dspring-boot.run.arguments=--server.port=8085"
timeout /t 5 /nobreak >nul

echo [6/7] Analytics Service (8089)...
start "PayFlow-Analytics" cmd /k "cd /d %PROJECT_ROOT% && mvn spring-boot:run -pl services/analytics-service -Dspring-boot.run.arguments=--server.port=8089"
timeout /t 5 /nobreak >nul

echo [7/7] Simulator Service (8086)...
start "PayFlow-Simulator" cmd /k "cd /d %PROJECT_ROOT% && mvn spring-boot:run -pl services/simulator-service -Dspring-boot.run.arguments=--server.port=8086"
timeout /t 5 /nobreak >nul

echo.
echo [LOCAL] Starting Frontend (3000)...
start "PayFlow-Frontend" cmd /k "cd /d %PROJECT_ROOT%web\frontend && npm run dev"
echo.
echo [LOCAL] All services started!
echo.
echo ============================================
echo Services:
echo   - Auth Service:      http://localhost:8081
echo   - API Gateway:      http://localhost:8080
echo   - Payment Service:  http://localhost:8083
echo   - Order Service:    http://localhost:8084
echo   - Notification:     http://localhost:8085
echo   - Analytics:        http://localhost:8089
echo   - Simulator:        http://localhost:8086
echo   - Frontend:         http://localhost:3000
echo ============================================
echo.
echo NOTE: Make sure to run 'mvn install -pl libs/common' first to build common library!
goto :eof

:all
echo [ALL] Starting full project...
echo.
echo [ALL] Step 1: Starting Docker infrastructure...
docker compose up -d
echo [ALL] Waiting for infrastructure...
timeout /t 15 /nobreak >nul
docker compose ps
echo.
echo [ALL] Step 2: Starting local services...
call :local
echo.
echo [ALL] Project ready!
goto :eof

endlocal