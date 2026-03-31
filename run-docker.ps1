<#
.SYNOPSIS
    Payment Gateway - Docker Full Run Script
.DESCRIPTION
    Run all services in Docker containers for production-like testing
#>

param(
    [switch]$Up,
    [switch]$Down,
    [switch]$Logs,
    [switch]$Status,
    [switch]$WithObservability,
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

# Start All Services
if ($Up) {
    $profiles = "--profile", "services"
    if ($WithObservability) {
        $profiles += "--profile", "observability"
    }
    
    Write-Status "Building all service images..."
    docker compose build
    
    Write-Status "Starting all services..."
    docker compose @profiles up -d
    
    Write-Status "Waiting for services to be healthy (this may take 2-3 minutes)..."
    Start-Sleep -Seconds 90
    
    Write-Success "All services started!"
    Write-Host ""
    Write-Host "Service Endpoints:" -ForegroundColor Yellow
    Write-Host "  - API Gateway:    http://localhost:8080" -ForegroundColor Gray
    Write-Host "  - Auth Service:   http://localhost:8081" -ForegroundColor Gray
    Write-Host "  - Order Service:  http://localhost:8082" -ForegroundColor Gray
    Write-Host "  - Payment Svc:    http://localhost:8083" -ForegroundColor Gray
    Write-Host "  - Notification:   http://localhost:8084" -ForegroundColor Gray
    Write-Host "  - Webhook:        http://localhost:8085" -ForegroundColor Gray
    Write-Host "  - Simulator:      http://localhost:8086" -ForegroundColor Gray
    Write-Host "  - Settlement:     http://localhost:8087" -ForegroundColor Gray
    Write-Host "  - Risk:           http://localhost:8088" -ForegroundColor Gray
    Write-Host "  - Analytics:      http://localhost:8089" -ForegroundColor Gray
    Write-Host "  - Merchant:       http://localhost:8090" -ForegroundColor Gray
    Write-Host "  - Dispute:        http://localhost:8091" -ForegroundColor Gray
    
    if ($WithObservability) {
        Write-Host ""
        Write-Host "Observability Stack:" -ForegroundColor Yellow
        Write-Host "  - Grafana:    http://localhost:3002 (admin/admin)" -ForegroundColor Gray
        Write-Host "  - Prometheus: http://localhost:9090" -ForegroundColor Gray
        Write-Host "  - Jaeger:     http://localhost:16686" -ForegroundColor Gray
    }
}

# Stop All Services
if ($Down) {
    $profiles = "--profile", "services", "--profile", "observability"
    
    Write-Status "Stopping all services..."
    docker compose @profiles down -v
    Write-Success "All services stopped!"
}

# Show Logs
if ($Logs) {
    if ($Service) {
        docker compose logs -f $Service
    } else {
        docker compose logs -f
    }
}

# Show Status
if ($Status) {
    docker compose ps
    Write-Host ""
    Write-Status "Checking service health..."
    
    $services = @(
        @{Name="api-gateway"; Port=8080},
        @{Name="auth-service"; Port=8081},
        @{Name="order-service"; Port=8082},
        @{Name="payment-service"; Port=8083},
        @{Name="notification-service"; Port=8084},
        @{Name="webhook-service"; Port=8085},
        @{Name="simulator-service"; Port=8086},
        @{Name="settlement-service"; Port=8087},
        @{Name="risk-service"; Port=8088},
        @{Name="analytics-service"; Port=8089},
        @{Name="merchant-service"; Port=8090},
        @{Name="dispute-service"; Port=8091}
    )
    
    foreach ($svc in $services) {
        try {
            $response = Invoke-WebRequest -Uri "http://localhost:$($svc.Port)/actuator/health" -TimeoutSec 2 -UseBasicParsing 2>$null
            if ($response.StatusCode -eq 200) {
                Write-Host "  [UP]   $($svc.Name) - http://localhost:$($svc.Port)" -ForegroundColor Green
            }
        } catch {
            Write-Host "  [DOWN] $($svc.Name) - http://localhost:$($svc.Port)" -ForegroundColor Red
        }
    }
}

if (-not $Up -and -not $Down -and -not $Logs -and -not $Status) {
    Write-Host "Payment Gateway - Docker Full Run" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Usage:" -ForegroundColor Cyan
    Write-Host "  .\run-docker.ps1 -Up                          Start all services"
    Write-Host "  .\run-docker.ps1 -Up -WithObservability       Start with Grafana/Prometheus/Jaeger"
    Write-Host "  .\run-docker.ps1 -Down                        Stop all services"
    Write-Host "  .\run-docker.ps1 -Status                      Show service status"
    Write-Host "  .\run-docker.ps1 -Logs                        Show all logs"
    Write-Host "  .\run-docker.ps1 -Logs -Service <name>        Show logs for specific service"
}
