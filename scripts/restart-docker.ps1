# Kill stuck Docker processes and restart
Write-Host "Checking for stuck Docker processes..."

# Kill all Docker-related processes
$procs = Get-Process -Name "Docker*" -ErrorAction SilentlyContinue
if ($procs) {
    Write-Host "Found stuck Docker processes, stopping..."
    $procs | Stop-Process -Force -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 3
}

# Check if Docker Desktop is running
$dockerPath = "C:\Program Files\Docker\Docker\Docker Desktop.exe"
if (Test-Path $dockerPath) {
    Write-Host "Starting Docker Desktop..."
    Start-Process $dockerPath -ArgumentList "--windowed"
    Write-Host "Started. Waiting 60 seconds for Docker to initialize..."
    Start-Sleep -Seconds 60
    
    Write-Host "Checking Docker status..."
    $output = docker ps 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "Docker is running!" -ForegroundColor Green
        docker ps
    } else {
        Write-Host "Docker may still be starting..." -ForegroundColor Yellow
    }
} else {
    Write-Host "Docker Desktop not found at: $dockerPath" -ForegroundColor Red
}
