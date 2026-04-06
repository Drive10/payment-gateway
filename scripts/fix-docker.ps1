# Force kill ALL Docker processes and restart fresh
Write-Host "=== Docker Force Restart ===" -ForegroundColor Cyan

# 1. Kill ALL Docker processes
Write-Host "`n[1/5] Killing all Docker processes..." -ForegroundColor Yellow
Get-Process -Name "Docker*" -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
Get-Process -Name "com.docker*" -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
Start-Sleep -Seconds 5

# 2. Check for Hyper-V status
Write-Host "[2/5] Checking Hyper-V..." -ForegroundColor Yellow
$hyperv = Get-WindowsOptionalFeature -Online -FeatureName Microsoft-Hyper-V-All -ErrorAction SilentlyContinue
if ($hyperv.State -ne "Enabled") {
    Write-Host "Hyper-V is NOT enabled. This may be why Docker won't start." -ForegroundColor Red
    Write-Host "Enable Hyper-V in: Control Panel > Programs > Turn Windows features on/off" -ForegroundColor Yellow
}

# 3. Clean Docker data
Write-Host "[3/5] Cleaning Docker data..." -ForegroundColor Yellow
$dockerData = "$env:PROGRAMDATA\Docker"
if (Test-Path $dockerData) {
    Write-Host "Docker data folder exists at: $dockerData"
}

# 4. Start Docker Desktop
Write-Host "[4/5] Starting Docker Desktop..." -ForegroundColor Yellow
$dockerPath = "C:\Program Files\Docker\Docker\Docker Desktop.exe"
if (Test-Path $dockerPath) {
    Start-Process $dockerPath -ArgumentList "--windowed" -PassThru
    Write-Host "Docker Desktop started, waiting 90 seconds..." -ForegroundColor Cyan
    Start-Sleep -Seconds 90
    
    # 5. Test Docker
    Write-Host "[5/5] Testing Docker..." -ForegroundColor Yellow
    $test = docker ps 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "`n=== DOCKER IS RUNNING ===" -ForegroundColor Green
        docker ps
    } else {
        Write-Host "`n=== DOCKER STILL NOT WORKING ===" -ForegroundColor Red
        Write-Host "Error: $test"
    }
} else {
    Write-Host "Docker Desktop not found at: $dockerPath" -ForegroundColor Red
    Write-Host "Please install Docker Desktop from: https://www.docker.com/products/docker-desktop/" -ForegroundColor Yellow
}
