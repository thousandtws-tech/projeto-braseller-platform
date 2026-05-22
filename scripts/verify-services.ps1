param(
    [string[]]$Services = @(
        "gateway-api",
        "core-service",
        "billing-service",
        "notification-service",
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

    Write-Host "Verifying $Service"
    Push-Location $ServicePath
    try {
        & .\mvnw.cmd -B -ntp verify
        if ($LASTEXITCODE -ne 0) {
            throw "$Service verification failed with exit code $LASTEXITCODE"
        }
    }
    finally {
        Pop-Location
    }
}
