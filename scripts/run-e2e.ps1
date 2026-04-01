<#
  End-to-End harness runner for Windows
  Usage: powershell -ExecutionPolicy Bypass -File scripts/run-e2e.ps1
#>
param()
{
}

Write-Host "Starting End-to-End harness (Windows)" -ForegroundColor Yellow

# Start infra
docker-compose --profile infra up -d
Start-Sleep -Seconds 20

$env:RUN_E2E = "true"
mvn -Dtest=dev.payment.paymentservice.e2e.EndToEndPaymentFlowTest test

docker-compose --profile infra down -v

Write-Host "End-to-End harness finished." -ForegroundColor Green
