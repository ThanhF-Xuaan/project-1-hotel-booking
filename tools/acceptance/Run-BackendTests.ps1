# Internal stage of Verify.ps1: run Maven in a separate process, without Compose/app env.
[CmdletBinding()]
param([Parameter(Mandatory = $true)][string]$EvidenceDirectory)

$ErrorActionPreference = 'Stop'
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = (Resolve-Path (Join-Path $ScriptDir '../..')).Path
$RuntimeFile = Join-Path $ScriptDir '.runtime/env.json'
$Config = Get-Content -LiteralPath $RuntimeFile -Raw | ConvertFrom-Json

if ($Config.DB_HOST -ne 'localhost' -or $Config.DB_PORT -ne 15432 -or
    $Config.DB_NAME -ne 'hotel_acceptance_db' -or $Config.DB_USER -ne 'postgres' -or
    $Config.REDIS_HOST -ne 'localhost' -or $Config.REDIS_PORT -ne 16379) {
    throw 'Backend IT chỉ được dùng DB/Redis acceptance cố định trên loopback.'
}
if (Test-Path -LiteralPath (Join-Path $ScriptDir '.runtime/processes.json')) {
    throw 'Phải dừng ứng dụng acceptance trước khi chạy PostgreSQL IT.'
}
foreach ($Port in @([int]$Config.BACKEND_PORT, [int]$Config.FRONTEND_PORT)) {
    if (Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue) {
        throw "Cổng ứng dụng $Port đang được dùng; chưa xác nhận DB exclusive."
    }
}
if (-not (Test-Path -LiteralPath $EvidenceDirectory -PathType Container)) {
    throw 'Thiếu thư mục evidence do Verify.ps1 tạo.'
}
$EvidenceFile = Join-Path $EvidenceDirectory 'backend.json'
if (Test-Path -LiteralPath $EvidenceFile) { throw 'Không dùng lại evidence Maven cũ.' }

