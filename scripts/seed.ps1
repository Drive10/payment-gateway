# Seed demo data for local development
# Usage: .\scripts\seed.ps1
# Requires: API Gateway running at http://localhost:8080

param(
    [string]$Gateway = "http://localhost:8080/api/v1"
)

$CYAN = "`e[36m"
$GREEN = "`e[32m"
$YELLOW = "`e[33m"
$RED = "`e[31m"
$NC = "`e[0m"

Write-Host ""
Write-Host "${CYAN}╔══════════════════════════════════════════╗${NC}" -NoNewline
Write-Host ""
Write-Host "${CYAN}║   Seeding Demo Data                     ║${NC}" -NoNewline
Write-Host ""
Write-Host "${CYAN}╚══════════════════════════════════════════╝${NC}"
Write-Host ""

# Check gateway is running
try {
    $health = Invoke-RestMethod -Uri "$Gateway/actuator/health" -TimeoutSec 3 -ErrorAction Stop
    if ($health.status -ne "UP") {
        Write-Host "${RED}API Gateway status: $($health.status)${NC}" -NoNewline
        Write-Host ""
        Write-Host "${YELLOW}Start it with: make dev:start${NC}" -NoNewline
        Write-Host ""
        exit 1
    }
} catch {
    Write-Host "${RED}API Gateway not running at $Gateway${NC}" -NoNewline
    Write-Host ""
    Write-Host "${YELLOW}Start it with: make dev:start${NC}" -NoNewline
    Write-Host ""
    exit 1
}

Write-Host "${GREEN}✓ API Gateway is running${NC}" -NoNewline
Write-Host ""

# 1. Create admin user
Write-Host "${YELLOW}Creating admin user...${NC}" -NoNewline
Write-Host ""
$adminBody = @{
    email = "admin@payflow.com"
    password = "Test@1234"
    firstName = "Admin"
    lastName = "User"
    role = "ADMIN"
} | ConvertTo-Json

try {
    $adminResp = Invoke-RestMethod -Uri "$Gateway/auth/register" -Method Post -Body $adminBody -ContentType "application/json" -ErrorAction SilentlyContinue
    Write-Host "${GREEN}✓ Admin user created${NC}" -NoNewline
    Write-Host ""
} catch {
    Write-Host "${GREEN}✓ Admin user already exists${NC}" -NoNewline
    Write-Host ""
}

# 2. Create regular user
Write-Host "${YELLOW}Creating regular user...${NC}" -NoNewline
Write-Host ""
$userBody = @{
    email = "john@payflow.com"
    password = "Test@1234"
    firstName = "John"
    lastName = "Doe"
    role = "USER"
} | ConvertTo-Json

try {
    $userResp = Invoke-RestMethod -Uri "$Gateway/auth/register" -Method Post -Body $userBody -ContentType "application/json" -ErrorAction SilentlyContinue
    Write-Host "${GREEN}✓ Regular user created${NC}" -NoNewline
    Write-Host ""
} catch {
    Write-Host "${GREEN}✓ Regular user already exists${NC}" -NoNewline
    Write-Host ""
}

# 3. Login as admin and get token
Write-Host "${YELLOW}Logging in as admin...${NC}" -NoNewline
Write-Host ""
$loginBody = @{
    email = "admin@payflow.com"
    password = "Test@1234"
} | ConvertTo-Json

$loginResp = Invoke-RestMethod -Uri "$Gateway/auth/login" -Method Post -Body $loginBody -ContentType "application/json" -ErrorAction Stop
$token = $loginResp.accessToken

if (-not $token) {
    Write-Host "${RED}Failed to get auth token${NC}" -NoNewline
    Write-Host ""
    exit 1
}
Write-Host "${GREEN}✓ Admin token obtained${NC}" -NoNewline
Write-Host ""

$headers = @{
    "Content-Type" = "application/json"
    "Authorization" = "Bearer $token"
}

# 4. Create orders
Write-Host "${YELLOW}Creating orders...${NC}" -NoNewline
Write-Host ""

$order1Body = @{
    amount = 5000
    currency = "USD"
    items = @(@{name = "Premium Plan"; quantity = 1; price = 5000})
} | ConvertTo-Json

$order1 = Invoke-RestMethod -Uri "$Gateway/orders" -Method Post -Body $order1Body -Headers $headers -ContentType "application/json" -ErrorAction Stop
$order1Id = $order1.id
Write-Host "${GREEN}✓ Order 1: $order1Id${NC}" -NoNewline
Write-Host ""

$order2Body = @{
    amount = 2500
    currency = "USD"
    items = @(@{name = "Basic Plan"; quantity = 1; price = 2500})
} | ConvertTo-Json

