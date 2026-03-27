param(
    [Parameter(Position = 0)]
    [string]$Command = "help"
)

$ErrorActionPreference = "Stop"

$RepoRoot = Split-Path -Parent $PSScriptRoot
$FrontendDir = Join-Path $RepoRoot "services\frontend"

function Write-Step {
    param([string]$Message)
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
    } finally {
        Pop-Location
    }
}

function Invoke-FrontendCommand {
    param(
        [string]$FilePath,
        [string[]]$Arguments
    )

    Push-Location $FrontendDir
    try {
        & $FilePath @Arguments
    } finally {
        Pop-Location
    }
}

function Show-Help {
    @"
Usage: ./scripts/dev.ps1 <command>

Commands:
  bootstrap       Prepare local repo defaults and optional frontend deps
  doctor          Validate Java, Maven, Docker, and optional Node/npm
  hybrid          Start Docker services for hybrid mode
  full            Start the full Docker stack
  down            Stop the active Docker stack
  payment-local   Run payment-service locally with the local profile
  frontend-check  Run frontend install + quality checks
  smoke           Run fast local smoke checks
  verify          Run backend verification
  compose-check   Validate Compose rendering for hybrid and full modes
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
        Write-Step "Installing frontend dependencies"
        Invoke-FrontendCommand "npm" @("ci")
    } else {
        Write-Host "Skipping frontend install because node/npm are not available." -ForegroundColor Yellow
    }

    Write-Host "Next steps:"
    Write-Host "  1. ./scripts/dev.ps1 doctor"
    Write-Host "  2. ./scripts/dev.ps1 hybrid"
    Write-Host "  3. ./scripts/dev.ps1 payment-local"
}

function Invoke-Doctor {
    Write-Step "Checking local toolchain"

    $checks = @(
        @{ Name = "java"; Required = $true; Hint = "Install JDK 21 and add it to PATH." },
        @{ Name = "mvn"; Required = $false; Hint = "Optional because the repo includes mvnw.cmd." },
        @{ Name = "docker"; Required = $true; Hint = "Install Docker Desktop and ensure the daemon is running." },
        @{ Name = "node"; Required = $false; Hint = "Optional until you work on services/frontend. Use the version from .nvmrc." },
        @{ Name = "npm"; Required = $false; Hint = "Optional until you work on services/frontend. Install Node.js first." }
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

    if (-not (Test-Path (Join-Path $RepoRoot ".env"))) {
        Write-Host ".env       MISSING" -ForegroundColor Yellow
        Write-Host "           Copy .env.example to .env before running the platform." -ForegroundColor Yellow
    } else {
        Write-Host ".env       OK"
    }

    if ($failed) {
        throw "Required tooling is missing."
    }
}

function Start-Hybrid {
    Require-Command docker "Install Docker Desktop and ensure the daemon is running."
    Write-Step "Starting hybrid Docker stack"
    Invoke-RepoCommand "docker" @(
        "compose",
        "-f", "docker-compose.yml",
        "-f", "docker-compose.override.yml",
        "--profile", "services",
        "--profile", "optional",
        "up", "-d", "--build"
    )
}

function Start-Full {
    Require-Command docker "Install Docker Desktop and ensure the daemon is running."
    Write-Step "Starting full Docker stack"
    Invoke-RepoCommand "docker" @(
        "compose",
        "-f", "docker-compose.yml",
        "-f", "docker-compose.docker.yml",
        "--profile", "services",
        "--profile", "full",
        "--profile", "optional",
        "up", "-d", "--build"
    )
}

function Stop-Stack {
    Require-Command docker "Install Docker Desktop and ensure the daemon is running."
    Write-Step "Stopping Docker stack"
    Invoke-RepoCommand "docker" @("compose", "down")
}

function Start-PaymentLocal {
    Write-Step "Running payment-service locally"
    Push-Location $RepoRoot
    try {
        $env:SPRING_PROFILES_ACTIVE = "local"
        & ".\mvnw.cmd" "-pl" "services/payment-service" "-am" "spring-boot:run"
    } finally {
        Pop-Location
    }
}

function Invoke-FrontendCheck {
    Require-Command node "Install Node.js $(Get-Content (Join-Path $RepoRoot '.nvmrc')) and add it to PATH."
    Require-Command npm "Install Node.js and npm before working on the frontend."
    Write-Step "Running frontend quality checks"
    Invoke-FrontendCommand "npm" @("ci")
    Invoke-FrontendCommand "npm" @("run", "check")
}

function Invoke-Verify {
    Write-Step "Running backend verification"
    Push-Location $RepoRoot
    try {
        & ".\mvnw.cmd" "-q" "verify"
    } finally {
        Pop-Location
    }
}

function Invoke-ComposeCheck {
    Require-Command docker "Install Docker Desktop and ensure the daemon is running."
    Write-Step "Validating Compose files"
    Push-Location $RepoRoot
    try {
        docker compose -f docker-compose.yml -f docker-compose.override.yml --profile services config | Out-Null
        docker compose -f docker-compose.yml -f docker-compose.docker.yml --profile services --profile full --profile optional config | Out-Null
    } finally {
        Pop-Location
    }
}

function Invoke-Smoke {
    Write-Step "Running fast smoke checks"
    Invoke-ComposeCheck

    Push-Location $RepoRoot
    try {
        & ".\mvnw.cmd" "-q" "-pl" "services/payment-service,services/ledger-service,services/api-gateway" "-am" "test"
    } finally {
        Pop-Location
    }

    if ((Test-CommandExists "node") -and (Test-CommandExists "npm")) {
        Write-Step "Running frontend lint"
        Invoke-FrontendCommand "npm" @("run", "lint")
    } else {
        Write-Host "Skipping frontend lint because node/npm are not available." -ForegroundColor Yellow
    }
}

switch ($Command.ToLowerInvariant()) {
    "bootstrap" { Invoke-Bootstrap }
    "doctor" { Invoke-Doctor }
    "hybrid" { Start-Hybrid }
    "full" { Start-Full }
    "down" { Stop-Stack }
    "payment-local" { Start-PaymentLocal }
    "frontend-check" { Invoke-FrontendCheck }
    "smoke" { Invoke-Smoke }
    "verify" { Invoke-Verify }
    "compose-check" { Invoke-ComposeCheck }
    "help" { Show-Help }
    default { Show-Help; throw "Unknown command: $Command" }
}