# Verify the Docker services really publish these fixed ports under the acceptance project.
foreach ($Service in @(
    @{ Name = 'acceptance_postgres'; InternalPort = 5432; Port = 15432 },
    @{ Name = 'acceptance_redis'; InternalPort = 6379; Port = 16379 }
)) {
    $Container = @(docker ps --filter 'label=com.docker.compose.project=hotel-m1-acceptance' `
        --filter "label=com.docker.compose.service=$($Service.Name)" --format '{{.ID}}')
    if ($LASTEXITCODE -ne 0 -or $Container.Count -ne 1) { throw "Không xác minh được $($Service.Name)." }
    $PublishedPort = docker port $Container[0] "$($Service.InternalPort)/tcp"
    if ($LASTEXITCODE -ne 0 -or $PublishedPort -ne "127.0.0.1:$($Service.Port)") {
        throw "Port binding không khớp cho $($Service.Name)."
    }
    if ($Service.Name -eq 'acceptance_postgres') { $PgContainer = $Container[0] }
}
$OtherConnections = docker exec $PgContainer psql -v ON_ERROR_STOP=1 -U postgres -d hotel_acceptance_db -Atc `
    'SELECT count(*) FROM pg_stat_activity WHERE datname=current_database() AND pid<>pg_backend_pid();'
if ($LASTEXITCODE -ne 0 -or [string]$OtherConnections -ne '0') { throw 'DB acceptance còn connection khác; không chạy IT exclusive.' }

foreach ($Item in @(Get-ChildItem Env:)) {
    if ($Item.Name -match '^(DB_|REDIS_|SPRING_|HOTEL_|GEMINI_|T05_EXPORT_IT|T12_REDIS_)') {
        Remove-Item -LiteralPath "Env:$($Item.Name)"
    }
}
$env:T05_EXPORT_IT = 'true'
$env:T05_EXPORT_IT_EXCLUSIVE_VERIFIED = 'true'
$env:T05_EXPORT_IT_DB_HOST = '127.0.0.1'
$env:T05_EXPORT_IT_DB_PORT = '15432'
$env:T05_EXPORT_IT_DB_NAME = 'hotel_acceptance_db'
$env:T05_EXPORT_IT_DB_USER = 'postgres'
$env:T05_EXPORT_IT_DB_PASSWORD = [string]$Config.DB_PASSWORD
$env:T12_REDIS_IT = 'true'
$env:T12_REDIS_HOST = '127.0.0.1'
$env:T12_REDIS_PORT = '16379'
$env:T12_REDIS_EXCLUSIVE_VERIFIED = 'true'

Push-Location (Join-Path $ProjectRoot 'backend')
try {
    & .\mvnw.cmd clean test
    if ($LASTEXITCODE -ne 0) { throw "Maven clean test thất bại (exit $LASTEXITCODE)." }

    # Run the otherwise-disabled full-application context smoke test separately so its sandbox
    # credentials cannot affect unit tests that intentionally verify missing-secret behavior.
    $env:DB_HOST = '127.0.0.1'
    $env:DB_PORT = [string]$Config.DB_PORT
    $env:DB_NAME = [string]$Config.DB_NAME
    $env:DB_USER = [string]$Config.DB_USER
    $env:DB_PASSWORD = [string]$Config.DB_PASSWORD
    $env:REDIS_HOST = '127.0.0.1'
    $env:REDIS_PORT = [string]$Config.REDIS_PORT
    $env:KEYCLOAK_SERVER_URL = [string]$Config.KEYCLOAK_SERVER_URL
    $env:KEYCLOAK_REALM = [string]$Config.KEYCLOAK_REALM
    $env:KEYCLOAK_CLIENT_ID = [string]$Config.KEYCLOAK_CLIENT_ID
    $env:KEYCLOAK_CLIENT_SECRET = [string]$Config.KEYCLOAK_CLIENT_SECRET
    $env:KEYCLOAK_ISSUER_URI = "$($Config.KEYCLOAK_SERVER_URL)/realms/$($Config.KEYCLOAK_REALM)"
    $env:KEYCLOAK_JWK_SET_URI = "$($Config.KEYCLOAK_SERVER_URL)/realms/$($Config.KEYCLOAK_REALM)/protocol/openid-connect/certs"
    $env:KEYCLOAK_ADMIN_USERNAME = [string]$Config.KEYCLOAK_ADMIN
    $env:KEYCLOAK_ADMIN_PASSWORD = [string]$Config.KEYCLOAK_ADMIN_PASSWORD
    $env:HOTEL_SCHEDULING_ENABLED = 'false'
    $env:HOTEL_AI_ADVISOR_SECURITY_ANON_TOKEN_SECRET = [string]$Config.HOTEL_AI_ADVISOR_SECURITY_ANON_TOKEN_SECRET
    $env:HOTEL_AI_PERSONALIZATION_SYNTHETIC_EVALUATION_ENABLED = 'true'
    $env:HOTEL_AI_PERSONALIZATION_LOCAL_DEMO_ENABLED = 'true'
    $env:SPRING_LIQUIBASE_ENABLED = 'false'
    & .\mvnw.cmd '-Dacceptance.integration=true' '-Dtest=HotelBookingApplicationTests' test
    if ($LASTEXITCODE -ne 0) { throw "Application context smoke test thất bại (exit $LASTEXITCODE)." }

    $Reports = @(Get-ChildItem -LiteralPath 'target/surefire-reports' -Filter 'TEST-*.xml' | ForEach-Object {
        $Suite = ([xml](Get-Content -LiteralPath $_.FullName -Raw)).testsuite
        [PSCustomObject]@{ name = [string]$Suite.name; tests = [int]$Suite.tests; failures = [int]$Suite.failures;
            errors = [int]$Suite.errors; skipped = [int]$Suite.skipped }
    })
    if ($Reports.Count -eq 0) { throw 'Maven không tạo Surefire evidence.' }
    foreach ($Required in @(
        @{ Name = 'LodgingExportPostgresTest'; Count = 3 },
        @{ Name = 'RedisRoomAdvisorSessionStoreRealRedisTest'; Count = 4 },
        @{ Name = 'HotelBookingApplicationTests'; Count = 1 }
    )) {
        $Suite = @($Reports | Where-Object { $_.name.EndsWith(".$($Required.Name)") })
        if ($Suite.Count -ne 1 -or $Suite[0].tests -ne $Required.Count -or
            $Suite[0].skipped -ne 0 -or $Suite[0].failures -ne 0 -or $Suite[0].errors -ne 0) {
            throw "Thiếu PASS thực chạy cho $($Required.Name)."
        }
    }
    $Failures = ($Reports | Measure-Object failures -Sum).Sum + ($Reports | Measure-Object errors -Sum).Sum
    if ($Failures -ne 0) { throw 'Surefire còn failure/error.' }
    [PSCustomObject]@{ status = 'PASS'; command = '.\mvnw.cmd clean test; .\mvnw.cmd -Dacceptance.integration=true -Dtest=HotelBookingApplicationTests test';
        tests = ($Reports | Measure-Object tests -Sum).Sum; skipped = ($Reports | Measure-Object skipped -Sum).Sum;
        suites = $Reports } | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath $EvidenceFile -Encoding UTF8
} finally {
    Pop-Location
}
