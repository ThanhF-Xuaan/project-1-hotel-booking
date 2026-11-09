# PowerShell 7.5+, Docker Desktop, Java 21+, Maven and Node.js.
[CmdletBinding()]
param([switch]$BackendOnly, [switch]$EnableGemini, [switch]$Stop)

$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$source = 'hotel_postgres'
$db = 'm1-realdata-pilot-pg'
$api = 'm1-realdata-pilot-api'
$label = 'vn.edu.utc.hotel-booking.member1=real-data-pilot'
$baseUrl = 'http://127.0.0.1:18082'
$uiUrl = 'http://127.0.0.1:13002'

if ($EnableGemini -and [string]::IsNullOrWhiteSpace($env:GEMINI_API_KEY)) {
    throw 'EnableGemini requires GEMINI_API_KEY in the current process environment.'
}

function Invoke-Docker {
    & docker @args
    if ($LASTEXITCODE -ne 0) { throw "Docker command failed: $($args[0])" }
}

function Test-OwnedContainer([string]$Name) {
    $match = Invoke-Docker ps -a --filter "name=^/$Name$" --filter "label=$label" --format '{{.Names}}'
    return $match -ceq $Name
}

function Wait-Ready([scriptblock]$Probe, [string]$Message) {
    for ($attempt = 0; $attempt -lt 90; $attempt++) {
        if (& $Probe) { return }
        Start-Sleep -Seconds 2
    }
    throw $Message
}

function Read-SourceJson([string]$Database, [string]$Query) {
    $lines = @("BEGIN READ ONLY; SET LOCAL statement_timeout='10s'; $Query ROLLBACK;" |
        docker exec -i $source sh -c 'exec psql -XqAt -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$1"' source $Database)
    if ($LASTEXITCODE -ne 0 -or $lines.Count -ne 1 -or $lines[0] -cnotmatch '^[A-Za-z0-9+/=]+$') {
        throw "Could not read the authorized snapshot from $Database."
    }
    return [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($lines[0])) |
        ConvertFrom-Json -Depth 25 -DateKind String
}

function Invoke-CloneSql([string]$Database, [string]$Sql) {
    $Sql | docker exec -i $db psql -Xq -v ON_ERROR_STOP=1 -U postgres -d $Database
    if ($LASTEXITCODE -ne 0) { throw "Could not initialize isolated database $Database." }
}

function Encode-Json($Value) {
    [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes(
        (ConvertTo-Json -InputObject $Value -Depth 25 -Compress)))
}

if ($Stop) {
    foreach ($name in @($api, $db)) {
        if (Test-OwnedContainer $name) {
            if ((Invoke-Docker inspect --format '{{.State.Running}}' $name) -eq 'true') {
                Invoke-Docker stop --timeout 10 $name | Out-Null
            }
            if ($name -eq $api) { Invoke-Docker rm $name | Out-Null }
        }
    }
    return
}

