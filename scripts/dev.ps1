param(
    [Parameter(Position = 0)]
    [string]$Command = "help",

    [Parameter(Position = 1)]
    [string]$Target = ""
)

$ErrorActionPreference = "Stop"

$RepoRoot = Split-Path -Parent $PSScriptRoot
$FrontendDir = Join-Path $RepoRoot "services\frontend"
$DashboardDir = Join-Path $RepoRoot "services\dashboard"
$InfraServices = @(
    "vault",
    "redis",
    "config-service",
    "kafka",
    "postgres",
    "mongodb"
)
$CoreBackendServices = @(
    "auth-service",
    "order-service",
    "notification-service",
    "webhook-service",
    "simulator-service",
    "settlement-service"
)
$ExtendedBackendServices = @(
    "risk-service",
    "analytics-service",
    "merchant-service",
    "dispute-service"
)
$HybridEdgeServices = @(
    "api-gateway",
    "frontend",
    "dashboard"
)
$FullOnlyServices = @(
    "payment-service"
)
$HybridServices = @(
    "vault",
    "redis",
    "config-service",
    "kafka",
    "postgres",
    "mongodb",
    "auth-service",
    "order-service",
    "notification-service",
    "webhook-service",
    "simulator-service",
    "settlement-service",
    "risk-service",
    "analytics-service",
    "merchant-service",
    "dispute-service",
    "api-gateway",
    "frontend",
    "dashboard"
)

function Write-Step {
    param([string]$Message)
    Write-Host ""
    Write-Host "==> $Message" -ForegroundColor Cyan
}

function Test-CommandExists {
    param([string]$Name)
    return $null -ne (Get-Command $Name -ErrorAction SilentlyContinue)
}

function Require-Command {
    param([string]$Name, [string]$Hint)
    if (-not (Test-CommandExists $Name)) {
        throw "$Name is not available. $Hint"
    }
}

function Invoke-RepoCommand {
    param(
        [string]$FilePath,
        [string[]]$Arguments
    )

    Push-Location $RepoRoot
    try {
        & $FilePath @Arguments
        if ($LASTEXITCODE -ne 0) {
            throw "$FilePath failed with exit code $LASTEXITCODE."
        }
    } finally {
        Pop-Location
    }
}

function Invoke-UiCommand {
    param(
        [string]$Directory,
        [string]$FilePath,
        [string[]]$Arguments
    )

    Push-Location $Directory
    try {
        & $FilePath @Arguments
        if ($LASTEXITCODE -ne 0) {
            throw "$FilePath failed with exit code $LASTEXITCODE."
        }
    } finally {
        Pop-Location
    }
}

function Ensure-UiDependencies {
    param([string]$Directory)

    if (-not (Test-Path (Join-Path $Directory "node_modules"))) {
        Invoke-UiCommand $Directory "npm" @("ci")
    }
}

function Use-TemporaryEnv {
    param(
        [hashtable]$Variables,
        [scriptblock]$Script
    )

    $previous = @{}
    foreach ($key in $Variables.Keys) {
        $previous[$key] = [Environment]::GetEnvironmentVariable($key, "Process")
        [Environment]::SetEnvironmentVariable($key, $Variables[$key], "Process")
    }

    try {
        & $Script
    } finally {
        foreach ($key in $Variables.Keys) {
            [Environment]::SetEnvironmentVariable($key, $previous[$key], "Process")
        }
    }
}

function Get-ComposeServiceState {
    param([string]$ServiceName)

    Push-Location $RepoRoot
    try {
        $containerId = (& docker compose ps -q $ServiceName 2>$null | Select-Object -First 1)
    } finally {
        Pop-Location
    }
    if ([string]::IsNullOrWhiteSpace($containerId)) {
        return @{
            Service = $ServiceName
            State = "missing"
            Health = "missing"
        }
    }

    $inspect = (& docker inspect --format '{{.State.Status}}|{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' $containerId 2>$null | Select-Object -First 1)
    if ([string]::IsNullOrWhiteSpace($inspect)) {
        return @{
            Service = $ServiceName
            State = "unknown"
            Health = "unknown"
        }
    }

    $parts = $inspect.Trim().Split("|", 2)
    return @{
        Service = $ServiceName
        State = $parts[0]
        Health = $parts[1]
    }
}

