<#
.SYNOPSIS
Dedicated, opt-in Gemini sandbox. Never calls Run-Application.ps1 or starts a frontend.
.DESCRIPTION
Requires PowerShell 7, a reviewed packaged backend, an already approved synthetic primary
acceptance DB/read-only role, private NTFS runtime/secret files and external owner approvals.
Template/Plan are offline. Provision requires G04 plus -ApproveProvision. Start/Restart require
G04/G05/G09 plus -AllowLive. G06 is additionally required by Test-RagGeminiLive.ps1 for ingestion.
Approvals JSON: {approvedConfigSha256:..., gates:{G04:{status:'APPROVED',reviewer:...,
reviewerRole:'Member 2',reference:...},...}}. This script never creates human approvals.
The policy format is enforced by RagOutboundLedger; pricing/bounds must be reviewed for the exact
models. Store pricing-snapshot.html next to the immutable policy. Unknown usage stops the campaign.
The secret JSON contains ragPassword, adminPassword, primaryPassword, geminiKey,
keycloakClientSecret and anonTokenSecret. It must be private and outside the repository.
Stop/Restore only affect verified state-owned processes/container. Volumes and the cost ledger
are retained. Restore reverts the pre-ingestion RAG snapshot; it never resets campaign spending.
.EXAMPLE
./Start-RagGeminiDemo.ps1 -Action Template
.EXAMPLE
./Start-RagGeminiDemo.ps1 -Action Plan -ConfigurationFile C:/private/demo.json
#>
[CmdletBinding()]
param(
    [ValidateSet('Template','PolicyTemplate','ApprovalTemplate','Plan','Provision','Start','Restart','Stop','Snapshot','Restore','Inspect','DbOutage','DbRecovery','Supervisor')]
    [string]$Action = 'Plan',
    [string]$ConfigurationFile,
    [string]$ApprovalFile,
    [switch]$ApproveProvision,
    [switch]$AllowLive
)
$ErrorActionPreference = 'Stop'
$script:RagRepo = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$script:RagDdl = Join-Path $script:RagRepo 'backend/src/main/resources/db/sqlFile/incremental/2026/20261030-230000-create-rag-pgvector-store.sql'
$script:RagProfile = Join-Path $script:RagRepo 'backend/src/main/resources/application-rag-gemini-local.yaml'

