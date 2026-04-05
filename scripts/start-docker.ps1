$dockerPath = "C:\Program Files\Docker\Docker\Docker Desktop.exe"
if (Test-Path $dockerPath) {
    Write-Host "Starting Docker Desktop..."
    Start-Process $dockerPath -ArgumentList "--windowed"
    Write-Host "Started, waiting 30 seconds..."
    Start-Sleep -Seconds 30
    Write-Host "Done"
} else {
    Write-Host "Docker not found at: $dockerPath"
}