function Wait-ForComposeServices {
    param(
        [string[]]$Services,
        [int]$TimeoutSeconds = 1200
    )

    if ($null -eq $Services -or $Services.Count -eq 0) {
        return
    }

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    $lastSummary = ""

    while ((Get-Date) -lt $deadline) {
        $pending = @()
        foreach ($service in $Services) {
            $state = Get-ComposeServiceState $service
            if ($state.State -ne "running") {
                $pending += "$service ($($state.State))"
                continue
            }

            if ($state.Health -in @("healthy", "none")) {
                continue
            }

            $pending += "$service ($($state.Health))"
        }

        if ($pending.Count -eq 0) {
            return
        }

        $summary = $pending -join ", "
        if ($summary -ne $lastSummary) {
            Write-Host "Waiting for: $summary"
            $lastSummary = $summary
        }

        Start-Sleep -Seconds 15
    }

    throw "Timed out waiting for Docker services: $($Services -join ', ')"
}

function Start-ComposeWave {
    param(
        [string[]]$Profiles,
        [string[]]$Services,
        [switch]$Build,
        [switch]$RemoveOrphans,
        [hashtable]$Environment = @{},
        [int]$TimeoutSeconds = 1200
    )

    $composeArgs = @("compose")
    foreach ($profile in $Profiles) {
        $composeArgs += @("--profile", $profile)
    }

    $composeArgs += @("up", "-d")
    if ($Build) {
        $composeArgs += "--build"
    }
    if ($RemoveOrphans) {
        $composeArgs += "--remove-orphans"
    }
    $composeArgs += $Services

    Use-TemporaryEnv $Environment {
        Invoke-RepoCommand "docker" $composeArgs
        Wait-ForComposeServices -Services $Services -TimeoutSeconds $TimeoutSeconds
    }
}

function Import-DotEnv {
    $envFile = Join-Path $RepoRoot ".env"
    if (-not (Test-Path $envFile)) {
        return
    }

    foreach ($line in Get-Content $envFile) {
        if ([string]::IsNullOrWhiteSpace($line) -or $line.TrimStart().StartsWith("#")) {
            continue
        }

        $parts = $line -split "=", 2
        if ($parts.Count -ne 2) {
            continue
        }

        $name = $parts[0].Trim()
        $value = $parts[1].Trim()
        if (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'"))) {
            $value = $value.Substring(1, $value.Length - 2)
        }

        if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($name, "Process"))) {
            [Environment]::SetEnvironmentVariable($name, $value, "Process")
        }
    }
}

function Show-Help {
    @"
Usage: ./scripts/dev.ps1 <command> [service-name]

Commands:
  bootstrap       Prepare local defaults and install UI dependencies when Node is available
  doctor          Validate Java, Maven wrapper, Docker, and optional Node/npm
  infra           Start Docker infrastructure only
  hybrid          Start Docker platform except payment-service
  full            Start the full Docker stack
  down            Stop the active Docker stack
  service-local   Run a single backend service locally (example: service-local payment-service)
  payment-local   Alias for service-local payment-service
  frontend-check  Run frontend and dashboard quality checks
  smoke           Run compose validation, backend smoke tests, and UI builds
  test-all        Run verify plus UI checks
  verify          Run backend verification
  compose-check   Validate infra, full, and hybrid Docker rendering
  help            Show this help
"@ | Write-Host
}

function Invoke-Bootstrap {
    Write-Step "Preparing local workspace"

    $envFile = Join-Path $RepoRoot ".env"
    $envExampleFile = Join-Path $RepoRoot ".env.example"

    if (-not (Test-Path $envFile) -and (Test-Path $envExampleFile)) {
        Copy-Item $envExampleFile $envFile
        Write-Host "Created .env from .env.example"
    } elseif (Test-Path $envFile) {
        Write-Host ".env already exists"
    }

    if ((Test-CommandExists "node") -and (Test-CommandExists "npm")) {
        Write-Step "Installing UI dependencies"
        Ensure-UiDependencies $FrontendDir
        Ensure-UiDependencies $DashboardDir
    } else {
        Write-Host "Skipping UI dependency install because node/npm are not available." -ForegroundColor Yellow
    }
}

