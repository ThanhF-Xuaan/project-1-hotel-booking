[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][ValidateSet('backend', 'frontend')][string]$Component,
    [Parameter(Mandatory = $true)][string]$RuntimeFile,
    [Parameter(Mandatory = $true)][string]$ProjectRoot
)

$ErrorActionPreference = 'Stop'
$runtime = Get-Content -LiteralPath $RuntimeFile -Raw | ConvertFrom-Json
$env:HOTEL_AI_DEMO_DATA_SOURCE = 'fixture'
foreach ($name in @('M1_DEMO_DB_URL','M1_DEMO_DB_USER','M1_DEMO_DB_PASSWORD')) {
    Remove-Item -LiteralPath "Env:$name" -ErrorAction SilentlyContinue
}
if ($runtime.HOTEL_AI_DEMO_DATA_SOURCE -and [string]$runtime.HOTEL_AI_DEMO_DATA_SOURCE -cnotin @('fixture','db')) {
    throw 'Invalid analytics demo data source.'
}
if ($Component -eq 'backend' -and [string]$runtime.HOTEL_AI_DEMO_DATA_SOURCE -ceq 'db') {
    if ([string]$runtime.M1_DEMO_DB_URL -cne 'jdbc:postgresql://localhost:15432/hotel_acceptance_db') {
        throw 'Analytics DB must use the fixed local acceptance target.'
    }
    $private = Get-Content -LiteralPath ([string]$runtime.M1_DEMO_DB_CREDENTIAL_FILE) -Raw | ConvertFrom-Json
    if ([string]$private.M1_DEMO_DB_USER -cnotmatch '^m1_demo_reader_[a-f0-9]{10}$' -or
        [string]::IsNullOrWhiteSpace([string]$private.M1_DEMO_DB_PASSWORD)) { throw 'Missing analytics readonly credentials.' }
    $env:HOTEL_AI_DEMO_DATA_SOURCE = 'db'
    $env:M1_DEMO_DB_URL = [string]$runtime.M1_DEMO_DB_URL
    $env:M1_DEMO_DB_USER = [string]$private.M1_DEMO_DB_USER
    $env:M1_DEMO_DB_PASSWORD = [string]$private.M1_DEMO_DB_PASSWORD
}
. (Join-Path $PSScriptRoot 'Rag-Runtime.ps1')
$ragRuntime = Resolve-AcceptanceRagRuntime $runtime
$profiles = [System.Collections.Generic.List[string]]::new()
if ($ragRuntime.Profile) { $profiles.Add($ragRuntime.Profile) }
$env:HOTEL_AI_DEMO_FORECAST_ENABLED = 'false'
if ([string]$runtime.HOTEL_AI_DEMO_FORECAST_ENABLED -ceq 'true') {
    $profiles.Add('m1-demo')
    $env:HOTEL_AI_DEMO_FORECAST_ENABLED = 'true'
}
$env:HOTEL_AI_DEMO_UTILITY_ENABLED = 'false'
if ([string]$runtime.HOTEL_AI_DEMO_UTILITY_ENABLED -ceq 'true') {
    if (-not $profiles.Contains('m1-demo')) { $profiles.Add('m1-demo') }
    $env:HOTEL_AI_DEMO_UTILITY_ENABLED = 'true'
}
$backendPort = [int]$runtime.BACKEND_PORT
$env:HOTEL_AI_DEMO_GROUP_ENABLED = 'false'
if ([string]$runtime.HOTEL_AI_DEMO_GROUP_ENABLED -ceq 'true') {
    if (-not $profiles.Contains('m1-demo')) { $profiles.Add('m1-demo') }
    $env:HOTEL_AI_DEMO_GROUP_ENABLED = 'true'
}
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
$env:HOTEL_AI_ADVISOR_PROVIDER_GEMINIENABLED = 'false'
$env:HOTEL_AI_ADVISOR_PROVIDER_GEMINI_ENABLED = 'false'
Remove-Item Env:GEMINI_API_KEY -ErrorAction SilentlyContinue
Remove-Item Env:GOOGLE_API_KEY -ErrorAction SilentlyContinue
if ([string]$runtime.HOTEL_AI_OCR_ENABLED -eq 'true') {
    if (-not $runtime.TESSERACT_EXE -or -not (Test-Path -LiteralPath ([string]$runtime.TESSERACT_EXE) -PathType Leaf)) {
        throw 'Enabled OCR acceptance requires a verified Tesseract executable.'
    }
    $profiles.Add('ocr-local')
    $env:HOTEL_AI_OCR_ENABLED = 'true'
    $env:TESSERACT_EXE = [string]$runtime.TESSERACT_EXE
} else {
    $env:HOTEL_AI_OCR_ENABLED = 'false'
}
if ($profiles.Count -gt 0) { $env:SPRING_PROFILES_ACTIVE = $profiles -join ',' }
else { Remove-Item Env:SPRING_PROFILES_ACTIVE -ErrorAction SilentlyContinue }
$env:HOTEL_AI_RAG_DEMO_CORPUS_ENABLED = $ragRuntime.Demo
$env:HOTEL_AI_RAG_DEMOCORPUS_ENABLED = $ragRuntime.Demo
$env:HOTEL_AI_RAG_STORETYPE = $ragRuntime.Mode
$env:HOTEL_AI_RAG_STORE_TYPE = $ragRuntime.Mode
foreach ($item in @('JDBC_URL', 'USERNAME', 'PASSWORD')) {
    $property = @{ JDBC_URL = 'Url'; USERNAME = 'Username'; PASSWORD = 'Password' }[$item]
    $value = if ($Component -eq 'backend') { $ragRuntime[$property] } else { $null }
    [Environment]::SetEnvironmentVariable("RAG_PGVECTOR_$item", $value, 'Process')
}

if ($Component -eq 'backend') {
    $backendDirectory = Join-Path $ProjectRoot 'backend'
    $mavenWrapper = Join-Path $backendDirectory 'mvnw.cmd'
    if (-not (Test-Path -LiteralPath $mavenWrapper)) {
        throw "Maven wrapper not found: $mavenWrapper"
    }

    Push-Location $backendDirectory
    try {
        $mavenArguments = @('spring-boot:run')
        if ($ragRuntime.JvmArguments) { $mavenArguments += "-Dspring-boot.run.jvmArguments=$($ragRuntime.JvmArguments)" }
        & $mavenWrapper @mavenArguments
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
# The shared API client gives VITE_API_BASE_URL precedence over VITE_API_URL.
# Override both so a developer's .env cannot redirect acceptance calls elsewhere.
$env:VITE_API_BASE_URL = $env:VITE_API_URL
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
