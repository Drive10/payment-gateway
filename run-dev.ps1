<#
.SYNOPSIS
    Payment Gateway - Development Mode Runner
.DESCRIPTION
    Start infrastructure in Docker and run services locally for development
#>

param(
    [switch]$StartInfra,
    [switch]$StopInfra,
    [switch]$BuildAll,
    [string]$Service
)

$ErrorActionPreference = "Stop"

function Write-Status($Message) {
    Write-Host "[$(Get-Date -Format 'HH:mm:ss')] $Message" -ForegroundColor Cyan
}

function Write-Success($Message) {
    Write-Host "[$(Get-Date -Format 'HH:mm:ss')] $Message" -ForegroundColor Green
}

function Write-Error($Message) {
    Write-Host "[$(Get-Date -Format 'HH:mm:ss')] $Message" -ForegroundColor Red
}

# Start Infrastructure
if ($StartInfra) {
    Write-Status "Starting infrastructure services (PostgreSQL, Redis, Kafka)..."
    docker compose --profile infra up -d
    
    Write-Status "Waiting for infrastructure to be ready..."
    Start-Sleep -Seconds 15
    
    # Check health
    Write-Status "Checking PostgreSQL..."
    docker compose exec postgres pg_isready -U payment 2>$null && Write-Success "PostgreSQL is ready" || Write-Error "PostgreSQL not ready"
    
    Write-Status "Checking Redis..."
    docker compose exec redis redis-cli -a "${env:REDIS_PASSWORD}" ping 2>$null && Write-Success "Redis is ready" || Write-Error "Redis not ready"
    
    Write-Success "Infrastructure started successfully!"
    Write-Host ""
    Write-Host "Infrastructure Services:" -ForegroundColor Yellow
    Write-Host "  - PostgreSQL: localhost:5433" -ForegroundColor Gray
    Write-Host "  - Redis: localhost:6379" -ForegroundColor Gray
    Write-Host "  - Kafka: localhost:9092" -ForegroundColor Gray
}

# Stop Infrastructure
if ($StopInfra) {
    Write-Status "Stopping infrastructure services..."
    docker compose --profile infra down -v
    Write-Success "Infrastructure stopped!"
}

# Build All Services
if ($BuildAll) {
    Write-Status "Building all services..."
    mvn clean package -DskipTests
    Write-Success "All services built successfully!"
}

# Run Specific Service
if ($Service) {
    $servicePort = switch ($Service) {
        "config" { 8888 }
        "auth" { 8081 }
        "order" { 8082 }
        "payment" { 8083 }
        "notification" { 8084 }
        "webhook" { 8085 }
        "simulator" { 8086 }
        "settlement" { 8087 }
        "risk" { 8088 }
        "analytics" { 8089 }
        "merchant" { 8090 }
        "dispute" { 8091 }
        "gateway" { 8080 }
        default { 0 }
    }
    
    if ($servicePort -eq 0) {
        Write-Error "Unknown service: $Service"
        Write-Host "Available services: config, auth, order, payment, notification, webhook, simulator, settlement, risk, analytics, merchant, dispute, gateway"
        exit 1
    }
    
    Write-Status "Starting $Service service on port $servicePort..."
    Set-Location "services\$Service-service"
    mvn spring-boot:run -Dspring-boot.run.profiles=local
}

if (-not $StartInfra -and -not $StopInfra -and -not $BuildAll -and -not $Service) {
    Write-Host "Payment Gateway - Dev Mode Runner" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Usage:" -ForegroundColor Cyan
    Write-Host "  .\run-dev.ps1 -StartInfra          Start infrastructure in Docker"
    Write-Host "  .\run-dev.ps1 -StopInfra           Stop infrastructure"
    Write-Host "  .\run-dev.ps1 -BuildAll            Build all services"
    Write-Host "  .\run-dev.ps1 -Service <name>      Run a specific service"
    Write-Host ""
    Write-Host "Example workflow:" -ForegroundColor Cyan
    Write-Host "  1. .\run-dev.ps1 -StartInfra"
    Write-Host "  2. .\run-dev.ps1 -BuildAll"
    Write-Host "  3. .\run-dev.ps1 -Service auth     (in separate terminal)"
    Write-Host "  4. .\run-dev.ps1 -Service payment  (in separate terminal)"
    Write-Host "  5. .\run-dev.ps1 -Service gateway  (in separate terminal)"
}
