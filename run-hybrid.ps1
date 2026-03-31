<#
.SYNOPSIS
    Payment Gateway - Hybrid Mode Testing Script
.DESCRIPTION
    Test edge cases with mixed Docker/local deployments
#>

param(
    [ValidateSet("InfraOnly", "MixedDockerLocal", "NetworkPartition", "ServiceRestart", "DatabaseFailover", "KafkaLag", "All")]
    [string]$Scenario,
    [switch]$CleanUp
)

$ErrorActionPreference = "Stop"

function Write-Header($Message) {
    Write-Host ""
    Write-Host "=" * 60 -ForegroundColor Yellow
    Write-Host $Message -ForegroundColor Yellow
    Write-Host "=" * 60 -ForegroundColor Yellow
    Write-Host ""
}

function Write-Status($Message) {
    Write-Host "[$(Get-Date -Format 'HH:mm:ss')] $Message" -ForegroundColor Cyan
}

function Write-Success($Message) {
    Write-Host "[$(Get-Date -Format 'HH:mm:ss')] $Message" -ForegroundColor Green
}

function Write-Error($Message) {
    Write-Host "[$(Get-Date -Format 'HH:mm:ss')] $Message" -ForegroundColor Red
}

function Wait-ForHealth($Url, $Timeout = 60) {
    $elapsed = 0
    while ($elapsed -lt $Timeout) {
        try {
            $response = Invoke-WebRequest -Uri $Url -TimeoutSec 2 -UseBasicParsing 2>$null
            if ($response.StatusCode -eq 200) { return $true }
        } catch {}
        Start-Sleep -Seconds 2
        $elapsed += 2
    }
    return $false
}

# Scenario 1: Infrastructure Only (for local dev)
function Test-InfraOnly {
    Write-Header "Scenario: Infrastructure Only (for local development)"
    
    Write-Status "Starting infrastructure services..."
    docker compose --profile infra up -d
    
    Write-Status "Waiting for infrastructure..."
    Start-Sleep -Seconds 15
    
    Write-Host ""
    Write-Host "Infrastructure is running. Run services locally:" -ForegroundColor Yellow
    Write-Host "  .\run-dev.ps1 -Service auth" -ForegroundColor Gray
    Write-Host "  .\run-dev.ps1 -Service payment" -ForegroundColor Gray
    Write-Host "  .\run-dev.ps1 -Service gateway" -ForegroundColor Gray
    Write-Host ""
    Write-Host "Endpoints:" -ForegroundColor Yellow
    Write-Host "  PostgreSQL: localhost:5433" -ForegroundColor Gray
    Write-Host "  Redis: localhost:6379" -ForegroundColor Gray
    Write-Host "  Kafka: localhost:9092" -ForegroundColor Gray
}

# Scenario 2: Mixed Docker/Local Services
function Test-MixedDockerLocal {
    Write-Header "Scenario: Mixed Docker and Local Services"
    
    Write-Status "Starting infrastructure in Docker..."
    docker compose --profile infra up -d
    Start-Sleep -Seconds 15
    
    Write-Status "Starting some services in Docker..."
    docker compose up -d auth-service notification-service webhook-service
    Start-Sleep -Seconds 30
    
    Write-Host ""
    Write-Host "Docker services running:" -ForegroundColor Green
    Write-Host "  - Auth Service (http://localhost:8081)" -ForegroundColor Gray
    Write-Host "  - Notification Service (http://localhost:8084)" -ForegroundColor Gray
    Write-Host "  - Webhook Service (http://localhost:8085)" -ForegroundColor Gray
    
    Write-Host ""
    Write-Host "Run remaining services locally:" -ForegroundColor Yellow
    Write-Host "  Terminal 1: cd services/payment-service; mvn spring-boot:run -Dspring-boot.run.profiles=local" -ForegroundColor Gray
    Write-Host "  Terminal 2: cd services/order-service; mvn spring-boot:run -Dspring-boot.run.profiles=local" -ForegroundColor Gray
    Write-Host "  Terminal 3: cd services/api-gateway; mvn spring-boot:run -Dspring-boot.run.profiles=local" -ForegroundColor Gray
    
    Write-Host ""
    Write-Host "Note: Docker services use container names (e.g., http://auth-service:8081)" -ForegroundColor Yellow
    Write-Host "      Local services use localhost" -ForegroundColor Yellow
}

# Scenario 3: Network Partition Simulation
function Test-NetworkPartition {
    Write-Header "Scenario: Network Partition Simulation"
    
    Write-Status "Starting all services..."
    docker compose --profile services up -d
    Start-Sleep -Seconds 90
    
    Write-Status "Checking initial health..."
    docker compose ps
    
    Write-Status "Simulating network partition - disconnecting payment-service..."
    docker network disconnect fintech-network payment-service 2>$null || Write-Error "Could not disconnect - service may not exist"
    
    Write-Host ""
    Write-Host "Payment service is now isolated. Test these scenarios:" -ForegroundColor Yellow
    Write-Host "  1. Try to create an order (should fail gracefully)" -ForegroundColor Gray
    Write-Host "  2. Check order-service logs for retry attempts" -ForegroundColor Gray
    Write-Host "  3. Check API Gateway error handling" -ForegroundColor Gray
    
    Write-Host ""
    Write-Host "Press Enter to reconnect payment-service..." -ForegroundColor Yellow
    Read-Host
    
    Write-Status "Reconnecting payment-service..."
    docker network connect fintech-network payment-service 2>$null || Write-Error "Could not reconnect"
    
    Write-Status "Waiting for recovery..."
    Start-Sleep -Seconds 30
    
    Write-Status "Checking health after recovery..."
    docker compose ps
}

