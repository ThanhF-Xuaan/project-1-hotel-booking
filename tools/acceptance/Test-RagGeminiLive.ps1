<#
.SYNOPSIS
Explicit, paid opt-in technical persistence check on the reviewed Gemini sandbox.
.DESCRIPTION
Does not provision a database or obtain admin credentials. Requires the existing owned sandbox,
G04/G05/G06/G09 approvals and a CHAIN_ADMIN bearer SecureString supplied in the current shell.
Fetches the official pricing page before starting; its exact bytes must match the reviewed price
snapshot. A changed page, missing usage, unresolved reservation or unknown price stops the run.
No cloud alerts are treated as a cap: backend transport reserves durable cost/input/call limits.
The ledger is NEVER deleted/refunded by DB restore or process restart. Default cleanup stops only
owned processes and restores the pre-ingestion RAG snapshot. -KeepKnowledge preserves corpus,
but still stops the backend and dedicated database. This is not TASK-10 semantic acceptance.
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$ConfigurationFile,
    [Parameter(Mandatory)][string]$ApprovalFile,
    [switch]$AllowLive,
    [Security.SecureString]$AdminToken,
    [switch]$KeepKnowledge
)
$ErrorActionPreference='Stop'
$liveRequested=$AllowLive.IsPresent
. (Join-Path $PSScriptRoot 'Start-RagGeminiDemo.ps1') -ConfigurationFile $ConfigurationFile -ApprovalFile $ApprovalFile -AllowLive:$liveRequested
function Get-RagLedgerSummary($Config) {
    $journal=Join-Path $Config.RuntimeDirectory ($Config.CampaignId+'.ledger.jsonl')
    if (-not (Test-Path -LiteralPath $journal)) { return @{embeddingInputs=0;embeddingRequests=0;generationCalls=0;reservedOutboundAttempts=0;inputTokens=0;outputTokens=0;totalTokens=0;chargedOrReservedUsd='0';pending=0;halted=$false;sha256=$null} }
    $content=[IO.File]::ReadAllText($journal)
    if ($content -and -not $content.EndsWith("`n")) { throw 'RAG_LEDGER_PARTIAL_WRITE' }
    $previous='0'*64; $inputs=0; $calls=0; $embeddingRequests=0; $requests=0
    [long]$inputTokens=0;[long]$outputTokens=0;[long]$totalTokens=0
    [decimal]$cost=0; $pending=@{}; $halted=$false
    foreach($line in $content.Split("`n",[StringSplitOptions]::RemoveEmptyEntries)) {
        $envelope=$line | ConvertFrom-Json -AsHashtable
        if ($envelope.previous -cne $previous -or (Get-RagTextHash ($previous+"`n"+$envelope.event)) -cne $envelope.hash) { throw 'RAG_LEDGER_CHAIN_INVALID' }
        $event=$envelope.event | ConvertFrom-Json -AsHashtable
        if ($event.policySha256 -cne $Config.PolicySha256) { throw 'RAG_LEDGER_POLICY_CHANGED' }
        switch($event.type) {
            'RESERVE' { $inputs+=[int]$event.inputs; $calls+=[int]$event.generations; $requests++;if($event.inputs -gt 0){$embeddingRequests++};$cost+=[decimal]$event.maxCost; $pending[$event.id]=[decimal]$event.maxCost }
            'SETTLE' { if (-not $pending.ContainsKey($event.id)) { throw 'RAG_LEDGER_SETTLE_INVALID' }; $cost-=$pending[$event.id]; $cost+=[decimal]$event.actualCost; $pending.Remove($event.id)
                $inputTokens+=[long]$event.inputTokens;$outputTokens+=[long]$event.outputTokens;$totalTokens+=[long]$event.totalTokens }
            'HALT' { $halted=$true }
            default { throw 'RAG_LEDGER_EVENT_INVALID' }
        }
        $previous=$envelope.hash
    }
    if ($inputs -gt 200 -or $calls -gt 120 -or $cost -gt 5) { throw 'RAG_LEDGER_CAP_INVALID' }
    $known=($pending.Count -eq 0 -and -not $halted)
    return @{embeddingInputs=$inputs;embeddingRequests=$embeddingRequests;generationCalls=$calls;reservedOutboundAttempts=$requests
        inputTokens=$(if($known){$inputTokens}else{$null});outputTokens=$(if($known){$outputTokens}else{$null});totalTokens=$(if($known){$totalTokens}else{$null})
        chargedOrReservedUsd=$cost.ToString([Globalization.CultureInfo]::InvariantCulture);pending=$pending.Count;halted=$halted;sha256=(Get-RagHash $journal)}
}
function Assert-RagLedgerReady($Config) {
    $summary=Get-RagLedgerSummary $Config
    if ($summary.halted -or $summary.pending -ne 0) { throw 'RAG_LEDGER_HALTED_OR_UNRESOLVED' }
    return $summary
}
function Invoke-RagApi($Client,[string]$Path,$Body,[switch]$Admin,[int]$ExpectedStatus=200) {
    $request=[Net.Http.HttpRequestMessage]::new([Net.Http.HttpMethod]::Post,$script:RagApiBase+$Path)
    try {
        $request.Content=[Net.Http.StringContent]::new(($Body | ConvertTo-Json -Depth 15 -Compress),[Text.Encoding]::UTF8,'application/json')
        if ($Admin) { $request.Headers.Authorization=[Net.Http.Headers.AuthenticationHeaderValue]::new('Bearer',$script:RagAdminBearer) }
        $response=$Client.Send($request)
        try {
            if ([int]$response.StatusCode -ne $ExpectedStatus) { throw 'RAG_API_STATUS_FAILED' }
            $result=$response.Content.ReadAsStringAsync().GetAwaiter().GetResult() | ConvertFrom-Json -AsHashtable -Depth 30
            if ([int]$result.code -ne $ExpectedStatus) { throw 'RAG_API_ENVELOPE_FAILED' }
            return $result
        } finally { $response.Dispose() }
    } catch { throw 'RAG_API_CHECK_FAILED: response/provider body suppressed.' }
    finally { $request.Dispose() }
}
function Invoke-RagLauncher([string]$Phase,[switch]$Live) {
    $arguments=@{Action=$Phase;ConfigurationFile=$ConfigurationFile;ApprovalFile=$ApprovalFile}
    if ($Live) { $arguments.AllowLive=$true }
    $global:LASTEXITCODE=0
    & (Join-Path $PSScriptRoot 'Start-RagGeminiDemo.ps1') @arguments | Out-Null
    if ($LASTEXITCODE -and $LASTEXITCODE -ne 0) { throw 'RAG_LAUNCHER_PHASE_FAILED' }
}
if ($MyInvocation.InvocationName -eq '.') { return }
$report=@{task='TASK-08';status='NOT RUN';phases=@();paidCallsAuthorized=$false;semanticQuality='NOT RUN / TASK-10';provisioning='NOT PERFORMED BY LIVE RUNNER'}
$phase='PREFLIGHT'; $owned=$false; $client=$null; $lock=$null
try {
    if (-not $AllowLive -or $null -eq $AdminToken -or $AdminToken.Length -eq 0) { throw 'RAG_EXPLICIT_LIVE_AND_CHAIN_ADMIN_TOKEN_REQUIRED' }
    $config=Read-RagJson $ConfigurationFile; Assert-RagConfig $config
    Assert-RagApproval $ApprovalFile (Get-RagHash $ConfigurationFile) @('G04','G05','G06','G09')
    $state=Read-RagState $config; $null=Assert-RagContainer $config $state
    Assert-RagNoProcess $config $state; Assert-RagModelPin $config $state
    $lock=[IO.File]::Open((Join-Path $config.RuntimeDirectory 'live-runner.lock'),[IO.FileMode]::OpenOrCreate,[IO.FileAccess]::ReadWrite,[IO.FileShare]::None)
    $null=Assert-RagLedgerReady $config
    $policy=Read-RagJson $config.PolicyFile
    if ([DateTimeOffset]::Parse($policy.expiresAtUtc) -le [DateTimeOffset]::UtcNow -or
        [DateTimeOffset]::Parse($policy.capturedAtUtc) -gt [DateTimeOffset]::UtcNow) { throw 'RAG_PRICE_SNAPSHOT_STALE' }
    # Never guess prices from model names or scrape numbers into a policy automatically.
    $observed=Join-Path $config.RuntimeDirectory ('pricing-observed-'+[Guid]::NewGuid().ToString('N')+'.html')
    Invoke-WebRequest -Uri 'https://ai.google.dev/gemini-api/docs/pricing' -MaximumRedirection 0 -TimeoutSec 20 -OutFile $observed
    if ((Get-RagHash $observed) -cne $policy.pricingSnapshotSha256) { throw 'RAG_CURRENT_PRICING_CHANGED_REVIEW_REQUIRED' }
    $manifest=Read-RagJson $config.Manifest
    foreach($entry in $manifest.fileHashes.GetEnumerator()) {
        $path=Join-Path (Split-Path -Parent $config.Manifest) $entry.Key
        if ((Get-RagHash $path) -cne $entry.Value.sha256) { throw 'RAG_CORPUS_HASH_CHANGED' }
    }
    $corpus=Read-RagJson (Join-Path (Split-Path -Parent $config.Manifest) 'corpus.json')
    if (-not $corpus.synthetic -or @($corpus.documents).Count -ne 13) { throw 'RAG_SYNTHETIC_CORPUS_REQUIRED' }
    foreach($doc in $corpus.documents) {
        if (-not $doc.synthetic -or $doc.visibility -cne 'PUBLIC' -or (Get-RagTextHash $doc.content) -cne $doc.contentSha256) { throw 'RAG_DOCUMENT_NOT_VERIFIED' }
        $chunks=@([regex]::Split($doc.content.Trim(),'(?m)^\s*\n\s*') | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
        if ($chunks.Count -gt 64 -or @($chunks | Where-Object { $_.Trim().Length -gt 2000 }).Count) { throw 'RAG_DOCUMENT_INPUT_LIMIT' }
    }
    $pointer=[Runtime.InteropServices.Marshal]::SecureStringToBSTR($AdminToken)
    try { $script:RagAdminBearer=[Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer) } finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer) }
    $script:RagApiBase="http://127.0.0.1:$($config.BackendPort)"
    $handler=[Net.Http.HttpClientHandler]::new(); $handler.AllowAutoRedirect=$false; $handler.UseProxy=$false
    $client=[Net.Http.HttpClient]::new($handler); $client.Timeout=[TimeSpan]::FromSeconds(15)
    $owned=$true; $report.paidCallsAuthorized=$true
    $phase='START'; Invoke-RagLauncher 'Start' -Live
    for($attempt=0;$attempt -lt 90;$attempt++) {
        try { $ready=$client.GetAsync($script:RagApiBase+'/v3/api-docs').GetAwaiter().GetResult(); if($ready.IsSuccessStatusCode) { $ready.Dispose(); break };$ready.Dispose() } catch { }
        if($attempt -eq 89) { throw 'RAG_BACKEND_NOT_READY' }; Start-Sleep -Seconds 1
    }
    $phase='INGEST'
    foreach($doc in ($corpus.documents | Sort-Object version,sourceId)) {
        $hotel=if ($doc.scopeType -ceq 'HOTEL_SCOPED') { $config.HotelIds[[int]$doc.hotelId-1] } else { $null }
        $body=@{sourceId=$doc.sourceId;scopeType=$doc.scopeType;hotelId=$hotel;category=$doc.category;title=$doc.title;content=$doc.content;effectiveFrom=$doc.effectiveFrom;effectiveTo=$doc.effectiveTo}
        $result=Invoke-RagApi $client ("/api/v1/ai/admin/knowledge/stage?version="+$doc.version) $body -Admin
        if ($result.result.status -cne 'STAGED') { throw 'RAG_STAGE_NOT_ATOMIC' }
        $null=Assert-RagLedgerReady $config
        $null=Invoke-RagApi $client '/api/v1/ai/admin/knowledge/activate' @{sourceId=$doc.sourceId;scopeType=$doc.scopeType;hotelId=$hotel;version=$doc.version} -Admin
    }
    $report.phases+=@{phase='ingestion';status='PASS';versions=13;adminAuthority='CHAIN_ADMIN enforced by existing backend API'}
    $ask=@{hotelId=$config.HotelIds[0];question='Giờ nhận và trả phòng?';language='vi';asOfDate='2026-11-20';topK=5}
    $phase='PERSISTENCE'
    $before=Invoke-RagApi $client '/api/v1/public/ai/room-advisor/knowledge/ask' $ask
    if ($before.result.status -cne 'GROUNDED_ANSWER' -or $before.result.warnings -contains 'GENERATION_FALLBACK_EXTRACTIVE' -or
        @($before.result.citations | Where-Object { $_.sourceId -ceq 'SYNTHETIC-CHECKIN' -and $_.version -eq 2 }).Count -ne 1) { throw 'RAG_GEMINI_TECHNICAL_ANSWER_FAILED' }
    $null=Assert-RagLedgerReady $config; $digest=Get-RagDataDigest $config (Read-RagState $config)
    foreach($mutation in @(@('EmbeddingModel','wrong-synthetic-model'),@('EmbeddingDimension',($config.EmbeddingDimension+1)))) {
        $wrong=@{};foreach($key in $config.Keys) { $wrong[$key]=$config[$key] };$wrong[$mutation[0]]=$mutation[1]
        $blocked=$false;try { Assert-RagModelPin $wrong (Read-RagState $config) } catch { $blocked=$true };if(-not $blocked) { throw 'RAG_MODEL_MISMATCH_NOT_BLOCKED' }
    }
    Invoke-RagLauncher 'Restart' -Live
    Start-Sleep -Seconds 2
    # Readiness polling only; API ask is never retried automatically (each attempt is billable).
    for($attempt=0;$attempt -lt 90;$attempt++) {
        try { $ready=$client.GetAsync($script:RagApiBase+'/v3/api-docs').GetAwaiter().GetResult();if($ready.IsSuccessStatusCode){$ready.Dispose();break};$ready.Dispose() } catch { }
        if($attempt -eq 89){throw 'RAG_RESTART_NOT_READY'};Start-Sleep -Seconds 1
    }
    $after=Invoke-RagApi $client '/api/v1/public/ai/room-advisor/knowledge/ask' $ask
    if ($after.result.status -cne 'GROUNDED_ANSWER' -or (Get-RagDataDigest $config (Read-RagState $config)) -cne $digest -or
        (@($before.result.citations.chunkId | Sort-Object) -join ',') -cne (@($after.result.citations.chunkId | Sort-Object) -join ',')) { throw 'RAG_RESTART_LOST_KNOWLEDGE' }
    $null=Assert-RagLedgerReady $config; $report.phases+=@{phase='restart-and-mismatch';status='PASS'}
    $phase='OUTAGE'; Invoke-RagLauncher 'DbOutage'
    $null=Invoke-RagApi $client '/api/v1/public/ai/room-advisor/knowledge/ask' $ask -ExpectedStatus 503
    Invoke-RagLauncher 'DbRecovery'
    $recovered=Invoke-RagApi $client '/api/v1/public/ai/room-advisor/knowledge/ask' $ask
    if($recovered.result.status -cne 'GROUNDED_ANSWER'){throw 'RAG_RECOVERY_FAILED'}
    $report.phases+=@{phase='db-outage-recovery';status='PASS'}; $report.ledger=Assert-RagLedgerReady $config; $report.status='PASS_TECHNICAL_ONLY'
} catch {
    $report.status='FAIL_OR_BLOCKED';$report.failedPhase=$phase;$report.reason='Gate/config/technical check failed; no raw exception, token or provider body recorded.'
} finally {
    $script:RagAdminBearer=$null
    if($client){$client.Dispose()}
    if($owned){
        try {
            Invoke-RagLauncher 'Stop'; Invoke-RagLauncher 'DbRecovery'
            if(-not $KeepKnowledge){Invoke-RagLauncher 'Restore';$report.snapshotRestored=$true}else{$report.snapshotRestored=$false;$report.knowledgePreserved=$true}
            Invoke-RagLauncher 'DbOutage';$report.ownedProcessesStopped=$true
        }catch{$report.status='FAIL_OR_BLOCKED';$report.cleanup='OWNERSHIP_OR_RESTORE_REQUIRES_REVIEW'}
        try{$report.ledger=Get-RagLedgerSummary $config}catch{$report.ledger='UNAVAILABLE / DO NOT RESUME LIVE'}
        $receipt=Join-Path $config.RuntimeDirectory ('live-receipt-'+[Guid]::NewGuid().ToString('N')+'.json')
        [IO.File]::WriteAllText($receipt,($report|ConvertTo-Json -Depth 15),[Text.UTF8Encoding]::new($false))
    }
    if($lock){$lock.Dispose()}
}
$report|ConvertTo-Json -Depth 15
if($report.status -cne 'PASS_TECHNICAL_ONLY'){exit 1}
