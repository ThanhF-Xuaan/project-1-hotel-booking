# Called only by Verify after its synthetic fixtures/tests and before its snapshot restore.
[CmdletBinding()]
param([Parameter(Mandatory = $true)][string]$EvidenceDirectory)
$ErrorActionPreference = 'Stop'
$ScriptDir = $PSScriptRoot
. (Join-Path $ScriptDir 'Runtime.ps1')
. (Join-Path $ScriptDir 'Rag-Runtime.ps1')
$runtime = Initialize-AcceptanceRuntime -ScriptDirectory $ScriptDir
$config = $runtime.Environment
$state = Join-Path $runtime.RuntimeDirectory 'processes.json'
if ($config.DB_HOST -ne 'localhost' -or $config.DB_PORT -ne 15432 -or
    $config.DB_NAME -ne 'hotel_acceptance_db' -or $config.BACKEND_PORT -ne 18080 -or
    $config.FRONTEND_PORT -ne 13000 -or $config.KEYCLOAK_SERVER_URL -ne 'http://localhost:18081' -or
    $config.KEYCLOAK_REALM -ne 'hotel-acceptance') { throw 'Unexpected main acceptance sandbox.' }
if (-not (Test-Path -LiteralPath (Join-Path $EvidenceDirectory 'snapshot.sql'))) { throw 'Verify snapshot required.' }
$EvidenceDirectory = (Resolve-Path -LiteralPath $EvidenceDirectory).Path
if (Test-Path -LiteralPath (Join-Path $EvidenceDirectory 'persistent-rag.json')) { throw 'Evidence already exists.' }
$main = @(docker ps --filter 'label=com.docker.compose.project=hotel-m1-acceptance' --filter 'label=com.docker.compose.service=acceptance_postgres' --format '{{.ID}}')
if ($LASTEXITCODE -ne 0 -or $main.Count -ne 1 -or
    (docker port $main[0] '5432/tcp') -ne '127.0.0.1:15432') { throw 'Main database ownership mismatch.' }
if (Get-NetTCPConnection -State Listen -LocalPort 15435 -ErrorAction SilentlyContinue) { throw 'Dedicated RAG port occupied.' }
$runId = [Guid]::NewGuid().ToString('N')
$database = "t12_pgvector_http_$runId"
$name = "m1-rag-http-$runId"
$source = "HTTP-$($runId.ToUpperInvariant())"
$image = 'pgvector/pgvector@sha256:c8483555ce48101872f888c1df8a895ff689d6c7c7a5f7ac266475f9dfe89e0b'
$password = [Convert]::ToHexString([Security.Cryptography.RandomNumberGenerator]::GetBytes(24))
$adminPassword = [Convert]::ToHexString([Security.Cryptography.RandomNumberGenerator]::GetBytes(24))
$previousPassword = [Environment]::GetEnvironmentVariable('POSTGRES_PASSWORD', 'Process')
$originalRuntime = [IO.File]::ReadAllBytes($runtime.RuntimeFile)
$container = $null
$snapshotReady = $false
$snapshotHash = $null
$restored = $false
$businessUnchanged = $false
$problems = [System.Collections.Generic.List[string]]::new()
$phases = [System.Collections.Generic.List[object]]::new()
$snapshot = Join-Path $EvidenceDirectory 'rag-before.sql'
$scriptHashes = @('Rag-Runtime.ps1', 'Run-Application.ps1', 'Test-PersistentRag.ps1', 'verify-persistent-rag.mjs') | ForEach-Object {
    @{ file = $_; sha256 = (Get-FileHash -LiteralPath (Join-Path $ScriptDir $_) -Algorithm SHA256).Hash }
}

