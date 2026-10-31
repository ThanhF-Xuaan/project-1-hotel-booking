# U14 opt-in suites require distinct empty databases; never clone the seeded main DB.
[CmdletBinding()]
param([Parameter(Mandatory = $true)][string]$EvidenceDirectory)
$ErrorActionPreference = 'Stop'
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
. (Join-Path $ScriptDir 'Runtime.ps1')
$Runtime = Initialize-AcceptanceRuntime -ScriptDirectory $ScriptDir
$Config = $Runtime.Environment
if ($Config.DB_HOST -ne 'localhost' -or $Config.DB_PORT -ne 15432 -or $Config.DB_NAME -ne 'hotel_acceptance_db') { throw 'Unexpected sandbox target.' }
$Container = @(docker ps --filter 'label=com.docker.compose.project=hotel-m1-acceptance' --filter 'label=com.docker.compose.service=acceptance_postgres' --format '{{.ID}}')
if ($Container.Count -ne 1 -or (docker port $Container[0] '5432/tcp') -ne '127.0.0.1:15432') { throw 'Cannot verify disposable PostgreSQL container.' }
$Suffix = (Get-Date).ToString('yyyyMMddHHmmss') + '_' + [Guid]::NewGuid().ToString('N').Substring(0, 8)
$ConsentDb = "u14_i02_final_$Suffix"
$HistoryDb = "u14_i03_final_$Suffix"
$Created = [System.Collections.Generic.List[string]]::new()
try {
    foreach ($Database in @($ConsentDb, $HistoryDb)) {
        if ($Database -notmatch '^u14_i0[23]_final_[a-z0-9_]+$') { throw 'Invalid disposable database name.' }
        $Exists = docker exec $Container[0] psql -U postgres -d postgres -Atc "SELECT count(*) FROM pg_database WHERE datname='$Database';"
        if ($LASTEXITCODE -ne 0 -or [string]$Exists -ne '0') { throw 'Disposable database already exists.' }
        docker exec $Container[0] createdb -U postgres -T template0 $Database
        if ($LASTEXITCODE -ne 0) { throw 'Cannot create empty integration database.' }
        $Created.Add($Database)
    }
    foreach ($Item in @(Get-ChildItem Env:)) {
        if ($Item.Name -match '^(DB_|REDIS_|SPRING_|HOTEL_|GEMINI_|GOOGLE_|U14_|T12_)') { Remove-Item -LiteralPath "Env:$($Item.Name)" }
    }
    $env:DB_HOST = '127.0.0.1'
    $env:DB_PORT = '15432'
    $env:DB_NAME = $ConsentDb
    $env:DB_USER = 'postgres'
    $env:DB_PASSWORD = [string]$Config.DB_PASSWORD
    $env:REDIS_HOST = '127.0.0.1'
    $env:REDIS_PORT = '16379'
    $env:KEYCLOAK_SERVER_URL = [string]$Config.KEYCLOAK_SERVER_URL
    $env:KEYCLOAK_REALM = [string]$Config.KEYCLOAK_REALM
    $env:KEYCLOAK_CLIENT_ID = [string]$Config.KEYCLOAK_CLIENT_ID
    $env:KEYCLOAK_CLIENT_SECRET = [string]$Config.KEYCLOAK_CLIENT_SECRET
    $env:HOTEL_AI_ADVISOR_SECURITY_ANON_TOKEN_SECRET = [string]$Config.HOTEL_AI_ADVISOR_SECURITY_ANON_TOKEN_SECRET
    $env:HOTEL_SCHEDULING_ENABLED = 'false'
    $env:U14_I02_IT = 'true'
    $env:U14_I03_IT = 'true'
    $env:U14_I03_JDBC_URL = "jdbc:postgresql://127.0.0.1:15432/$HistoryDb"
    $env:U14_I03_DB_USER = 'postgres'
    $env:U14_I03_DB_PASSWORD = [string]$Config.DB_PASSWORD
    Push-Location (Join-Path $Runtime.ProjectRoot 'backend')
    try {
        & .\mvnw.cmd '-Dtest=PersonalizationProviderPostgresIT,PersonalizationConsentPostgresIT' '-DfailIfNoTests=true' test
        if ($LASTEXITCODE -ne 0) { throw 'U14 PostgreSQL suites failed.' }
        $Results = foreach ($Class in @('PersonalizationProviderPostgresIT', 'PersonalizationConsentPostgresIT')) {
            $Files = @(Get-ChildItem target/surefire-reports -Filter "TEST-*.$Class.xml")
            if ($Files.Count -ne 1) { throw "Missing $Class evidence." }
            $Suite = ([xml](Get-Content -LiteralPath $Files[0].FullName -Raw)).testsuite
            if ([int]$Suite.tests -ne 3 -or [int]$Suite.skipped -ne 0 -or [int]$Suite.failures -ne 0 -or [int]$Suite.errors -ne 0) { throw "Incomplete $Class execution." }
            [PSCustomObject]@{ name = $Class; tests = 3; failures = 0; errors = 0; skipped = 0 }
        }
        [PSCustomObject]@{ status = 'PASS'; tests = 6; suites = @($Results); databases = @($Created.ToArray()); createdEmpty = $true } |
            ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $EvidenceDirectory 'additional-postgres.json') -Encoding UTF8
    } finally { Pop-Location }
} finally {
    # Only names created successfully in THIS process are eligible for cleanup.
    foreach ($Database in $Created) {
        docker exec $Container[0] dropdb -U postgres $Database
        if ($LASTEXITCODE -ne 0) { throw "Cleanup failed for owned disposable DB $Database." }
    }
}