function Invoke-Doctor {
    Write-Step "Checking local toolchain"

    $checks = @(
        @{ Name = "java"; Required = $true; Hint = "Install JDK 21 and add it to PATH." },
        @{ Name = "docker"; Required = $true; Hint = "Install Docker Desktop and ensure the daemon is running." },
        @{ Name = "node"; Required = $false; Hint = "Optional until you work on services/frontend or services/dashboard." },
        @{ Name = "npm"; Required = $false; Hint = "Optional until you work on services/frontend or services/dashboard." }
    )

    $failed = $false
    foreach ($check in $checks) {
        $exists = Test-CommandExists $check.Name
        $status = if ($exists) { "OK" } else { "MISSING" }
        Write-Host ("{0,-10} {1}" -f $check.Name, $status)
        if (-not $exists) {
            Write-Host ("           " + $check.Hint) -ForegroundColor Yellow
            if ($check.Required) {
                $failed = $true
            }
        }
    }

    if (Test-Path (Join-Path $RepoRoot ".env")) {
        Write-Host ".env       OK"
    } else {
        Write-Host ".env       MISSING" -ForegroundColor Yellow
        Write-Host "           Run ./scripts/dev.ps1 bootstrap first." -ForegroundColor Yellow
    }

    if (Test-CommandExists "docker") {
        docker info *> $null
        if ($LASTEXITCODE -eq 0) {
            Write-Host "dockerd    OK"
        } else {
            Write-Host "dockerd    NOT RUNNING" -ForegroundColor Yellow
            $failed = $true
        }
    }

    if ($failed) {
        throw "Required tooling is missing or Docker is not running."
    }
}

function Start-Infra {
    Require-Command docker "Install Docker Desktop and ensure the daemon is running."
    Write-Step "Starting Docker infrastructure"
    Start-ComposeWave -Profiles @("infra") -Services $InfraServices -RemoveOrphans -TimeoutSeconds 600
    Write-Host "Infrastructure is ready."
}

function Start-Full {
    Require-Command docker "Install Docker Desktop and ensure the daemon is running."
    Write-Step "Starting full Docker stack"
    Start-ComposeWave -Profiles @("services", "full") -Services $InfraServices -RemoveOrphans -TimeoutSeconds 600
    Start-ComposeWave -Profiles @("services", "full") -Services $CoreBackendServices -Build -TimeoutSeconds 900
    Start-ComposeWave -Profiles @("services", "full") -Services $FullOnlyServices -Build -TimeoutSeconds 1200
    Start-ComposeWave -Profiles @("services", "full") -Services $ExtendedBackendServices -Build -TimeoutSeconds 1200
    Start-ComposeWave -Profiles @("services", "full") -Services $HybridEdgeServices -Build -TimeoutSeconds 900
    Write-Host "Frontend:  http://localhost:3000"
    Write-Host "Dashboard: http://localhost:3001"
    Write-Host "Gateway:   http://localhost:8080"
}

function Start-Hybrid {
    Require-Command docker "Install Docker Desktop and ensure the daemon is running."
    Write-Step "Starting hybrid Docker stack without payment-service"
    $hybridEnv = @{ PAYMENT_SERVICE_URL = "http://host.docker.internal:8083" }
    Start-ComposeWave -Profiles @("services") -Services $InfraServices -RemoveOrphans -Environment $hybridEnv -TimeoutSeconds 600
    Start-ComposeWave -Profiles @("services") -Services $CoreBackendServices -Build -Environment $hybridEnv -TimeoutSeconds 900
    Start-ComposeWave -Profiles @("services") -Services $ExtendedBackendServices -Build -Environment $hybridEnv -TimeoutSeconds 1200
    Start-ComposeWave -Profiles @("services") -Services $HybridEdgeServices -Build -Environment $hybridEnv -TimeoutSeconds 900
    Write-Host "Docker services are up without payment-service."
    Write-Host "Run local payment-service with: ./scripts/dev.ps1 payment-local"
}

function Stop-Stack {
    Require-Command docker "Install Docker Desktop and ensure the daemon is running."
    Write-Step "Stopping Docker stack"
    Invoke-RepoCommand "docker" @("compose", "--profile", "services", "--profile", "full", "--profile", "infra", "--profile", "observability", "--profile", "advanced", "down", "--remove-orphans")
}

