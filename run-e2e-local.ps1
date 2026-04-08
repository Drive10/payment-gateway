$ErrorActionPreference = "Stop"
$RootDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$frontendUrl = if ($env:E2E_BASE_URL) { $env:E2E_BASE_URL } else { "http://localhost:5173" }
$gatewayUrl = if ($env:E2E_GATEWAY_URL) { $env:E2E_GATEWAY_URL } else { "http://localhost:8080" }
$healthUrls = if ($env:E2E_REQUIRED_HEALTH_URLS) {
  $env:E2E_REQUIRED_HEALTH_URLS
} else {
  "http://localhost:8080,http://localhost:8081,http://localhost:8082,http://localhost:8083,http://localhost:8084,http://localhost:8086,http://localhost:8089,http://localhost:8090"
}
$env:E2E_REQUIRED_HEALTH_URLS = $healthUrls
& "$RootDir\run-e2e.ps1" -FrontendUrl $frontendUrl -GatewayUrl $gatewayUrl
