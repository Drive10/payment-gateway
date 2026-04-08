param(
  [string]$FrontendUrl = $env:E2E_BASE_URL,
  [string]$GatewayUrl = $env:E2E_GATEWAY_URL
)

$ErrorActionPreference = "Stop"
$RootDir = Split-Path -Parent $MyInvocation.MyCommand.Path

if ([string]::IsNullOrWhiteSpace($FrontendUrl)) { $FrontendUrl = "http://localhost:5173" }
if ([string]::IsNullOrWhiteSpace($GatewayUrl)) { $GatewayUrl = "http://localhost:8080" }

$healthUrlsRaw = if ([string]::IsNullOrWhiteSpace($env:E2E_REQUIRED_HEALTH_URLS)) {
  $GatewayUrl
} else {
  $env:E2E_REQUIRED_HEALTH_URLS
}

$healthUrls = $healthUrlsRaw.Split(",") | ForEach-Object { $_.Trim() } | Where-Object { -not [string]::IsNullOrWhiteSpace($_) }
foreach ($healthUrl in $healthUrls) {
  Write-Host "[e2e] waiting for service health at $healthUrl"
  while ($true) {
    try {
      Invoke-RestMethod -Uri "$healthUrl/actuator/health" -TimeoutSec 5 | Out-Null
      break
    } catch {
      Start-Sleep -Seconds 2
    }
  }
}

Write-Host "[e2e] waiting for frontend at $FrontendUrl"
while ($true) {
  try {
    $resp = Invoke-WebRequest -Uri $FrontendUrl -TimeoutSec 5 -UseBasicParsing
    if ($resp.StatusCode -ge 200) { break }
  } catch {
    Start-Sleep -Seconds 2
  }
}

Write-Host "[e2e] running fullstack frontend+backend Playwright spec"
Push-Location "$RootDir\web\frontend"
try {
  $env:E2E_BASE_URL = $FrontendUrl
  npm run test:e2e:fullstack
} finally {
  Pop-Location
}