function Start-LocalService {
    param([string]$ServiceName)

    if ([string]::IsNullOrWhiteSpace($ServiceName)) {
        throw "Provide a service name, for example: ./scripts/dev.ps1 service-local payment-service"
    }

    $serviceDir = Join-Path $RepoRoot ("services\" + $ServiceName)
    if (-not (Test-Path $serviceDir)) {
        throw "Unknown service: $ServiceName"
    }
    $servicePom = Join-Path $serviceDir "pom.xml"
    if (-not (Test-Path $servicePom)) {
        throw "Service pom.xml not found for $ServiceName"
    }

    $localProfile = Join-Path $serviceDir "src\main\resources\application-local.yml"
    $devProfile = Join-Path $serviceDir "src\main\resources\application-dev.yml"
    $profile = $null

    if (Test-Path $localProfile) {
        $profile = "local"
    } elseif (Test-Path $devProfile) {
        $profile = "dev"
    }

    $servicePorts = @{
        "api-gateway" = 8080
        "auth-service" = 8081
        "order-service" = 8082
        "payment-service" = 8083
        "notification-service" = 8084
        "webhook-service" = 8085
        "simulator-service" = 8086
        "settlement-service" = 8087
        "risk-service" = 8088
        "analytics-service" = 8089
        "merchant-service" = 8090
        "dispute-service" = 8091
        "config-service" = 8888
    }

    Import-DotEnv
    $localEnv = @{
        DB_HOST = "localhost"
        POSTGRES_HOST = "localhost"
        DB_PORT = "5433"
        POSTGRES_PORT = "5433"
        KAFKA_BOOTSTRAP_SERVERS = "localhost:9092"
        REDIS_HOST = "localhost"
        REDIS_PORT = "6379"
        WEBHOOK_SERVICE_URL = "http://localhost:8085"
        ORDER_SERVICE_URL = "http://localhost:8082"
        PAYMENT_SERVICE_URL = "http://localhost:8083"
        SIMULATOR_SERVICE_URL = "http://localhost:8086"
        AUTH_SERVICE_URL = "http://localhost:8081"
        MERCHANT_SERVICE_URL = "http://localhost:8090"
        DISPUTE_SERVICE_URL = "http://localhost:8091"
    }

    $serviceDbEnvMap = @{
        "auth-service" = @{ DB_USERNAME = $env:AUTH_DB_USER; DB_PASSWORD = $env:AUTH_DB_PASSWORD; DB_NAME = "authdb" }
        "notification-service" = @{ DB_USERNAME = $env:NOTIFICATION_DB_USER; DB_PASSWORD = $env:NOTIFICATION_DB_PASSWORD; DB_NAME = "notificationdb" }
        "webhook-service" = @{ DB_USERNAME = $env:WEBHOOK_DB_USER; DB_PASSWORD = $env:WEBHOOK_DB_PASSWORD; DB_NAME = "webhookdb" }
        "simulator-service" = @{ DB_USERNAME = $env:SIMULATOR_DB_USER; DB_PASSWORD = $env:SIMULATOR_DB_PASSWORD; DB_NAME = "simulatordb" }
        "settlement-service" = @{ DB_USERNAME = $env:SETTLEMENT_DB_USER; DB_PASSWORD = $env:SETTLEMENT_DB_PASSWORD; DB_NAME = "settlementdb" }
        "risk-service" = @{ DB_USERNAME = $env:RISK_DB_USER; DB_PASSWORD = $env:RISK_DB_PASSWORD; DB_NAME = "riskdb" }
        "analytics-service" = @{ DB_USERNAME = $env:ANALYTICS_DB_USER; DB_PASSWORD = $env:ANALYTICS_DB_PASSWORD; DB_NAME = "analyticsdb" }
        "merchant-service" = @{ DB_USERNAME = $env:MERCHANT_DB_USER; DB_PASSWORD = $env:MERCHANT_DB_PASSWORD; DB_NAME = "merchantdb" }
        "dispute-service" = @{ DB_USERNAME = $env:DISPUTE_DB_USER; DB_PASSWORD = $env:DISPUTE_DB_PASSWORD; DB_NAME = "disputedb" }
        "payment-service" = @{ DB_USERNAME = $env:PAYMENT_DB_USER; DB_PASSWORD = $(if ($env:POSTGRES_PASSWORD) { $env:POSTGRES_PASSWORD } else { $env:PAYMENT_DB_PASSWORD }); DB_NAME = "paymentdb" }
    }

    if ($serviceDbEnvMap.ContainsKey($ServiceName)) {
        foreach ($entry in $serviceDbEnvMap[$ServiceName].GetEnumerator()) {
            if (-not [string]::IsNullOrWhiteSpace($entry.Value)) {
                $localEnv[$entry.Key] = $entry.Value
            }
        }
    }

    Write-Step "Running $ServiceName locally"
    Push-Location $RepoRoot
    try {
        if (Test-CommandExists "docker") {
            $containerId = (& docker compose ps -q $ServiceName 2>$null)
            if (-not [string]::IsNullOrWhiteSpace($containerId)) {
                Write-Host "Stopping Docker container for $ServiceName so the local process can bind its port."
                & docker compose stop $ServiceName | Out-Null
            }
        }

        if ($servicePorts.ContainsKey($ServiceName)) {
            $targetPort = $servicePorts[$ServiceName]
            $listeners = Get-NetTCPConnection -LocalPort $targetPort -State Listen -ErrorAction SilentlyContinue |
                Select-Object -ExpandProperty OwningProcess -Unique
            foreach ($listenerPid in $listeners) {
                $listener = Get-Process -Id $listenerPid -ErrorAction SilentlyContinue
                if ($null -ne $listener -and $listener.ProcessName -eq "java") {
                    Write-Host "Stopping stale local Java process on port $targetPort before starting $ServiceName."
                    Stop-Process -Id $listenerPid -Force -ErrorAction SilentlyContinue
                }
            }
        }

        $previousProfile = [Environment]::GetEnvironmentVariable("SPRING_PROFILES_ACTIVE", "Process")
        $localEnv["SPRING_PROFILES_ACTIVE"] = $profile
        Use-TemporaryEnv $localEnv {
            $arguments = @("-f", $servicePom, "clean", "spring-boot:run")
            if (-not [string]::IsNullOrWhiteSpace($profile)) {
                $arguments += "-Dspring-boot.run.profiles=$profile"
            }
            & ".\mvnw.cmd" @arguments
            if ($LASTEXITCODE -ne 0) {
                throw "mvnw.cmd failed with exit code $LASTEXITCODE."
            }
        }
    } finally {
        [Environment]::SetEnvironmentVariable("SPRING_PROFILES_ACTIVE", $previousProfile, "Process")
        Pop-Location
    }
}

function Invoke-FrontendCheck {
    Require-Command node "Install Node.js 22 and add it to PATH."
    Require-Command npm "Install Node.js and npm before working on the frontend."

    Write-Step "Running frontend checks"
    Ensure-UiDependencies $FrontendDir
    Invoke-UiCommand $FrontendDir "npm" @("run", "check")

    Write-Step "Running dashboard checks"
    Ensure-UiDependencies $DashboardDir
    Invoke-UiCommand $DashboardDir "npm" @("run", "lint")
    Invoke-UiCommand $DashboardDir "npm" @("run", "build")
}

function Invoke-Verify {
    Write-Step "Running backend verification"
    Invoke-RepoCommand ".\mvnw.cmd" @("-q", "verify")
}

function Invoke-ComposeCheck {
    Require-Command docker "Install Docker Desktop and ensure the daemon is running."
    Write-Step "Validating compose rendering"
    Push-Location $RepoRoot
    try {
        docker compose --profile infra config | Out-Null
        if ($LASTEXITCODE -ne 0) {
            throw "docker compose infra config failed."
        }

        docker compose --profile services config | Out-Null
        if ($LASTEXITCODE -ne 0) {
            throw "docker compose services config failed."
        }

        docker compose --profile services --profile full config | Out-Null
        if ($LASTEXITCODE -ne 0) {
            throw "docker compose full config failed."
        }

        Use-TemporaryEnv @{ PAYMENT_SERVICE_URL = "http://host.docker.internal:8083" } {
            docker compose --profile services config | Out-Null
            if ($LASTEXITCODE -ne 0) {
                throw "docker compose hybrid config failed."
            }
        }
    } finally {
        Pop-Location
    }
}

function Invoke-Smoke {
    Write-Step "Running smoke checks"
    Invoke-ComposeCheck
    Invoke-RepoCommand ".\mvnw.cmd" @("-q", "-pl", "services/payment-service,services/api-gateway", "-am", "test")

    if ((Test-CommandExists "node") -and (Test-CommandExists "npm")) {
        Invoke-FrontendCheck
    } else {
        Write-Host "Skipping UI checks because node/npm are not available." -ForegroundColor Yellow
    }
}

function Invoke-TestAll {
    Write-Step "Running full verification"
    Invoke-ComposeCheck
    Invoke-Verify

    if ((Test-CommandExists "node") -and (Test-CommandExists "npm")) {
        Invoke-FrontendCheck
    } else {
        Write-Host "Skipping UI checks because node/npm are not available." -ForegroundColor Yellow
    }
}

switch ($Command.ToLowerInvariant()) {
    "bootstrap" { Invoke-Bootstrap }
    "doctor" { Invoke-Doctor }
    "infra" { Start-Infra }
    "hybrid" { Start-Hybrid }
    "full" { Start-Full }
    "down" { Stop-Stack }
    "service-local" { Start-LocalService $Target }
    "payment-local" { Start-LocalService "payment-service" }
    "frontend-check" { Invoke-FrontendCheck }
    "smoke" { Invoke-Smoke }
    "test-all" { Invoke-TestAll }
    "verify" { Invoke-Verify }
    "compose-check" { Invoke-ComposeCheck }
    "help" { Show-Help }
    default { Show-Help; throw "Unknown command: $Command" }
}
