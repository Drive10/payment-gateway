<#
.SYNOPSIS
    Payment Gateway - E2E API Testing Script
.DESCRIPTION
    Run end-to-end tests against running services
#>

param(
    [string]$BaseUrl = "http://localhost:8080",
    [switch]$RunAll,
    [switch]$Auth,
    [switch]$Payments,
    [switch]$Orders,
    [switch]$Health
)

$ErrorActionPreference = "Continue"

function Write-Test($Name) {
    Write-Host "[TEST] $Name" -ForegroundColor Cyan
}

function Write-Pass($Message) {
    Write-Host "  [PASS] $Message" -ForegroundColor Green
}

function Write-Fail($Message) {
    Write-Host "  [FAIL] $Message" -ForegroundColor Red
}

# Test Health Endpoints
function Test-Health {
    Write-Host ""
    Write-Host "=" * 60 -ForegroundColor Yellow
    Write-Host "Testing Health Endpoints" -ForegroundColor Yellow
    Write-Host "=" * 60 -ForegroundColor Yellow
    
    $endpoints = @(
        @{Name="API Gateway"; Port=8080},
        @{Name="Auth Service"; Port=8081},
        @{Name="Order Service"; Port=8082},
        @{Name="Payment Service"; Port=8083},
        @{Name="Notification Service"; Port=8084},
        @{Name="Webhook Service"; Port=8085},
        @{Name="Simulator Service"; Port=8086},
        @{Name="Settlement Service"; Port=8087},
        @{Name="Risk Service"; Port=8088},
        @{Name="Analytics Service"; Port=8089},
        @{Name="Merchant Service"; Port=8090},
        @{Name="Dispute Service"; Port=8091}
    )
    
    foreach ($ep in $endpoints) {
        Write-Test "$($ep.Name) Health"
        try {
            $response = Invoke-RestMethod -Uri "http://localhost:$($ep.Port)/actuator/health" -TimeoutSec 5
            if ($response.status -eq "UP") {
                Write-Pass "Service is UP"
            } else {
                Write-Fail "Service status: $($response.status)"
            }
        } catch {
            Write-Fail "Service not reachable: $_"
        }
    }
}

# Test Auth Endpoints
function Test-Auth {
    Write-Host ""
    Write-Host "=" * 60 -ForegroundColor Yellow
    Write-Host "Testing Auth Endpoints" -ForegroundColor Yellow
    Write-Host "=" * 60 -ForegroundColor Yellow
    
    $timestamp = [int][double]::Parse((Get-Date -UFormat %s))
    $email = "test$timestamp@example.com"
    
    # Register
    Write-Test "User Registration"
    try {
        $body = @{
            email = $email
            password = "Test@123456"
            fullName = "Test User"
        } | ConvertTo-Json
        
        $response = Invoke-RestMethod -Uri "$BaseUrl/api/auth/register" -Method Post -Body $body -ContentType "application/json"
        Write-Pass "User registered: $($response.email)"
    } catch {
        Write-Fail "Registration failed: $_"
    }
    
    # Login
    Write-Test "User Login"
    try {
        $body = @{
            email = $email
            password = "Test@123456"
        } | ConvertTo-Json
        
        $response = Invoke-RestMethod -Uri "$BaseUrl/api/auth/login" -Method Post -Body $body -ContentType "application/json"
        $script:AuthToken = $response.accessToken
        Write-Pass "Login successful, token obtained"
    } catch {
        Write-Fail "Login failed: $_"
    }
    
    # Get Profile
    Write-Test "Get User Profile"
    if ($script:AuthToken) {
        try {
            $headers = @{ Authorization = "Bearer $script:AuthToken" }
            $response = Invoke-RestMethod -Uri "$BaseUrl/api/auth/profile" -Headers $headers
            Write-Pass "Profile retrieved: $($response.email)"
        } catch {
            Write-Fail "Profile fetch failed: $_"
        }
    } else {
        Write-Fail "No auth token available"
    }
}

# Test Payment Endpoints
function Test-Payments {
    Write-Host ""
    Write-Host "=" * 60 -ForegroundColor Yellow
    Write-Host "Testing Payment Endpoints" -ForegroundColor Yellow
    Write-Host "=" * 60 -ForegroundColor Yellow
    
    if (-not $script:AuthToken) {
        Write-Fail "Auth token required. Run -Auth first."
        return
    }
    
    $headers = @{ Authorization = "Bearer $script:AuthToken" }
    
    # Get Payments
    Write-Test "List Payments"
    try {
        $response = Invoke-RestMethod -Uri "$BaseUrl/api/payments" -Headers $headers
        Write-Pass "Payments retrieved: $($response.totalElements) total"
    } catch {
        Write-Fail "List payments failed: $_"
    }
}

# Test Order Endpoints
function Test-Orders {
    Write-Host ""
    Write-Host "=" * 60 -ForegroundColor Yellow
    Write-Host "Testing Order Endpoints" -ForegroundColor Yellow
    Write-Host "=" * 60 -ForegroundColor Yellow
    
    if (-not $script:AuthToken) {
        Write-Fail "Auth token required. Run -Auth first."
        return
    }
    
    $headers = @{ Authorization = "Bearer $script:AuthToken" }
    
    # Create Order
    Write-Test "Create Order"
    try {
        $body = @{
            items = @(
                @{
                    productId = "PROD-001"
                    productName = "Test Product"
                    quantity = 1
                    unitPrice = 99.99
                }
            )
            currency = "USD"
        } | ConvertTo-Json -Depth 3
        
        $response = Invoke-RestMethod -Uri "$BaseUrl/api/orders" -Method Post -Body $body -ContentType "application/json" -Headers $headers
        $script:OrderId = $response.id
        Write-Pass "Order created: $($response.id)"
    } catch {
        Write-Fail "Create order failed: $_"
    }
    
    # Get Order
    if ($script:OrderId) {
        Write-Test "Get Order"
        try {
            $response = Invoke-RestMethod -Uri "$BaseUrl/api/orders/$script:OrderId" -Headers $headers
            Write-Pass "Order retrieved: $($response.id), status: $($response.status)"
        } catch {
            Write-Fail "Get order failed: $_"
        }
    }
    
    # List Orders
    Write-Test "List Orders"
    try {
        $response = Invoke-RestMethod -Uri "$BaseUrl/api/orders" -Headers $headers
        Write-Pass "Orders retrieved: $($response.totalElements) total"
    } catch {
        Write-Fail "List orders failed: $_"
    }
}

# Main execution
if ($RunAll) {
    Test-Health
    Test-Auth
    Test-Orders
    Test-Payments
} else {
    if ($Health) { Test-Health }
    if ($Auth) { Test-Auth }
    if ($Orders) { Test-Orders }
    if ($Payments) { Test-Payments }
    
    if (-not $Health -and -not $Auth -and -not $Orders -and -not $Payments) {
        Write-Host "Payment Gateway - E2E Testing" -ForegroundColor Yellow
        Write-Host ""
        Write-Host "Usage:" -ForegroundColor Cyan
        Write-Host "  .\test-e2e.ps1 -RunAll           Run all tests"
        Write-Host "  .\test-e2e.ps1 -Health           Test health endpoints"
        Write-Host "  .\test-e2e.ps1 -Auth             Test authentication"
        Write-Host "  .\test-e2e.ps1 -Orders           Test order endpoints"
        Write-Host "  .\test-e2e.ps1 -Payments         Test payment endpoints"
        Write-Host "  .\test-e2e.ps1 -BaseUrl http://staging:8080  Custom base URL"
    }
}