if (!(Test-OwnedContainer $api) -or !(Test-OwnedContainer $db)) {
    $existing = @(Invoke-Docker ps -a --format '{{.Names}}')
    if ($db -in $existing -or $api -in $existing) { throw 'Pilot containers already exist. Use -Stop before restarting.' }
    if ((Invoke-Docker port $source '5432/tcp') -cne '127.0.0.1:5433') {
        throw 'Source is not the local hotel PostgreSQL binding.'
    }
    $core = Read-SourceJson 'hotel_booking_db' @'
SELECT replace(encode(convert_to(jsonb_build_object('hotel',to_jsonb(h),'region',to_jsonb(r))::text,'UTF8'),'base64'),chr(10),'')
FROM hotels h JOIN regions r ON h.region_id=r.id
WHERE h.id=4 AND h.status='ACTIVE' AND NOT h.is_deleted
  AND h.created_at=TIMESTAMPTZ '2026-11-05T14:02:47.505591Z';
'@
    # Export only public rows. Exclude embedding so the source need not load vector.so.
    $rag = Read-SourceJson 't12_pgvector_pilot_hl' @'
WITH sources AS (
  SELECT * FROM rag_sources WHERE hotel_id=4 AND scope_type='HOTEL_SCOPED'
    AND source_id IN ('VP-HL-DELUXE','VP-HL-DINING','VP-HL-FACILITIES','VP-HL-MEETING','VP-HL-WEDDING')
), documents AS (
  SELECT d.* FROM rag_document_versions d JOIN sources s ON s.id=d.source_key
  WHERE d.version=1 AND d.status='ACTIVE'
)
SELECT replace(encode(convert_to(jsonb_build_object(
 'config',(SELECT to_jsonb(t) FROM rag_store_config t WHERE id=1),
 'sources',(SELECT jsonb_agg(to_jsonb(s) ORDER BY s.source_id) FROM sources s),
 'documents',(SELECT jsonb_agg(to_jsonb(d) ORDER BY d.source_key) FROM documents d),
 'chunks',(SELECT jsonb_agg(to_jsonb(c) ORDER BY c.document_id,c.chunk_index)
           FROM (SELECT c.id,c.document_id,c.chunk_index,c.embedding_dimension,c.payload,c.created_at,c.updated_at
                 FROM rag_chunks c JOIN documents d ON d.id=c.document_id) c)
)::text,'UTF8'),'base64'),chr(10),'');
'@
    $approved = @{
        'VP-HL-DELUXE' = '60d9e2c09cc5298914f7446d7c445fb820cb768c2505e9a91cfd9da1c238a363'
        'VP-HL-DINING' = '3bbd12cafce8964eca6d4fc820039e53552a4745f09e6305563743082caab496'
        'VP-HL-FACILITIES' = 'efe288dd6a3bf6abd7c63d8f657a6767ad7da1e5ae124114f64d1caf0023751c'
        'VP-HL-MEETING' = '0e8661c740299809e48ef936c799900fae8df1c3b5ccf9d2ddc49bb77a2582b5'
        'VP-HL-WEDDING' = 'fe0965aca6b24977419ce6878cf77b2bf32f2e2fe07251e6594cc4cade790f3d'
    }
    if ($rag.config.embedding_model_id -cne 'local-multilingual-hashing-v1' -or $rag.config.embedding_dimension -ne 256 -or
        @($rag.sources).Count -ne 5 -or @($rag.documents).Count -ne 5 -or @($rag.chunks).Count -ne 5) {
        throw 'Unexpected corpus model or row count.'
    }
    $chunksBySource = @{}
    $embeddingInput = foreach ($entry in $rag.sources) {
        if ($entry.hotel_id -ne 4 -or $entry.scope_type -cne 'HOTEL_SCOPED' -or
            !$approved.ContainsKey($entry.source_id) -or $chunksBySource.ContainsKey($entry.source_id)) {
            throw 'Unapproved source or scope.'
        }
        $documents = @($rag.documents | Where-Object source_key -eq $entry.id)
        if ($documents.Count -ne 1) { throw 'Expected one version per source.' }
        $document = $documents[0]
        $chunks = @($rag.chunks | Where-Object document_id -eq $document.id)
        $normalized = ([string]$document.payload.rawContent).Replace("`r`n","`n").Replace("`r","`n").Normalize()
        $hash = [Convert]::ToHexString([Security.Cryptography.SHA256]::HashData([Text.Encoding]::UTF8.GetBytes($normalized))).ToLowerInvariant()
        if ($document.status -cne 'ACTIVE' -or $document.version -ne 1 -or $document.embedding_dimension -ne 256 -or
            $document.content_hash -cne $hash -or $approved[$entry.source_id] -cne $hash -or
            $chunks.Count -ne 1 -or $chunks[0].embedding_dimension -ne 256 -or
            ([string]$chunks[0].payload.content).Trim() -cne ([string]$document.payload.rawContent).Trim()) {
            throw 'Source content, chunk, or version differs from the approved pilot snapshot.'
        }
        $chunksBySource[$entry.source_id] = $chunks[0]
        $fields = @($document.payload.title, $chunks[0].payload.content, $chunks[0].payload.sanitizedContent) |
            ForEach-Object { [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes([string]$_)) }
        $fields -join [char]9
    }

    Push-Location (Join-Path $root 'backend')
    try {
        & mvn.cmd -B '-Dmaven.test.skip=true' package
        if ($LASTEXITCODE -ne 0) { throw 'Backend package failed.' }
    } finally { Pop-Location }
    $jar = (Resolve-Path (Join-Path $root 'backend/target/hotel-booking-system-0.0.1-SNAPSHOT.jar')).Path
    $classes = Join-Path $root 'backend/target/classes'
    $vectors = @($embeddingInput | & java --class-path $classes (Join-Path $PSScriptRoot 'RealDataPilotEmbedding.java'))
    if ($LASTEXITCODE -ne 0 -or $vectors.Count -ne 5) { throw 'Pilot vector rebuild failed.' }
    for ($index = 0; $index -lt $vectors.Count; $index++) {
        $chunksBySource[$rag.sources[$index].source_id] | Add-Member -NotePropertyName embedding -NotePropertyValue $vectors[$index]
    }

    $dbId = $null
    $apiId = $null
    try {
        $password = [Guid]::NewGuid().ToString('N')
        $dbId = Invoke-Docker run -d --rm --pull never --name $db --label $label -p '127.0.0.1:18082:18082' `
            -e "POSTGRES_PASSWORD=$password" -e 'POSTGRES_DB=m1_pilot_core' pgvector/pgvector:pg16
        Wait-Ready { docker exec $db pg_isready -q -h 127.0.0.1 -U postgres -d m1_pilot_core 2>$null; $LASTEXITCODE -eq 0 } 'Isolated PostgreSQL did not start.'
        $resources = Join-Path $root 'backend/src/main/resources'
        Invoke-Docker run --rm --pull never --network "container:$db" --mount "type=bind,source=$resources,target=/project,readonly" `
            liquibase/liquibase:4.31-alpine '--search-path=/project' '--changelog-file=db/db-changelog-root.xml' `
            '--url=jdbc:postgresql://127.0.0.1:5432/m1_pilot_core' '--username=postgres' update '--context-filter=master-data' | Out-Host

        $coreData = Encode-Json $core
        Invoke-CloneSql 'm1_pilot_core' @"
BEGIN;
DO `$`$ BEGIN
    IF EXISTS (SELECT 1 FROM hotels) OR EXISTS (SELECT 1 FROM hotel_room_types)
       OR EXISTS (SELECT 1 FROM databasechangelog WHERE contexts ILIKE '%dev-seed%') THEN
        RAISE EXCEPTION 'Unexpected hotel, room or seed data in pilot';
    END IF;
END `$`$;
CREATE TEMP TABLE pilot_input AS SELECT convert_from(decode('$coreData','base64'),'UTF8')::jsonb AS data;
INSERT INTO regions OVERRIDING SYSTEM VALUE SELECT (jsonb_populate_record(NULL::regions,data->'region')).* FROM pilot_input;
INSERT INTO hotels OVERRIDING SYSTEM VALUE SELECT (jsonb_populate_record(NULL::hotels,data->'hotel')).* FROM pilot_input;
CREATE ROLE m1_core_reader LOGIN;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO m1_core_reader;
COMMIT;
"@
        $pilotData = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'Seed-T12PilotData.sql') -Raw
        Invoke-CloneSql 'm1_pilot_core' $pilotData
        Invoke-CloneSql 'postgres' 'CREATE DATABASE t12_pgvector_real_pilot;'
        $ragData = Encode-Json $rag
        $schema = Get-Content -LiteralPath (Join-Path $resources 'db/sqlFile/incremental/2026/20261030-230000-create-rag-pgvector-store.sql') -Raw
        Invoke-CloneSql 't12_pgvector_real_pilot' @"
BEGIN;
CREATE EXTENSION vector;
CREATE TABLE hotels (id smallint PRIMARY KEY);
INSERT INTO hotels VALUES (4);
CREATE FUNCTION update_updated_at_column() RETURNS trigger LANGUAGE plpgsql AS 'BEGIN NEW.updated_at = now(); RETURN NEW; END;';
$schema
CREATE TEMP TABLE pilot_input AS SELECT convert_from(decode('$ragData','base64'),'UTF8')::jsonb AS data;
INSERT INTO rag_store_config SELECT (jsonb_populate_record(NULL::rag_store_config,data->'config')).* FROM pilot_input;
INSERT INTO rag_sources (id,scope_type,hotel_id,source_id,created_at,updated_at)
SELECT s.id,s.scope_type,s.hotel_id,s.source_id,s.created_at,s.updated_at
FROM pilot_input CROSS JOIN LATERAL jsonb_populate_recordset(NULL::rag_sources,data->'sources') s;
INSERT INTO rag_document_versions SELECT d.* FROM pilot_input
CROSS JOIN LATERAL jsonb_populate_recordset(NULL::rag_document_versions,data->'documents') d;
INSERT INTO rag_chunks SELECT c.* FROM pilot_input
CROSS JOIN LATERAL jsonb_populate_recordset(NULL::rag_chunks,data->'chunks') c;
CREATE ROLE m1_rag_reader LOGIN;
GRANT SELECT ON rag_store_config,rag_sources,rag_document_versions,rag_chunks TO m1_rag_reader;
COMMIT;
"@
        $settings = @(
            'BACKEND_PORT=18082', 'SPRING_PROFILES_ACTIVE=rag-pgvector-local',
            'SPRING_DATASOURCE_URL=jdbc:postgresql://127.0.0.1:5432/m1_pilot_core',
            'SPRING_DATASOURCE_USERNAME=m1_core_reader', 'SPRING_DATASOURCE_PASSWORD=local-trust-placeholder',
            'SPRING_DATASOURCE_HIKARI_READ_ONLY=true', 'SPRING_JPA_SHOW_SQL=false',
            'RAG_PGVECTOR_JDBC_URL=jdbc:postgresql://127.0.0.1:5432/t12_pgvector_real_pilot',
            'RAG_PGVECTOR_USERNAME=m1_rag_reader', 'RAG_PGVECTOR_PASSWORD=local-trust-placeholder',
            'SPRING_LIQUIBASE_ENABLED=false', 'HOTEL_SCHEDULING_ENABLED=false',
            'HOTEL_STORAGE_S3_AUTO_CREATE_BUCKET=false', 'HOTEL_AI_ADVISOR_PROVIDER_GEMINI_ENABLED=false',
            'HOTEL_AI_RAG_DEMO_CORPUS_ENABLED=false', "CORS_ALLOWED_ORIGINS=$uiUrl,http://localhost:13002"
        )
        if ($EnableGemini) {
            $settings += 'HOTEL_AI_RAG_GENERATION_GEMINI_ENABLED=true'
        }
        $run = @('run','-d','--pull','never','--name',$api,'--label',$label,'--network',"container:$db",
            '--read-only','--tmpfs','/tmp:rw,nosuid,size=256m','--mount',"type=bind,source=$jar,target=/m1-pilot.jar,readonly")
        foreach ($setting in $settings) { $run += @('-e',$setting) }
        if ($EnableGemini) { $run += @('-e','GEMINI_API_KEY') }
        $run += @('--entrypoint','java','project-1-hotel-booking-backend:latest','-Duser.timezone=Asia/Ho_Chi_Minh','-jar','/m1-pilot.jar')
        $apiId = Invoke-Docker @run
        Wait-Ready {
            if ((Invoke-Docker inspect --format '{{.State.Running}}' $apiId) -ne 'true') {
                throw 'Isolated backend exited during startup.'
            }
            try { $null=Invoke-RestMethod "$baseUrl/v3/api-docs" -TimeoutSec 4; $true } catch { $false }
        } 'Isolated backend did not start.'
    } catch {
        if ($apiId) { docker logs --tail 30 $apiId; docker stop $apiId | Out-Null; docker rm $apiId | Out-Null }
        if ($dbId) { docker stop $dbId | Out-Null }
        throw
    }
} elseif ((Invoke-Docker inspect --format '{{.State.Running}}' $api) -ne 'true' -or
          (Invoke-Docker inspect --format '{{.State.Running}}' $db) -ne 'true') {
    throw 'Pilot is stopped. Use -Stop before restarting.'
}

Write-Host "API: $baseUrl/swagger-ui/index.html"
if ($BackendOnly) { return }
Push-Location (Join-Path $root 'frontend')
try {
    $env:VITE_API_BASE_URL = $baseUrl
    $env:VITE_API_URL = $baseUrl
    if (!(Test-Path 'node_modules/.bin/vite.cmd')) {
        & npm.cmd ci
        if ($LASTEXITCODE -ne 0) { throw 'Frontend dependency install failed.' }
    }
    Write-Host "UI: $uiUrl/t12-pilot.html"
    & npm.cmd run dev -- --host 127.0.0.1 --port 13002 --strictPort
    if ($LASTEXITCODE -ne 0) { throw 'Frontend startup failed.' }
} finally { Pop-Location }
