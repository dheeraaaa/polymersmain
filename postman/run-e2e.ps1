<#!
.SYNOPSIS
Starts the StockAI backend and runs the complete Postman E2E collection.

.DESCRIPTION
Requires Docker Desktop and Node.js (npx). The first invocation downloads Newman
if it is not already available. Test data is created in the MongoDB Atlas database
configured by backend/.env.
#>

[CmdletBinding()]
param(
    [switch]$SkipBuild
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$collection = Join-Path $PSScriptRoot 'StockAI Backend API.postman_collection.json'
$healthUrl = 'http://localhost:4000/api/v1/health'

if (-not (Test-Path -LiteralPath $collection)) {
    throw "Postman collection was not found: $collection"
}

Push-Location $projectRoot
try {
    if ($SkipBuild) {
        & docker compose up -d
    } else {
        & docker compose up -d --build
    }
    if ($LASTEXITCODE -ne 0) { throw 'Docker Compose could not start the backend.' }

    $ready = $false
    for ($attempt = 1; $attempt -le 30; $attempt++) {
        try {
            $response = Invoke-WebRequest -Uri $healthUrl -UseBasicParsing -TimeoutSec 3
            if ($response.StatusCode -eq 200) { $ready = $true; break }
        } catch {
            Start-Sleep -Seconds 2
        }
    }
    if (-not $ready) { throw "Backend did not become healthy at $healthUrl. Run 'docker compose logs backend' for details." }

    & npx --yes newman run $collection --reporters cli
    if ($LASTEXITCODE -ne 0) { throw 'One or more E2E API tests failed.' }
} finally {
    Pop-Location
}