function Get-RagHash([string]$Path) { return (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash.ToLowerInvariant() }
function Get-RagTextHash([string]$Text) { return [Convert]::ToHexString([Security.Cryptography.SHA256]::HashData([Text.Encoding]::UTF8.GetBytes($Text))).ToLowerInvariant() }
function Read-RagJson([string]$Path) {
    try {
        if (-not [IO.Path]::IsPathFullyQualified($Path) -or (Get-Item -LiteralPath $Path).Length -gt 65536) { throw 'invalid' }
        return [IO.File]::ReadAllText($Path) | ConvertFrom-Json -AsHashtable -Depth 20
    } catch { throw 'RAG_JSON_INVALID: missing, oversized or invalid private configuration.' }
}
function Get-RagTemplate {
    return [ordered]@{
        SchemaVersion='t12-rag-gemini-sandbox-v1'; RunId=('a'*32); CampaignId=('b'*32)
        RuntimeDirectory='C:/private/hotel-rag-gemini'; SecretFile='C:/private/hotel-rag-gemini/secrets.json'
        RagDatabase=('t12_pgvector_gemini_'+('a'*32)); RagRole=('rag_gemini_'+('a'*32)); RagPort=15436; BackendPort=18090
        Image='pgvector/pgvector@sha256:REPLACE_WITH_MEMBER2_APPROVED_DIGEST'
        DockerContext='desktop-linux'
        PrimaryContainerId='REPLACE_WITH_APPROVED_64_HEX_CONTAINER_ID'; PrimaryReaderRole='m1_gemini_reader_REPLACE'
        KeycloakUrl='http://127.0.0.1:18081'; KeycloakRealm='hotel-acceptance'; KeycloakClientId='hotel-backend'; RedisPort=16379
        JavaExecutable='REPLACE_WITH_ABSOLUTE_JAVA21_EXE'; BackendJar='REPLACE_WITH_REVIEWED_JAR'; BackendJarSha256=''
        ChatModel=''; EmbeddingModel=''; EmbeddingDimension=0; RetrievalThreshold=0.30; HotelIds=@(1,2)
        Manifest=(Join-Path $script:RagRepo 'eval/t12/r2/gemini-v1/manifest.json'); ManifestSha256=''
        PolicyFile='C:/private/hotel-rag-gemini/policy.json'; PolicySha256=''
        DdlSha256=(Get-RagHash $script:RagDdl); ProfileSha256=(Get-RagHash $script:RagProfile); ProvisionTemplateSha256=''
        Note='DRAFT / NOT APPROVED. Fill exact models, price/bounds, hashes and owner-reviewed target before use.'
    }
}
function Get-RagPolicyTemplate {
    return [ordered]@{
        schemaVersion='rag-gemini-ledger-v1';campaignId=('b'*32);currency='USD';limitUsd='5'
        capturedAtUtc='';expiresAtUtc='';pricingSource='https://ai.google.dev/gemini-api/docs/pricing';pricingSnapshotSha256=''
        allBillableTokensCovered=$false;maxEmbeddingInputs=200;maxGenerationCalls=120
        embedding=@{model='';inputTokenBound=0;inputUsdPerMillion='';outputUsdPerMillion='0';boundsAndWorstCaseRatesApproved=$false}
        generation=@{model='';inputTokenBound=0;outputTokenBound=768;inputUsdPerMillion='';outputUsdPerMillion='';boundsAndWorstCaseRatesApproved=$false}
        note='DRAFT / NOT APPROVED. Use reviewed full model input-context bounds and worst billable token rates including thinking/tiering. Only token-based billing is supported. Snapshot must be exact current pricing bytes, expiry <=24h. Unknown usage halts; never reset the journal to resume.'
    }
}
function Get-RagApprovalTemplate {
    $gates=@{}
    foreach($id in @('G04','G05','G06','G09')) { $gates[$id]=@{status='PENDING';reviewer='';reviewerRole=$(if($id -eq 'G04'){'Member 2'}else{''});reference=''} }
    return @{approvedConfigSha256='';gates=$gates;note='Template only; every approval requires a real owner decision for the locked configuration/hash.'}
}
function Assert-RagPrivate([string]$Path, [switch]$Directory) {
    $full = [IO.Path]::GetFullPath($Path)
    if ($full.StartsWith($script:RagRepo+[IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase) -or
        -not (Test-Path -LiteralPath $full) -or (Get-Item -LiteralPath $full).LinkType) { throw 'RAG_PRIVATE_PATH_REQUIRED' }
    if (-not $IsWindows) { throw 'RAG_PRIVATE_ACL_CHECK_REQUIRES_WINDOWS' }
    $current = [Security.Principal.WindowsIdentity]::GetCurrent().User.Value
    foreach ($entry in (Get-Acl -LiteralPath $full).Access) {
        $sid = $entry.IdentityReference.Translate([Security.Principal.SecurityIdentifier]).Value
        if ($entry.AccessControlType -eq 'Allow' -and $sid -notin @($current,'S-1-5-18','S-1-5-32-544')) { throw 'RAG_PRIVATE_ACL_TOO_BROAD' }
    }
    if ($Directory -and -not (Test-Path -LiteralPath $full -PathType Container)) { throw 'RAG_RUNTIME_DIRECTORY_REQUIRED' }
}
function Assert-RagPricePolicy($Config,$Policy) {
    if($Policy.schemaVersion -cne 'rag-gemini-ledger-v1' -or $Policy.currency -cne 'USD' -or
        $Policy.campaignId -cne $Config.CampaignId -or -not $Policy.allBillableTokensCovered -or
        $Policy.pricingSource -cne 'https://ai.google.dev/gemini-api/docs/pricing' -or
        $Policy.limitUsd -cnotmatch '^\d{1,7}(\.\d{1,12})?$' -or [decimal]$Policy.limitUsd -le 0 -or [decimal]$Policy.limitUsd -gt 5 -or
        $Policy.maxEmbeddingInputs -lt 1 -or $Policy.maxEmbeddingInputs -gt 200 -or $Policy.maxGenerationCalls -lt 1 -or $Policy.maxGenerationCalls -gt 120) { throw 'RAG_PRICE_POLICY_INCOMPLETE' }
    $captured=[DateTimeOffset]::Parse($Policy.capturedAtUtc);$expires=[DateTimeOffset]::Parse($Policy.expiresAtUtc)
    if($captured -gt [DateTimeOffset]::UtcNow -or $expires -le [DateTimeOffset]::UtcNow -or $expires -gt $captured.AddHours(24)) { throw 'RAG_PRICE_SNAPSHOT_STALE' }
    foreach($kind in @('embedding','generation')) {
        $price=$Policy[$kind];$expected=if($kind -ceq 'embedding'){$Config.EmbeddingModel}else{$Config.ChatModel}
        if($price.model -cne $expected -or -not $price.boundsAndWorstCaseRatesApproved -or
            ($price.inputTokenBound -isnot [int] -and $price.inputTokenBound -isnot [long]) -or
            $price.inputTokenBound -lt 1 -or $price.inputTokenBound -gt 10000000 -or
            $price.inputUsdPerMillion -cnotmatch '^\d{1,7}(\.\d{1,12})?$' -or [decimal]$price.inputUsdPerMillion -le 0 -or
            $price.outputUsdPerMillion -cnotmatch '^\d{1,7}(\.\d{1,12})?$' -or [decimal]$price.outputUsdPerMillion -lt 0) { throw 'RAG_MODEL_PRICE_OR_BOUND_MISSING' }
        if($kind -ceq 'generation' -and ($price.outputTokenBound -ne 768 -or [decimal]$price.outputUsdPerMillion -le 0)) { throw 'RAG_GENERATION_BOUND_MISSING' }
    }
    $snapshot=Join-Path (Split-Path -Parent $Config.PolicyFile) 'pricing-snapshot.html'
    Assert-RagPrivate $snapshot
    if((Get-Item -LiteralPath $snapshot).Length -gt 8388608 -or (Get-RagHash $snapshot) -cne $Policy.pricingSnapshotSha256) { throw 'RAG_PRICE_SNAPSHOT_HASH_MISMATCH' }
}
function Assert-RagConfig([System.Collections.IDictionary]$Config, [switch]$StructuralOnly) {
    if ($Config.SchemaVersion -cne 't12-rag-gemini-sandbox-v1' -or $Config.RunId -cnotmatch '^[a-f0-9]{32}$' -or
        $Config.CampaignId -cnotmatch '^[a-f0-9]{32}$' -or $Config.RagDatabase -cne ('t12_pgvector_gemini_'+$Config.RunId) -or
        $Config.RagRole -cne ('rag_gemini_'+$Config.RunId)) { throw 'RAG_TARGET_IDENTITY_INVALID' }
    foreach ($port in @($Config.RagPort,$Config.BackendPort,$Config.RedisPort)) {
        if ($port -isnot [long] -and $port -isnot [int]) { throw 'RAG_PORT_INVALID' }
        if ($port -lt 1024 -or $port -gt 65535) { throw 'RAG_PORT_INVALID' }
    }
    if ($Config.RagPort -eq $Config.BackendPort -or $Config.RagPort -in @(15432,18080,13000,18081,$Config.RedisPort) -or
        $Config.BackendPort -in @(15432,18080,13000,18081,$Config.RedisPort)) { throw 'RAG_PORT_COLLISION' }
    if ($Config.Image -cnotmatch '^pgvector/pgvector@sha256:[a-f0-9]{64}$' -or $Config.PrimaryContainerId -cnotmatch '^[a-f0-9]{64}$' -or
        $Config.DockerContext -cnotin @('desktop-linux','default') -or
        $Config.PrimaryReaderRole -cnotmatch '^m1_gemini_reader_[a-f0-9]{10}$' -or
        $Config.KeycloakUrl -cne 'http://127.0.0.1:18081' -or $Config.KeycloakRealm -cne 'hotel-acceptance' -or
        $Config.KeycloakClientId -cne 'hotel-backend') { throw 'RAG_APPROVED_SANDBOX_REQUIRED' }
    foreach ($model in @($Config.ChatModel,$Config.EmbeddingModel)) {
        if ($model -cnotmatch '^[a-zA-Z0-9][a-zA-Z0-9._-]{0,127}$') { throw 'RAG_MODEL_REQUIRED' }
    }
    if (($Config.EmbeddingDimension -isnot [int] -and $Config.EmbeddingDimension -isnot [long]) -or
        $Config.EmbeddingDimension -lt 1 -or $Config.EmbeddingDimension -gt 3072 -or
        [double]$Config.RetrievalThreshold -notin @(0.30,0.40,0.50,0.60)) { throw 'RAG_DIMENSION_OR_THRESHOLD_INVALID' }
    if (@($Config.HotelIds).Count -ne 2 -or @($Config.HotelIds | Sort-Object -Unique).Count -ne 2 -or
        @($Config.HotelIds | Where-Object { $_ -isnot [int] -and $_ -isnot [long] -or $_ -lt 1 -or $_ -gt 32767 }).Count) { throw 'RAG_HOTEL_MAPPING_INVALID' }
    foreach ($name in @('RuntimeDirectory','SecretFile','JavaExecutable','BackendJar','Manifest','PolicyFile')) {
        if (-not [IO.Path]::IsPathFullyQualified([string]$Config[$name]) -or ([string]$Config[$name]).StartsWith('\\')) { throw 'RAG_LOCAL_ABSOLUTE_PATH_REQUIRED' }
    }
    foreach ($name in @('BackendJarSha256','ManifestSha256','PolicySha256','DdlSha256','ProfileSha256','ProvisionTemplateSha256')) {
        if ($Config[$name] -cnotmatch '^[a-f0-9]{64}$') { throw 'RAG_HASH_LOCK_REQUIRED' }
    }
    $script:RagDockerContext=$Config.DockerContext
    if ($StructuralOnly) { return }
    Assert-RagPrivate $Config.RuntimeDirectory -Directory
    Assert-RagPrivate $Config.SecretFile
    Assert-RagPrivate $Config.PolicyFile
    if ([IO.Path]::GetFullPath((Split-Path -Parent $Config.PolicyFile)) -cne [IO.Path]::GetFullPath($Config.RuntimeDirectory)) { throw 'RAG_POLICY_DIRECTORY_MISMATCH' }
    foreach ($entry in @(@($script:RagDdl,$Config.DdlSha256),@($script:RagProfile,$Config.ProfileSha256),
            @($Config.BackendJar,$Config.BackendJarSha256),@($Config.Manifest,$Config.ManifestSha256),@($Config.PolicyFile,$Config.PolicySha256))) {
        if ((Get-RagHash $entry[0]) -cne $entry[1]) { throw 'RAG_ARTIFACT_HASH_MISMATCH' }
    }
    if ((Get-RagTextHash (Get-RagProvisionSql $Config '__PRIVATE_RAG_PASSWORD__')) -cne $Config.ProvisionTemplateSha256) { throw 'RAG_PROVISION_TEMPLATE_CHANGED' }
    $policy = Read-RagJson $Config.PolicyFile
    Assert-RagPricePolicy $Config $policy
    $zip = [IO.Compression.ZipFile]::OpenRead($Config.BackendJar)
    try {
        foreach ($required in @('BOOT-INF/classes/application-rag-gemini-local.yaml',
            'BOOT-INF/classes/vn/edu/utc/hotel_booking/modules/ai/rag/service/RagOutboundLedger.class')) {
            if (-not $zip.GetEntry($required)) { throw 'RAG_JAR_STALE' }
        }
        $reader = [IO.StreamReader]::new($zip.GetEntry('BOOT-INF/classes/application-rag-gemini-local.yaml').Open())
        try { $packaged = $reader.ReadToEnd() } finally { $reader.Dispose() }
        if ((Get-RagTextHash $packaged) -cne $Config.ProfileSha256) { throw 'RAG_PACKAGED_PROFILE_MISMATCH' }
    } finally { $zip.Dispose() }
}
function Assert-RagApproval([string]$File, [string]$ConfigHash, [string[]]$Gates) {
    $approval = Read-RagJson $File
    if ($approval.approvedConfigSha256 -cne $ConfigHash) { throw 'RAG_APPROVAL_CONFIG_MISMATCH' }
    foreach ($gate in $Gates) {
        $record = $approval.gates[$gate]
        if (-not $record -or $record.status -cne 'APPROVED' -or [string]::IsNullOrWhiteSpace($record.reviewer) -or
            [string]::IsNullOrWhiteSpace($record.reference) -or ($gate -eq 'G04' -and $record.reviewerRole -cne 'Member 2')) { throw "RAG_APPROVAL_REQUIRED_$gate" }
    }
}
function Invoke-RagNative([string]$Executable, [string[]]$Arguments, [string]$InputText,
    [hashtable]$Environment=@{}, [string]$OutputFile, [string]$InputFile) {
    $info = [Diagnostics.ProcessStartInfo]::new($Executable)
    $info.UseShellExecute=$false; $info.CreateNoWindow=$true
    $info.RedirectStandardInput=$true; $info.RedirectStandardOutput=$true; $info.RedirectStandardError=$true
    $file=$null
    $info.Environment.Clear()
    foreach($name in @('SystemRoot','ComSpec','PATH','JAVA_HOME','USERPROFILE','TEMP','TMP','APPDATA','LOCALAPPDATA')) {
        $value=[Environment]::GetEnvironmentVariable($name,'Process');if($null -ne $value){$info.Environment[$name]=$value}
    }
    if($Executable -ceq 'docker') {
        if($script:RagDockerContext -cnotin @('desktop-linux','default')){throw 'RAG_LOCAL_DOCKER_CONTEXT_REQUIRED'}
        $info.ArgumentList.Add('--context');$info.ArgumentList.Add($script:RagDockerContext)
    }
    foreach ($argument in $Arguments) { $info.ArgumentList.Add($argument) }
    foreach ($name in $Environment.Keys) { $info.Environment[$name]=[string]$Environment[$name] }
    $process = [Diagnostics.Process]::new(); $process.StartInfo=$info
    try {
        if (-not $process.Start()) { throw 'failed' }
        $errors = $process.StandardError.ReadToEndAsync()
        if ($OutputFile) {
            $file = [IO.File]::Open($OutputFile,[IO.FileMode]::CreateNew,[IO.FileAccess]::Write,[IO.FileShare]::None)
            $copy = $process.StandardOutput.BaseStream.CopyToAsync($file)
        } else { $output = $process.StandardOutput.ReadToEndAsync() }
        if ($InputFile) {
            $source = [IO.File]::OpenRead($InputFile)
            try { $source.CopyTo($process.StandardInput.BaseStream) } finally { $source.Dispose() }
        } elseif ($InputText) { $process.StandardInput.Write($InputText) }
        $process.StandardInput.Close()
        if (-not $process.WaitForExit(45000)) { $process.Kill(); throw 'timeout' }
        if ($OutputFile) { $copy.GetAwaiter().GetResult(); $file.Dispose() }
        if ($process.ExitCode -ne 0) { throw 'failed' }
        if (-not $OutputFile) { return $output.GetAwaiter().GetResult().Trim() }
    } catch { throw 'RAG_NATIVE_OPERATION_FAILED: output suppressed for privacy.' }
    finally { if ($file) { $file.Dispose() }; $process.Dispose() }
}
function Assert-RagLocalDocker($Config) {
    $contexts=@((Invoke-RagNative 'docker' @('context','inspect',$Config.DockerContext)) | ConvertFrom-Json -AsHashtable)
    if($contexts.Count -ne 1 -or $contexts[0].Endpoints.docker.Host -cnotmatch '^npipe:////\./pipe/[A-Za-z0-9_-]+$') { throw 'RAG_REMOTE_DOCKER_DAEMON_FORBIDDEN' }
}
function Get-RagStatePath($Config) { return Join-Path $Config.RuntimeDirectory 'state.json' }
function Read-RagState($Config) {
    $state = Read-RagJson (Get-RagStatePath $Config)
    if ($state.runId -cne $Config.RunId -or $state.database -cne $Config.RagDatabase -or $state.configSha256 -cne (Get-RagHash $ConfigurationFile)) { throw 'RAG_STATE_OWNERSHIP_MISMATCH' }
    return $state
}
function Save-RagState($Config,$State) {
    $path = Get-RagStatePath $Config; $temp = $path+'.tmp-'+[Guid]::NewGuid().ToString('N')
    [IO.File]::WriteAllText($temp,($State | ConvertTo-Json -Depth 12),[Text.UTF8Encoding]::new($false))
    [IO.File]::Move($temp,$path,$true)
}
function Assert-RagContainer($Config,$State) {
    if ($State.containerId -cnotmatch '^[a-f0-9]{64}$') { throw 'RAG_CONTAINER_ID_INVALID' }
    $json = Invoke-RagNative 'docker' @('inspect',$State.containerId)
    $item = @($json | ConvertFrom-Json -AsHashtable)[0]
    $binding = @($item.HostConfig.PortBindings['5432/tcp'])
    if ($item.Config.Labels['hotel.m1.rag-gemini'] -cne $Config.RunId -or
        $item.Config.Image -cne $Config.Image -or $binding.Count -ne 1 -or
        $binding[0].HostIp -cne '127.0.0.1' -or $binding[0].HostPort -ne [string]$Config.RagPort -or
        @($item.Mounts | Where-Object { $_.Type -ceq 'volume' -and $_.Name -ceq ('m1-rag-gemini-'+$Config.RunId+'-data') -and $_.Destination -ceq '/var/lib/postgresql/data' }).Count -ne 1) { throw 'RAG_CONTAINER_OWNERSHIP_MISMATCH' }
    return $item
}
function Invoke-RagSql($Config,$State,[string]$Sql) {
    $null = Assert-RagContainer $Config $State
    return Invoke-RagNative 'docker' @('exec','-i',$State.containerId,'psql','-X','-v','ON_ERROR_STOP=1','-U','postgres','-d',$Config.RagDatabase,'-At') $Sql
}
function Get-RagProvisionSql($Config,[string]$Password) {
    $database=$Config.RagDatabase; $role=$Config.RagRole; $ids=($Config.HotelIds | ForEach-Object { "($_)" }) -join ','
    $ddl=[IO.File]::ReadAllText($script:RagDdl)
    $dollarQuote='$$'
    return @"
CREATE EXTENSION vector;
CREATE TABLE hotels(id SMALLINT PRIMARY KEY);
INSERT INTO hotels(id) VALUES $ids;
CREATE FUNCTION update_updated_at_column() RETURNS trigger LANGUAGE plpgsql AS $dollarQuote
BEGIN NEW.updated_at=now(); RETURN NEW; END $dollarQuote;
REVOKE ALL ON DATABASE "$database" FROM PUBLIC;
REVOKE ALL ON SCHEMA public FROM PUBLIC;
$ddl
CREATE ROLE "$role" LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS PASSWORD '$Password';
GRANT CONNECT ON DATABASE "$database" TO "$role";
GRANT USAGE ON SCHEMA public TO "$role";
GRANT SELECT,INSERT ON rag_store_config TO "$role";
GRANT SELECT,INSERT,UPDATE,DELETE ON rag_sources,rag_document_versions,rag_chunks TO "$role";
GRANT USAGE,SELECT ON SEQUENCE rag_sources_id_seq TO "$role";
INSERT INTO rag_store_config(id,embedding_model_id,embedding_dimension) VALUES(1,'$($Config.EmbeddingModel)',$($Config.EmbeddingDimension));
"@
}
function Wait-RagDatabase($Config,$State) {
    for ($attempt=0; $attempt -lt 45; $attempt++) {
        try { if ((Invoke-RagSql $Config $State 'SELECT current_database();') -ceq $Config.RagDatabase) { return } } catch { }
        Start-Sleep -Seconds 1
    }
    throw 'RAG_DATABASE_NOT_READY'
}
function Get-RagDataDigest($Config,$State) {
    $rows=Invoke-RagSql $Config $State @'
SELECT coalesce(jsonb_agg(to_jsonb(t) ORDER BY id)::text,'[]') FROM rag_store_config t;
SELECT coalesce(jsonb_agg(to_jsonb(t) ORDER BY id)::text,'[]') FROM rag_sources t;
SELECT coalesce(jsonb_agg(to_jsonb(t) ORDER BY id)::text,'[]') FROM rag_document_versions t;
SELECT coalesce(jsonb_agg(to_jsonb(t) ORDER BY id)::text,'[]') FROM rag_chunks t;
SELECT last_value,is_called FROM rag_sources_id_seq;
'@
    return Get-RagTextHash $rows
}
function Assert-RagModelPin($Config,$State) {
    $pin = Invoke-RagSql $Config $State "SELECT embedding_model_id || '|' || embedding_dimension FROM rag_store_config WHERE id=1;"
    if ($pin -cne ($Config.EmbeddingModel+'|'+$Config.EmbeddingDimension)) { throw 'RAG_PERSISTED_MODEL_DIMENSION_MISMATCH' }
}
function Assert-RagPrimaryReader($Config) {
    $primary=@((Invoke-RagNative 'docker' @('inspect',$Config.PrimaryContainerId)) | ConvertFrom-Json -AsHashtable)[0]
    $bindings=@($primary.HostConfig.PortBindings['5432/tcp'])
    if ($primary.Config.Labels['com.docker.compose.project'] -cne 'hotel-m1-acceptance' -or
        $primary.Config.Labels['com.docker.compose.service'] -cne 'acceptance_postgres' -or $bindings.Count -ne 1 -or
        $bindings[0].HostIp -cne '127.0.0.1' -or $bindings[0].HostPort -cne '15432') { throw 'RAG_PRIMARY_OWNERSHIP_MISMATCH' }
    $secret=Read-RagJson $Config.SecretFile
    $ids=$Config.HotelIds -join ','
    $sql=@"
SELECT (NOT rolsuper AND NOT rolcreatedb AND NOT rolcreaterole AND NOT rolreplication AND NOT rolbypassrls)::text FROM pg_roles WHERE rolname=current_user;
SELECT count(*) FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname='public' AND c.relkind IN ('r','p') AND has_table_privilege(current_user,c.oid,'INSERT,UPDATE,DELETE,TRUNCATE');
SELECT (NOT has_schema_privilege(current_user,'public','CREATE'))::text;
SELECT count(*) FROM hotels WHERE id IN ($ids) AND status='ACTIVE' AND is_deleted=false;
SELECT count(*) FROM pg_proc p JOIN pg_namespace n ON n.oid=p.pronamespace WHERE n.nspname='public' AND p.prosecdef AND has_function_privilege(current_user,p.oid,'EXECUTE');
"@
    $check=Invoke-RagNative 'docker' @('exec','-i','-e','PGPASSWORD',$Config.PrimaryContainerId,'psql','-X','-v','ON_ERROR_STOP=1','-h','127.0.0.1','-U',$Config.PrimaryReaderRole,'-d','hotel_acceptance_db','-At') $sql @{PGPASSWORD=$secret.primaryPassword}
    if (($check -replace "`r",'') -cne "true`n0`ntrue`n2`n0") { throw 'RAG_PRIMARY_ROLE_OR_HOTEL_FIXTURES_INVALID' }
}
function Assert-RagNoProcess($Config,$State) {
    if ($State.backendPid -and (Get-Process -Id $State.backendPid -ErrorAction SilentlyContinue)) { throw 'RAG_BACKEND_MUST_BE_STOPPED' }
    if (Get-NetTCPConnection -State Listen -LocalPort $Config.BackendPort -ErrorAction SilentlyContinue) { throw 'RAG_BACKEND_PORT_OCCUPIED' }
}
function Stop-RagBackend($Config,$State) {
    if ($State.backendPid) {
        $process=Get-Process -Id $State.backendPid -ErrorAction SilentlyContinue
        if ($process) {
            $cim=Get-CimInstance Win32_Process -Filter "ProcessId=$($State.backendPid)"
            if ($process.StartTime.ToUniversalTime().Ticks -ne [long]$State.backendStartTicks -or
                $process.Path -ine $State.javaExecutable -or $cim.CommandLine -notlike ('*'+$Config.BackendJar+'*') -or
                $cim.CommandLine -notlike '*rag-gemini-local*') { throw 'RAG_PROCESS_OWNERSHIP_MISMATCH' }
            $process.Kill(); if (-not $process.WaitForExit(5000)) { throw 'RAG_PROCESS_STOP_FAILED' }
        }
    }
    Assert-RagNoProcess $Config $State
}
function Get-RagChildEnvironment($Config,$Secrets) {
    $environment=@{}
    foreach ($name in @('SystemRoot','ComSpec','PATH','JAVA_HOME','USERPROFILE','TEMP','TMP','APPDATA','LOCALAPPDATA')) {
        $value=[Environment]::GetEnvironmentVariable($name,'Process'); if ($null -ne $value) { $environment[$name]=$value }
    }
    $values=@{
        RAG_GEMINI_API_KEY=$Secrets.geminiKey; RAG_GEMINI_LEDGER_REQUIRED='true'; RAG_GEMINI_LEDGER_POLICY=$Config.PolicyFile; RAG_GEMINI_LEDGER_POLICY_SHA256=$Config.PolicySha256
        RAG_GEMINI_PRIMARY_JDBC_URL='jdbc:postgresql://127.0.0.1:15432/hotel_acceptance_db'; RAG_GEMINI_PRIMARY_USERNAME=$Config.PrimaryReaderRole; RAG_GEMINI_PRIMARY_PASSWORD=$Secrets.primaryPassword
        RAG_PGVECTOR_JDBC_URL="jdbc:postgresql://127.0.0.1:$($Config.RagPort)/$($Config.RagDatabase)"; RAG_PGVECTOR_USERNAME=$Config.RagRole; RAG_PGVECTOR_PASSWORD=$Secrets.ragPassword
        RAG_GEMINI_BACKEND_PORT=[string]$Config.BackendPort; RAG_GEMINI_STORAGE_DIRECTORY=(Join-Path $Config.RuntimeDirectory 'storage')
        RAG_GEMINI_CHAT_MODEL=$Config.ChatModel; RAG_GEMINI_EMBEDDING_MODEL=$Config.EmbeddingModel; RAG_GEMINI_EMBEDDING_DIMENSION=[string]$Config.EmbeddingDimension
        RAG_GEMINI_RETRIEVAL_THRESHOLD=([double]$Config.RetrievalThreshold).ToString([Globalization.CultureInfo]::InvariantCulture)
        HOTEL_AI_ADVISOR_PROVIDER_GEMINIENABLED='false'; HOTEL_AI_ADVISOR_PROVIDER_GEMINI_ENABLED='false'; HOTEL_SCHEDULING_ENABLED='false'; SPRING_LIQUIBASE_ENABLED='false'
        KEYCLOAK_SERVER_URL=$Config.KeycloakUrl; KEYCLOAK_REALM=$Config.KeycloakRealm; KEYCLOAK_CLIENT_ID=$Config.KeycloakClientId; KEYCLOAK_CLIENT_SECRET=$Secrets.keycloakClientSecret
        KEYCLOAK_ISSUER_URI="$($Config.KeycloakUrl)/realms/$($Config.KeycloakRealm)"; KEYCLOAK_JWK_SET_URI="$($Config.KeycloakUrl)/realms/$($Config.KeycloakRealm)/protocol/openid-connect/certs"
        HOTEL_AI_ADVISOR_SECURITY_ANON_TOKEN_SECRET=$Secrets.anonTokenSecret; REDIS_HOST='127.0.0.1'; REDIS_PORT=[string]$Config.RedisPort
    }
    foreach ($key in $values.Keys) { $environment[$key]=[string]$values[$key] }
    return $environment
}
function Get-RagBackendArguments($Config) {
    return @('-Duser.timezone=Asia/Ho_Chi_Minh','-jar',$Config.BackendJar,'--spring.profiles.active=rag-gemini-local',
        '--spring.config.location=classpath:/application.yaml,classpath:/application-rag-gemini-local.yaml')
}
function Start-RagSupervisor($Config,$State) {
    Assert-RagNoProcess $Config $State; $null=Assert-RagContainer $Config $State; Assert-RagModelPin $Config $State
    Assert-RagPrimaryReader $Config
    for ($attempt=0;$attempt -lt 30;$attempt++) {
        try { $lease=[IO.File]::Open((Join-Path $Config.RuntimeDirectory 'backend-lease.lock'),[IO.FileMode]::OpenOrCreate,[IO.FileAccess]::ReadWrite,[IO.FileShare]::None);$lease.Dispose();break }
        catch { if($attempt -eq 29){throw 'RAG_PREVIOUS_SUPERVISOR_STILL_ACTIVE'};Start-Sleep -Milliseconds 100 }
    }
    $info=[Diagnostics.ProcessStartInfo]::new((Get-Command pwsh).Source); $info.UseShellExecute=$false; $info.CreateNoWindow=$true
    foreach ($arg in @('-NoProfile','-File',(Join-Path $PSScriptRoot 'Start-RagGeminiDemo.ps1'),'-Action','Supervisor','-ConfigurationFile',$ConfigurationFile,'-ApprovalFile',$ApprovalFile,'-AllowLive')) { $info.ArgumentList.Add($arg) }
    $info.Environment.Clear()
    foreach ($name in @('SystemRoot','ComSpec','PATH','USERPROFILE','TEMP','TMP','APPDATA','LOCALAPPDATA')) { $info.Environment[$name]=[Environment]::GetEnvironmentVariable($name,'Process') }
    $parent=[Diagnostics.Process]::Start($info)
    for ($attempt=0; $attempt -lt 50; $attempt++) {
        Start-Sleep -Milliseconds 200
        $current=Read-RagState $Config
        if ($current.backendPid -and (Get-Process -Id $current.backendPid -ErrorAction SilentlyContinue)) { return }
        if ($parent.HasExited) { throw 'RAG_SUPERVISOR_START_FAILED' }
    }
    throw 'RAG_SUPERVISOR_START_TIMEOUT'
}
function Invoke-RagSupervisor($Config,$State) {
    $secret=Read-RagJson $Config.SecretFile
    foreach ($name in @('ragPassword','adminPassword')) { if ($secret[$name] -cnotmatch '^[a-fA-F0-9]{48,128}$') { throw 'RAG_PRIVATE_DB_CREDENTIAL_INVALID' } }
    if ([string]::IsNullOrWhiteSpace($secret.geminiKey) -or $secret.geminiKey -match '\s' -or $secret.anonTokenSecret.Length -lt 64) { throw 'RAG_PRIVATE_CREDENTIALS_MISSING' }
    $info=[Diagnostics.ProcessStartInfo]::new($Config.JavaExecutable); $info.UseShellExecute=$false; $info.CreateNoWindow=$true
    $info.RedirectStandardOutput=$true; $info.RedirectStandardError=$true; $info.WorkingDirectory=$Config.RuntimeDirectory
    foreach ($arg in (Get-RagBackendArguments $Config)) { $info.ArgumentList.Add($arg) }
    $info.Environment.Clear(); $envValues=Get-RagChildEnvironment $Config $secret
    foreach ($name in $envValues.Keys) { $info.Environment[$name]=$envValues[$name] }
    # A dedicated C# reader persists only the fixed audit record, never arbitrary stdout/stderr.
    if (-not ('HotelRagAuditPump' -as [type])) {
        Add-Type -TypeDefinition @'
using System; using System.IO; using System.Text.RegularExpressions; using System.Threading.Tasks;
public static class HotelRagAuditPump {
 public static Task Drain(StreamReader input, string path) { return Task.Run(async () => {
   using(var writer=new StreamWriter(new FileStream(path,FileMode.Append,FileAccess.Write,FileShare.Read))) {
    string line; while((line=await input.ReadLineAsync())!=null) {
     var match=Regex.Match(line,@"RAG_(?:REQUEST|PROVIDER) requestId=[a-f0-9-]{36} model=[A-Za-z0-9._-]{1,128} durationMs=\d+ inputTokens=-?\d+ outputTokens=-?\d+ totalTokens=-?\d+ outcome=(?:SUCCESS|UNAVAILABLE|DEADLINE|RATE_LIMITED|PROVIDER_ERROR)$");
     if(match.Success) { await writer.WriteLineAsync(match.Value); await writer.FlushAsync(); }
    }
   }
 }); }
}
'@
    }
    $process=[Diagnostics.Process]::Start($info)
    $State.backendPid=$process.Id; $State.backendStartTicks=$process.StartTime.ToUniversalTime().Ticks; $State.javaExecutable=$process.Path
    Save-RagState $Config $State
    $stdout=[HotelRagAuditPump]::Drain($process.StandardOutput,(Join-Path $Config.RuntimeDirectory 'backend-audit.log'))
    $stderr=[HotelRagAuditPump]::Drain($process.StandardError,(Join-Path $Config.RuntimeDirectory 'backend-error-audit.log'))
    $secret=$null; $envValues=$null; $info.Environment.Clear()
    $process.WaitForExit(); $stdout.GetAwaiter().GetResult(); $stderr.GetAwaiter().GetResult()
}
function Invoke-RagDemo {
    if ($Action -eq 'Template') { Get-RagTemplate | ConvertTo-Json -Depth 12; return }
    if ($Action -eq 'PolicyTemplate') { Get-RagPolicyTemplate | ConvertTo-Json -Depth 12; return }
    if ($Action -eq 'ApprovalTemplate') { Get-RagApprovalTemplate | ConvertTo-Json -Depth 12; return }
    $config=Read-RagJson $ConfigurationFile; Assert-RagConfig $config -StructuralOnly
    if ($Action -eq 'Plan') {
        [ordered]@{status='PROPOSED / NOT APPROVED';configSha256=(Get-RagHash $ConfigurationFile);ddlSha256=(Get-RagHash $script:RagDdl)
            provisionTemplateSha256=(Get-RagTextHash (Get-RagProvisionSql $config '__PRIVATE_RAG_PASSWORD__'))
            database=$config.RagDatabase;ragPort=$config.RagPort;backendPort=$config.BackendPort;paidCalls=0;databaseOperations=0} | ConvertTo-Json; return
    }
    if($Action -in @('Provision','Start','Restart','Supervisor')) { Assert-RagConfig $config }
    else { Assert-RagPrivate $config.RuntimeDirectory -Directory }
    $gates=@('G04'); if ($Action -in @('Start','Restart','Supervisor')) {
        if (-not $AllowLive) { throw 'RAG_EXPLICIT_LIVE_OPT_IN_REQUIRED' }; $gates+=@('G05','G09')
    }
    Assert-RagApproval $ApprovalFile (Get-RagHash $ConfigurationFile) $gates
    Assert-RagLocalDocker $config
    if ($Action -eq 'Provision') {
        if (-not $ApproveProvision) { throw 'RAG_EXPLICIT_PROVISION_OPT_IN_REQUIRED' }
        if (Test-Path -LiteralPath (Get-RagStatePath $config)) { throw 'RAG_STATE_ALREADY_EXISTS' }
        if (Get-NetTCPConnection -State Listen -LocalPort $config.RagPort,$config.BackendPort -ErrorAction SilentlyContinue) { throw 'RAG_TARGET_PORT_OCCUPIED' }
        Assert-RagPrimaryReader $config
        $secrets=Read-RagJson $config.SecretFile
        foreach ($name in @('ragPassword','adminPassword')) { if ($secrets[$name] -cnotmatch '^[a-fA-F0-9]{48,128}$') { throw 'RAG_PRIVATE_DB_CREDENTIAL_INVALID' } }
        # Snapshot original reviewed config BEFORE provisioning; never modify shared runtime/config files.
        $backup=Join-Path $config.RuntimeDirectory 'configuration-before.json'
        [IO.File]::Copy($ConfigurationFile,$backup,$false)
        $null=Invoke-RagNative 'docker' @('exec',$config.PrimaryContainerId,'pg_dump','-Fc','--schema-only','--no-owner','-U','postgres','-d','hotel_acceptance_db') '' @{} (Join-Path $config.RuntimeDirectory 'primary-schema-before.dump')
        $name='m1-rag-gemini-'+$config.RunId
        $id=Invoke-RagNative 'docker' @('run','-d','--name',$name,'--label',"hotel.m1.rag-gemini=$($config.RunId)",'--publish',"127.0.0.1:$($config.RagPort):5432",
            '--mount',"type=volume,source=$name-data,target=/var/lib/postgresql/data",'-e','POSTGRES_PASSWORD','-e',"POSTGRES_DB=$($config.RagDatabase)",$config.Image) '' @{POSTGRES_PASSWORD=$secrets.adminPassword}
        if ($id -cnotmatch '^[a-f0-9]{64}$') { throw 'RAG_CONTAINER_CREATE_FAILED' }
        $state=@{runId=$config.RunId;configSha256=(Get-RagHash $ConfigurationFile);database=$config.RagDatabase;containerId=$id;backendPid=$null;phase='CREATED'}
        Save-RagState $config $state; Wait-RagDatabase $config $state
        if ((Invoke-RagSql $config $state "SELECT count(*) FROM information_schema.tables WHERE table_schema='public';") -cne '0') { throw 'RAG_NEW_DATABASE_NOT_EMPTY' }
        $emptySnapshot=Join-Path $config.RuntimeDirectory 'rag-empty-before-ddl.dump'
        $null=Invoke-RagNative 'docker' @('exec',$id,'pg_dump','-Fc','--no-owner','-U','postgres','-d',$config.RagDatabase) '' @{} $emptySnapshot
        $state.emptySnapshotSha256=Get-RagHash $emptySnapshot;$state.phase='EMPTY_SNAPSHOT_READY';Save-RagState $config $state
        $null=Invoke-RagSql $config $state (Get-RagProvisionSql $config $secrets.ragPassword)
        $state.phase='PROVISIONED'; Save-RagState $config $state
        $snapshot=Join-Path $config.RuntimeDirectory 'rag-before.dump'
        $null=Invoke-RagNative 'docker' @('exec',$id,'pg_dump','-Fc','--no-owner','-U','postgres','-d',$config.RagDatabase) '' @{} $snapshot
        $state.snapshotSha256=Get-RagHash $snapshot; $state.beforeDigest=Get-RagDataDigest $config $state; Save-RagState $config $state
        Write-Output 'CONFIRMED: owned sandbox provisioned and pre-ingestion snapshot captured.'; return
    }
    $state=Read-RagState $config
    switch ($Action) {
        'Start' { Start-RagSupervisor $config $state }
        'Restart' { Stop-RagBackend $config $state; Start-RagSupervisor $config (Read-RagState $config) }
        'Supervisor' {
            $lease=[IO.File]::Open((Join-Path $config.RuntimeDirectory 'backend-lease.lock'),[IO.FileMode]::OpenOrCreate,[IO.FileAccess]::ReadWrite,[IO.FileShare]::None)
            try { Assert-RagNoProcess $config (Read-RagState $config); Invoke-RagSupervisor $config (Read-RagState $config) }
            finally { $lease.Dispose() }
        }
        'Stop' { Stop-RagBackend $config $state }
        'Inspect' { $null=Assert-RagContainer $config $state; Assert-RagModelPin $config $state; Write-Output 'CONFIRMED: owned target and persisted model/dimension match.' }
        'Snapshot' { throw 'RAG_BASELINE_SNAPSHOT_IS_IMMUTABLE: a new run requires a separately reviewed runtime directory.' }
        'DbOutage' { $null=Assert-RagContainer $config $state; $null=Invoke-RagNative 'docker' @('stop','--time','10',$state.containerId) }
        'DbRecovery' { $null=Assert-RagContainer $config $state; $null=Invoke-RagNative 'docker' @('start',$state.containerId); Wait-RagDatabase $config $state }
        'Restore' {
            Stop-RagBackend $config $state; $null=Assert-RagContainer $config $state
            $emptyBaseline=-not $state.snapshotSha256
            $snapshot=Join-Path $config.RuntimeDirectory $(if($emptyBaseline){'rag-empty-before-ddl.dump'}else{'rag-before.dump'})
            $expected=if($emptyBaseline){$state.emptySnapshotSha256}else{$state.snapshotSha256}
            if (-not $expected -or (Get-RagHash $snapshot) -cne $expected -or (Get-RagHash (Join-Path $config.RuntimeDirectory 'configuration-before.json')) -cne $state.configSha256) { throw 'RAG_SNAPSHOT_CHANGED' }
            if ((Invoke-RagSql $config $state 'SELECT count(*) FROM pg_stat_activity WHERE datname=current_database() AND pid<>pg_backend_pid();') -cne '0') { throw 'RAG_FOREIGN_DATABASE_SESSIONS' }
            if($emptyBaseline) {
                # An empty archive cannot drop objects created by a failed bootstrap. Only this verified,
                # newly owned RAG database is reset; never execute this against the primary datasource.
                $null=Invoke-RagSql $config $state 'DROP EXTENSION IF EXISTS vector CASCADE; DROP SCHEMA public CASCADE; CREATE SCHEMA public;'
            }
            $null=Invoke-RagNative 'docker' @('exec','-i',$state.containerId,'pg_restore','--clean','--if-exists','--no-owner','-U','postgres','-d',$config.RagDatabase) '' @{} '' $snapshot
            if($emptyBaseline) {
                if((Invoke-RagSql $config $state "SELECT count(*) FROM information_schema.tables WHERE table_schema='public';") -cne '0'){throw 'RAG_EMPTY_RESTORE_MISMATCH'}
            } elseif ((Get-RagDataDigest $config $state) -cne $state.beforeDigest) { throw 'RAG_RESTORE_DIGEST_MISMATCH' }
            if ((Get-RagHash $ConfigurationFile) -cne $state.configSha256) { throw 'RAG_CONFIGURATION_CHANGED' }
            $state.phase='RESTORED'; Save-RagState $config $state
            Write-Output 'CONFIRMED: RAG snapshot restored; config unchanged; spending ledger retained.'
        }
    }
}
if ($MyInvocation.InvocationName -ne '.') {
    try { Invoke-RagDemo } catch { Write-Error ('RAG_GEMINI_ACTION_FAILED: '+$Action+'. Inspect reviewed inputs and ownership; secret/provider details suppressed.'); exit 1 }
}