function Query-Rag([string]$Sql) {
    $result = $Sql | docker exec -i $container psql -X -v ON_ERROR_STOP=1 -U postgres -d $database -At
    if ($LASTEXITCODE -ne 0) { throw 'Dedicated RAG SQL failed.' }
    return $result
}
function Digest([string]$ContainerId, [string]$DatabaseName) {
    $lines = @(docker exec $ContainerId pg_dump --data-only --no-owner --no-privileges -U postgres -d $DatabaseName)
    if ($LASTEXITCODE -ne 0) { throw 'Database digest failed.' }
    $text = ($lines | Where-Object { $_ -notmatch '^\\(?:un)?restrict ' }) -join "`n"
    return [Convert]::ToHexString([Security.Cryptography.SHA256]::HashData([Text.Encoding]::UTF8.GetBytes($text)))
}
function Stop-Apps {
    if (-not (Stop-AcceptanceTrackedProcesses -StateFile $state)) { throw 'Unverified application process; refuse restore.' }
    if (Get-NetTCPConnection -State Listen -LocalPort 18080, 13000 -ErrorAction SilentlyContinue) { throw 'Applications still listening.' }
}
function Start-Apps {
    if ([Environment]::GetEnvironmentVariable('POSTGRES_PASSWORD', 'Process') -eq $adminPassword) {
        throw 'Bootstrap credential must not enter application processes.'
    }
    & (Join-Path $ScriptDir 'Start.ps1') -ApplicationsOnly
}
function Wait-Rag {
    for ($attempt = 0; $attempt -lt 90; $attempt++) {
        # The image's temporary init server accepts Unix sockets before shutting down.
        # TCP becomes available only for the final server used by the application.
        docker exec $container pg_isready -h 127.0.0.1 -p 5432 -U postgres -d $database *> $null
        if ($LASTEXITCODE -eq 0) { return }
        Start-Sleep -Seconds 1
    }
    throw 'Dedicated RAG database did not become ready.'
}
function Phase([string]$Name) {
    & node (Join-Path $ScriptDir 'verify-persistent-rag.mjs') $Name $EvidenceDirectory $container $database $source
    if ($LASTEXITCODE -ne 0) { throw "Persistent RAG phase failed: $Name" }
    $report = Get-Content -LiteralPath (Join-Path $EvidenceDirectory "persistent-rag-$Name.json") -Raw | ConvertFrom-Json
    if ($report.status -ne 'PASS' -or $report.checks -lt 1) { throw 'Missing persistent RAG evidence.' }
    $phases.Add($report)
}
try {
    Stop-Apps
    $beforeBusiness = Digest $main[0] 'hotel_acceptance_db'
    $env:POSTGRES_PASSWORD = $adminPassword
    $container = (docker run -d --name $name --label "hotel.m1.rag-http=$database" --publish '127.0.0.1:15435:5432' `
        --mount "type=volume,source=$name-data,target=/var/lib/postgresql/data" `
        -e POSTGRES_PASSWORD -e "POSTGRES_DB=$database" $image).Trim()
    if ($LASTEXITCODE -ne 0 -or $container -notmatch '\A[a-f0-9]{64}\z') { throw 'Could not create dedicated RAG container.' }
    [Environment]::SetEnvironmentVariable('POSTGRES_PASSWORD', $previousPassword, 'Process')
    Wait-Rag
    if ((Query-Rag 'SELECT current_database()') -ne $database -or
        (Query-Rag "SELECT count(*) FROM information_schema.tables WHERE table_schema='public'") -ne '0') {
        throw 'Dedicated RAG target is not new and empty.'
    }
    $bootstrap = @'
CREATE EXTENSION vector;
CREATE TABLE hotels(id SMALLINT PRIMARY KEY);
INSERT INTO hotels(id) VALUES(1),(2);
CREATE FUNCTION update_updated_at_column() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN NEW.updated_at=now(); RETURN NEW; END $$;
REVOKE ALL ON DATABASE "DATABASE_NAME" FROM PUBLIC;
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
'@
    $null = Query-Rag ($bootstrap.Replace('DATABASE_NAME', $database))
    $migration = Join-Path $runtime.ProjectRoot 'backend/src/main/resources/db/sqlFile/incremental/2026/20261030-230000-create-rag-pgvector-store.sql'
    $null = Query-Rag ([IO.File]::ReadAllText($migration))
    $null = Query-Rag "CREATE ROLE rag_http_writer LOGIN PASSWORD '$password'; GRANT CONNECT ON DATABASE $database TO rag_http_writer; GRANT USAGE ON SCHEMA public TO rag_http_writer; GRANT SELECT,INSERT,UPDATE,DELETE ON rag_store_config,rag_sources,rag_document_versions,rag_chunks TO rag_http_writer; GRANT USAGE,SELECT ON SEQUENCE rag_sources_id_seq TO rag_http_writer;"
    $dump = @(docker exec $container pg_dump --clean --if-exists --no-owner --no-privileges -U postgres -d $database)
    if ($LASTEXITCODE -ne 0 -or $dump.Count -lt 1) { throw 'RAG snapshot failed.' }
    [IO.File]::WriteAllLines($snapshot, $dump, [Text.UTF8Encoding]::new($false))
    $snapshotHash = (Get-FileHash -LiteralPath $snapshot -Algorithm SHA256).Hash
    $beforeRag = Digest $container $database
    $snapshotReady = $true
    $config | Add-Member -NotePropertyMembers @{
        HOTEL_AI_RAG_STORE_TYPE = 'pgvector'; HOTEL_AI_RAG_DEMO_CORPUS_ENABLED = 'false';
        RAG_PGVECTOR_JDBC_URL = "jdbc:postgresql://127.0.0.1:15435/$database";
        RAG_PGVECTOR_USERNAME = 'rag_http_writer'; RAG_PGVECTOR_PASSWORD = $password
    } -Force
    $null = Resolve-AcceptanceRagRuntime $config
    [IO.File]::WriteAllText($runtime.RuntimeFile, ($config | ConvertTo-Json -Depth 8), [Text.UTF8Encoding]::new($false))
    Start-Apps
    Phase 'lifecycle'
    Stop-Apps
    Start-Apps
    Phase 'restart'
    docker stop --time 15 $container | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Cannot inject RAG outage safely.' }
    Phase 'outage'
    docker start $container | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Cannot restart owned RAG container.' }
    Wait-Rag
    Phase 'recovered'
} catch {
    $problems.Add($_.Exception.Message)
    Write-Warning $_.Exception.Message
} finally {
    try {
        Stop-Apps
        [IO.File]::WriteAllBytes($runtime.RuntimeFile, $originalRuntime)
        if ($container) {
            $identity = docker inspect $container | ConvertFrom-Json
            if ($LASTEXITCODE -ne 0 -or $identity[0].Config.Labels.'hotel.m1.rag-http' -ne $database) {
                throw 'RAG container identity changed; refuse cleanup.'
            }
            if (-not $identity[0].State.Running) { docker start $container | Out-Null; Wait-Rag }
            if ($snapshotReady) {
                if ((Get-FileHash -LiteralPath $snapshot -Algorithm SHA256).Hash -ne $snapshotHash) { throw 'RAG snapshot changed.' }
                if ((Query-Rag "SELECT count(*) FROM pg_stat_activity WHERE datname=current_database() AND pid<>pg_backend_pid()") -ne '0') {
                    throw 'Foreign RAG sessions; refuse restore.'
                }
                $null = Query-Rag ([IO.File]::ReadAllText($snapshot))
                $afterRag = Digest $container $database
                if ($afterRag -ne $beforeRag) { throw 'RAG data/sequence restore digest differs.' }
                $restored = $true
            }
            docker stop --time 15 $container | Out-Null
            if ($LASTEXITCODE -ne 0) { throw 'RAG container did not stop.' }
        }
        $afterBusiness = Digest $main[0] 'hotel_acceptance_db'
        $businessUnchanged = $beforeBusiness -eq $afterBusiness
        if (-not $businessUnchanged) { throw 'Persistent RAG flow changed main business data/sequence digest.' }
    } catch { $problems.Add("Cleanup: $($_.Exception.Message)"); Write-Warning $problems[$problems.Count - 1] }
    [Environment]::SetEnvironmentVariable('POSTGRES_PASSWORD', $previousPassword, 'Process')
}
foreach ($entry in $scriptHashes) {
    if ((Get-FileHash -LiteralPath (Join-Path $ScriptDir $entry.file) -Algorithm SHA256).Hash -ne $entry.sha256) {
        $problems.Add('Runtime verification source changed during execution.')
    }
}
$configRestored = [Convert]::ToHexString([IO.File]::ReadAllBytes($runtime.RuntimeFile)) -eq [Convert]::ToHexString($originalRuntime)
$status = if ($problems.Count -eq 0 -and $phases.Count -eq 4 -and $restored -and $businessUnchanged -and $configRestored) { 'PASS' } else { 'FAIL' }
$result = @{ status = $status; phases = @($phases.ToArray()); restored = $restored; businessUnchanged = $businessUnchanged;
    database = $database; container = $container; image = $image; snapshotSha256 = $snapshotHash;
    runtimeConfigRestored = $configRestored; scriptHashes = @($scriptHashes);
    beforeBusinessDigest = $beforeBusiness; afterBusinessDigest = $afterBusiness; problems = @($problems.ToArray());
    limitations = 'Synthetic corpus, local hashing embeddings; no production or paid provider evidence.' }
$result | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath (Join-Path $EvidenceDirectory 'persistent-rag.json') -Encoding UTF8
if ($status -ne 'PASS') { throw 'Persistent RAG verification failed; see sanitized phase reports and runtime logs.' }
Write-Host 'PASS: full persistent RAG HTTP/browser, JVM restart, outage/recovery and digest restore.'
