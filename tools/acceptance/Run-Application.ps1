[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][ValidateSet('backend', 'frontend')][string]$Component,
    [Parameter(Mandatory = $true)][string]$RuntimeFile,
    [Parameter(Mandatory = $true)][string]$ProjectRoot
)

$ErrorActionPreference = 'Stop'
$runtime = Get-Content -LiteralPath $RuntimeFile -Raw | ConvertFrom-Json
$backendPort = [int]$runtime.BACKEND_PORT
$frontendPort = [int]$runtime.FRONTEND_PORT
$keycloakRoot = ([string]$runtime.KEYCLOAK_SERVER_URL).TrimEnd('/')
$realmUrl = "$keycloakRoot/realms/$($runtime.KEYCLOAK_REALM)"

$env:DB_HOST = [string]$runtime.DB_HOST
$env:DB_PORT = [string]$runtime.DB_PORT
$env:DB_NAME = [string]$runtime.DB_NAME
$env:DB_USER = [string]$runtime.DB_USER
$env:DB_PASSWORD = [string]$runtime.DB_PASSWORD
$env:REDIS_HOST = [string]$runtime.REDIS_HOST
$env:REDIS_PORT = [string]$runtime.REDIS_PORT
$env:KEYCLOAK_SERVER_URL = $keycloakRoot
$env:KEYCLOAK_REALM = [string]$runtime.KEYCLOAK_REALM
$env:KEYCLOAK_CLIENT_ID = [string]$runtime.KEYCLOAK_CLIENT_ID
$env:KEYCLOAK_CLIENT_SECRET = [string]$runtime.KEYCLOAK_CLIENT_SECRET
$env:KEYCLOAK_ISSUER_URI = $realmUrl
$env:KEYCLOAK_JWK_SET_URI = "$realmUrl/protocol/openid-connect/certs"
$env:BACKEND_PORT = [string]$backendPort
$env:CORS_ALLOWED_ORIGINS = "http://localhost:$frontendPort"
$env:HOTEL_SCHEDULING_ENABLED = 'false'
$env:HOTEL_AI_PERSONALIZATION_SYNTHETIC_EVALUATION_ENABLED = if (
    [string]$runtime.HOTEL_AI_PERSONALIZATION_SYNTHETIC_EVALUATION_ENABLED -eq 'true'
) { 'true' } else { 'false' }
$env:HOTEL_AI_PERSONALIZATION_LOCAL_DEMO_ENABLED = if (
    [string]$runtime.HOTEL_AI_PERSONALIZATION_LOCAL_DEMO_ENABLED -eq 'true'
) { 'true' } else { 'false' }
$env:SPRING_LIQUIBASE_ENABLED = 'false'
$env:LIQUIBASE_CONTEXTS = 'master-data'
$env:HOTEL_AI_ADVISOR_SECURITY_ANON_TOKEN_SECRET = [string]$runtime.HOTEL_AI_ADVISOR_SECURITY_ANON_TOKEN_SECRET
# The acceptance sandbox must use deterministic extraction even when the host has a Gemini key.
Remove-Item Env:GEMINI_API_KEY -ErrorAction SilentlyContinue

if ($Component -eq 'backend') {
    $backendDirectory = Join-Path $ProjectRoot 'backend'
    $mavenWrapper = Join-Path $backendDirectory 'mvnw.cmd'
    if (-not (Test-Path -LiteralPath $mavenWrapper)) {
        throw "Maven wrapper not found: $mavenWrapper"
    }

    Push-Location $backendDirectory
    try {
        & $mavenWrapper 'spring-boot:run'
        exit $LASTEXITCODE
    } finally {
        Pop-Location
    }
}

$frontendDirectory = Join-Path $ProjectRoot 'frontend'
if (-not (Test-Path -LiteralPath (Join-Path $frontendDirectory 'node_modules/.bin/vite.cmd'))) {
    throw "Frontend dependencies are missing. Run 'npm ci' in $frontendDirectory before starting the sandbox."
}
$env:VITE_API_URL = "http://localhost:$backendPort"
$env:VITE_KEYCLOAK_URL = $keycloakRoot
$env:VITE_KEYCLOAK_REALM = [string]$runtime.KEYCLOAK_REALM
$env:VITE_KEYCLOAK_CLIENT_ID = 'hotel-frontend'

Push-Location $frontendDirectory
try {
    & npm.cmd run dev -- --host 127.0.0.1 --port $frontendPort --strictPort
    exit $LASTEXITCODE
} finally {
    Pop-Location
}
