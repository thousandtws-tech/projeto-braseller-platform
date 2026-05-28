param(
    [string]$Tag = "local",
    [string[]]$Services = @(
        "gateway-api",
        "core-service",
        "billing-service",
        "notification-service",
        "reporting-service",
        "user-service",
        "auth-service"
    )
)

$ErrorActionPreference = "Stop"
$RootPath = Split-Path -Parent $PSScriptRoot

foreach ($Service in $Services) {
    $ServicePath = Join-Path $RootPath $Service
    if (-not (Test-Path $ServicePath)) {
        throw "Service path not found: $ServicePath"
    }

    Write-Host "Building brasaller/$Service`:$Tag"
    Push-Location $ServicePath
    try {
        & docker build -f "src/main/docker/Dockerfile.jvm" -t "brasaller/$Service`:$Tag" "."
        if ($LASTEXITCODE -ne 0) {
            throw "$Service image build failed with exit code $LASTEXITCODE"
        }
    }
    finally {
        Pop-Location
    }
}