# Scenario 4: Service Restart and Recovery
function Test-ServiceRestart {
    Write-Header "Scenario: Service Restart and Recovery"
    
    Write-Status "Starting all services..."
    docker compose --profile services up -d
    Start-Sleep -Seconds 90
    
    Write-Status "Killing payment-service..."
    docker compose kill payment-service
    
    Write-Host ""
    Write-Host "Payment service is down. Test these scenarios:" -ForegroundColor Yellow
    Write-Host "  1. Try to access payment endpoints via API Gateway" -ForegroundColor Gray
    Write-Host "  2. Check order-service logs for retry behavior" -ForegroundColor Gray
    Write-Host "  3. Check circuit breaker activation" -ForegroundColor Gray
    
    Write-Host ""
    Write-Host "Press Enter to restart payment-service..." -ForegroundColor Yellow
    Read-Host
    
    Write-Status "Restarting payment-service..."
    docker compose start payment-service
    
    Write-Status "Waiting for service to be healthy..."
    if (Wait-ForHealth "http://localhost:8083/actuator/health" 60) {
        Write-Success "Payment service recovered!"
    } else {
        Write-Error "Payment service failed to recover"
    }
    
    docker compose ps
}

# Scenario 5: Database Failover
function Test-DatabaseFailover {
    Write-Header "Scenario: Database Failover"
    
    Write-Status "Starting infrastructure..."
    docker compose --profile infra up -d
    Start-Sleep -Seconds 15
    
    Write-Status "Starting services..."
    docker compose up -d auth-service payment-service
    Start-Sleep -Seconds 45
    
    Write-Status "Current status:"
    docker compose ps
    
    Write-Status "Stopping PostgreSQL..."
    docker compose stop postgres
    
    Write-Host ""
    Write-Host "PostgreSQL is down. Test these scenarios:" -ForegroundColor Yellow
    Write-Host "  1. Check service health endpoints" -ForegroundColor Gray
    Write-Host "  2. Try to make API calls (should fail gracefully)" -ForegroundColor Gray
    Write-Host "  3. Check service logs for connection errors" -ForegroundColor Gray
    
    Write-Host ""
    Write-Host "Press Enter to restart PostgreSQL..." -ForegroundColor Yellow
    Read-Host
    
    Write-Status "Restarting PostgreSQL..."
    docker compose start postgres
    
    Write-Status "Waiting for PostgreSQL to be ready..."
    Start-Sleep -Seconds 20
    
    Write-Status "Checking service recovery..."
    docker compose ps
}

# Scenario 6: Kafka Consumer Lag
function Test-KafkaLag {
    Write-Header "Scenario: Kafka Consumer Lag"
    
    Write-Status "Starting infrastructure and producers..."
    docker compose --profile infra up -d
    Start-Sleep -Seconds 15
    
    docker compose up -d auth-service payment-service
    Start-Sleep -Seconds 45
    
    Write-Status "Sending test events to Kafka..."
    # Events will be produced by the services
    
    Write-Host ""
    Write-Host "Services are producing events. Now start consumer:" -ForegroundColor Yellow
    Write-Host "  docker compose up -d notification-service" -ForegroundColor Gray
    Write-Host ""
    Write-Host "Check consumer lag with:" -ForegroundColor Yellow
    Write-Host "  docker compose exec kafka kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group notification-group" -ForegroundColor Gray
    
    Write-Host ""
    Write-Host "Press Enter to start notification-service..." -ForegroundColor Yellow
    Read-Host
    
    Write-Status "Starting notification-service..."
    docker compose up -d notification-service
    Start-Sleep -Seconds 30
    
    Write-Status "Checking notification logs..."
    docker compose logs notification-service --tail 50
}

# Main execution
switch ($Scenario) {
    "InfraOnly" { Test-InfraOnly }
    "MixedDockerLocal" { Test-MixedDockerLocal }
    "NetworkPartition" { Test-NetworkPartition }
    "ServiceRestart" { Test-ServiceRestart }
    "DatabaseFailover" { Test-DatabaseFailover }
    "KafkaLag" { Test-KafkaLag }
    "All" {
        Test-InfraOnly
        docker compose --profile infra down -v
        
        Test-MixedDockerLocal
        docker compose --profile infra down -v
        
        Test-NetworkPartition
        docker compose --profile services down -v
        
        Test-ServiceRestart
        docker compose --profile services down -v
        
        Test-DatabaseFailover
        docker compose --profile infra down -v
        
        Test-KafkaLag
        docker compose --profile services down -v
        
        Write-Success "All scenarios completed!"
    }
    default {
        Write-Host "Payment Gateway - Hybrid Mode Testing" -ForegroundColor Yellow
        Write-Host ""
        Write-Host "Available Scenarios:" -ForegroundColor Cyan
        Write-Host "  InfraOnly         - Start only infrastructure for local development"
        Write-Host "  MixedDockerLocal  - Mix of Docker and local services"
        Write-Host "  NetworkPartition  - Simulate network partition between services"
        Write-Host "  ServiceRestart    - Test service restart and recovery"
        Write-Host "  DatabaseFailover  - Test database failover behavior"
        Write-Host "  KafkaLag          - Test Kafka consumer lag scenarios"
        Write-Host "  All               - Run all scenarios sequentially"
        Write-Host ""
        Write-Host "Usage:" -ForegroundColor Cyan
        Write-Host "  .\run-hybrid.ps1 -Scenario NetworkPartition"
        Write-Host "  .\run-hybrid.ps1 -CleanUp"
    }
}

if ($CleanUp) {
    Write-Status "Cleaning up all Docker resources..."
    docker compose --profile services --profile infra --profile observability down -v
    docker system prune -f
    Write-Success "Cleanup complete!"
}