$order2 = Invoke-RestMethod -Uri "$Gateway/orders" -Method Post -Body $order2Body -Headers $headers -ContentType "application/json" -ErrorAction Stop
$order2Id = $order2.id
Write-Host "${GREEN}✓ Order 2: $order2Id${NC}" -NoNewline
Write-Host ""

$order3Body = @{
    amount = 10000
    currency = "USD"
    items = @(@{name = "Enterprise Plan"; quantity = 1; price = 10000})
} | ConvertTo-Json

$order3 = Invoke-RestMethod -Uri "$Gateway/orders" -Method Post -Body $order3Body -Headers $headers -ContentType "application/json" -ErrorAction Stop
$order3Id = $order3.id
Write-Host "${GREEN}✓ Order 3: $order3Id${NC}" -NoNewline
Write-Host ""

# 5. Create payments
Write-Host "${YELLOW}Creating payments...${NC}" -NoNewline
Write-Host ""

$pay1Body = @{
    orderId = $order1Id
    amount = 5000
    currency = "USD"
    provider = "razorpay"
    method = "CARD"
    merchantId = "550e8400-e29b-41d4-a716-446655440000"
} | ConvertTo-Json

$pay1 = Invoke-RestMethod -Uri "$Gateway/payments" -Method Post -Body $pay1Body -Headers $headers -ContentType "application/json" -ErrorAction Stop
$pay1Id = $pay1.id
Write-Host "${GREEN}✓ Payment 1 created: $pay1Id${NC}" -NoNewline
Write-Host ""

$pay2Body = @{
    orderId = $order2Id
    amount = 2500
    currency = "USD"
    provider = "razorpay"
    method = "CARD"
    merchantId = "550e8400-e29b-41d4-a716-446655440000"
} | ConvertTo-Json

$pay2 = Invoke-RestMethod -Uri "$Gateway/payments" -Method Post -Body $pay2Body -Headers $headers -ContentType "application/json" -ErrorAction Stop
$pay2Id = $pay2.id
Write-Host "${GREEN}✓ Payment 2 created: $pay2Id${NC}" -NoNewline
Write-Host ""

# 6. Capture payment 1
Write-Host "${YELLOW}Capturing payment 1...${NC}" -NoNewline
Write-Host ""

$captureBody = @{
    amount = 5000
} | ConvertTo-Json

try {
    $null = Invoke-RestMethod -Uri "$Gateway/payments/$pay1Id/capture" -Method Post -Body $captureBody -Headers $headers -ContentType "application/json" -ErrorAction Stop
    Write-Host "${GREEN}✓ Payment 1 captured${NC}" -NoNewline
    Write-Host ""
} catch {
    Write-Host "${YELLOW}! Payment 1 may already be captured${NC}" -NoNewline
    Write-Host ""
}

Write-Host ""
Write-Host "${CYAN}╔══════════════════════════════════════════════════════╗${NC}" -NoNewline
Write-Host ""
Write-Host "${CYAN}║  Demo Data Summary                                   ║${NC}" -NoNewline
Write-Host ""
Write-Host "${CYAN}╠══════════════════════════════════════════════════════╣${NC}" -NoNewline
Write-Host ""
Write-Host "${CYAN}║  Admin:  admin@payflow.com / Test@1234               ║${NC}" -NoNewline
Write-Host ""
Write-Host "${CYAN}║  User:   john@payflow.com / Test@1234                ║${NC}" -NoNewline
Write-Host ""
Write-Host "${CYAN}║                                                      ║${NC}" -NoNewline
Write-Host ""
Write-Host "${CYAN}║  Order 1: Premium Plan   (`$50.00) - Payment Captured ║${NC}" -NoNewline
Write-Host ""
Write-Host "${CYAN}║  Order 2: Basic Plan     (`$25.00) - Payment Created  ║${NC}" -NoNewline
Write-Host ""
Write-Host "${CYAN}║  Order 3: Enterprise Plan(`$100.00)- No Payment       ║${NC}" -NoNewline
Write-Host ""
Write-Host "${CYAN}╠══════════════════════════════════════════════════════╣${NC}" -NoNewline
Write-Host ""
Write-Host "${CYAN}║  Dashboard: http://localhost:3001/login              ║${NC}" -NoNewline
Write-Host ""
Write-Host "${CYAN}║  Frontend:  http://localhost:3000                    ║${NC}" -NoNewline
Write-Host ""
Write-Host "${CYAN}║  API:       http://localhost:8080                    ║${NC}" -NoNewline
Write-Host ""
Write-Host "${CYAN}╚══════════════════════════════════════════════════════╝${NC}"
