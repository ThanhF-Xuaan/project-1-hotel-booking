# One-command local technical acceptance. Requires an idle, disposable acceptance sandbox.
[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = (Resolve-Path (Join-Path $ScriptDir '../..')).Path
$RuntimeDir = Join-Path $ScriptDir '.runtime'
$Shell = (Get-Process -Id $PID).Path
$RunId = (Get-Date).ToString('yyyyMMdd-HHmmss-fff') + '-' + [Guid]::NewGuid().ToString('N').Substring(0, 8)
$RunDir = Join-Path $RuntimeDir "runs/$RunId"
$Results = [System.Collections.Generic.List[object]]::new()
$Problems = [System.Collections.Generic.List[string]]::new()
$OwnedInfrastructure = $false
$SnapshotReady = $false
$Restored = $false
$RunLock = $null
$Backend = $null
$UiResults = $null
$EnvironmentBefore = @{}
foreach ($Name in @('DB_NAME', 'DB_USER', 'DB_PASSWORD', 'DB_PORT', 'REDIS_PORT',
    'KEYCLOAK_ADMIN', 'KEYCLOAK_ADMIN_PASSWORD', 'KEYCLOAK_PORT', 'CI')) {
    $EnvironmentBefore[$Name] = [Environment]::GetEnvironmentVariable($Name, 'Process')
}

function Invoke-Stage {
    param([string]$Name, [string]$Executable, [string[]]$Arguments, [int[]]$AllowedExitCodes = @(0),
        [switch]$DetachedChildren)
    $LogPath = Join-Path $RunDir "$Name.log"
    Write-Host "[$Name] Đang chạy; log: $LogPath" -ForegroundColor Cyan
    $Timer = [Diagnostics.Stopwatch]::StartNew()
    $global:LASTEXITCODE = -1
    $Code = -1
    try {
        if ($DetachedChildren) {
            # Native piping can wait for handles inherited by Start.ps1's background apps.
            # Wait only for the launcher process; do not use Start-Process -Wait (waits for its tree).
            $QuotedArguments = @($Arguments | ForEach-Object {
                if ($_ -match '["\r\n]' -or $_.EndsWith('\')) { throw 'Invalid script-stage argument.' }
                '"' + $_ + '"'
            }) -join ' '
            $Process = Start-Process -FilePath $Executable -ArgumentList $QuotedArguments -WindowStyle Hidden `
                -PassThru -RedirectStandardOutput $LogPath -RedirectStandardError "$LogPath.stderr"
            try {
                $Process.WaitForExit()
                if ($null -eq $Process.ExitCode) { throw 'Không lấy được exit code của stage.' }
                $Code = $Process.ExitCode
            } finally { $Process.Dispose() }
        } else {
            & $Executable @Arguments *> $LogPath
            $Code = $LASTEXITCODE
        }
    } catch {
        $_.Exception.Message | Set-Content -LiteralPath $LogPath -Encoding UTF8
    }
    $Timer.Stop()
    $Status = if ($Code -in $AllowedExitCodes) { 'PASS' } else { 'FAIL' }
    $Results.Add([PSCustomObject]@{ stage = $Name; status = $Status; exitCode = $Code;
        seconds = [Math]::Round($Timer.Elapsed.TotalSeconds, 2); log = $LogPath;
        command = "$Executable $($Arguments -join ' ')" })
    if ($Status -eq 'FAIL') { throw "$Name thất bại (exit $Code); xem $LogPath" }
    Write-Host "[$Name] Hoàn tất ($($Timer.Elapsed.TotalSeconds.ToString('F1'))s)." -ForegroundColor Green
}

function Invoke-ScriptStage {
    param([string]$Name, [string]$Script, [string[]]$Arguments = @(), [int[]]$AllowedExitCodes = @(0))
    Invoke-Stage -Name $Name -Executable $Shell -Arguments (@('-NoLogo', '-NoProfile', '-ExecutionPolicy',
        'Bypass', '-File', (Join-Path $ScriptDir $Script)) + $Arguments) -AllowedExitCodes $AllowedExitCodes -DetachedChildren
}

try {
    New-Item -ItemType Directory -Path $RuntimeDir -Force | Out-Null
    # Exclusive handle prevents a second Verify process from reseeding/restoring this sandbox.
    $RunLock = [IO.File]::Open((Join-Path $RuntimeDir 'verify.lock'), 'OpenOrCreate', 'ReadWrite', 'None')
    New-Item -ItemType Directory -Path $RunDir | Out-Null
    if (-not (Test-Path -LiteralPath (Join-Path $RuntimeDir 'env.json'))) {
        Invoke-ScriptStage -Name 'prepare' -Script 'Prepare.ps1'
    }
    . (Join-Path $ScriptDir 'Runtime.ps1')
    $Runtime = Initialize-AcceptanceRuntime -ScriptDirectory $ScriptDir
    $Config = $Runtime.Environment
    $ComposeArgs = $Runtime.ComposeArguments
    if ($Config.DB_HOST -ne 'localhost' -or $Config.DB_PORT -ne 15432 -or
        $Config.DB_NAME -ne 'hotel_acceptance_db' -or $Config.DB_USER -ne 'postgres' -or
        $Config.REDIS_HOST -ne 'localhost' -or $Config.REDIS_PORT -ne 16379 -or
        $Config.KEYCLOAK_PORT -ne 18081 -or $Config.KEYCLOAK_REALM -ne 'hotel-acceptance' -or
        $Config.KEYCLOAK_SERVER_URL -ne 'http://localhost:18081' -or
        $Config.BACKEND_PORT -ne 18080 -or $Config.FRONTEND_PORT -ne 13000 -or
        $Config.HOTEL_AI_PERSONALIZATION_SYNTHETIC_EVALUATION_ENABLED -ne 'true') {
        throw 'Runtime phải là sandbox acceptance cố định, bật U14 synthetic; không tự sửa cấu hình.'
    }
    if (Test-Path -LiteralPath (Join-Path $RuntimeDir 'processes.json')) {
        throw 'Có manifest ứng dụng đang chạy; dừng sandbox trước khi Verify.'
    }
    $ExistingContainers = @(docker ps --filter 'label=com.docker.compose.project=hotel-m1-acceptance' --format '{{.ID}}')
    if ($LASTEXITCODE -ne 0) { throw 'Docker chưa sẵn sàng.' }
    if ($ExistingContainers.Count -gt 0) { throw 'Sandbox đang chạy; không reseed dữ liệu đang được sử dụng.' }
    foreach ($Port in @(15432, 16379, 18081, 18080, 13000)) {
        if (Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue) {
            throw "Cổng $Port bị chiếm; không dừng tiến trình này."
        }
    }
    $env:CI = 'true'
    $BaseSha = (git -C $ProjectRoot rev-parse HEAD).Trim()
    if ($LASTEXITCODE -ne 0) { throw 'Không xác định được Git SHA.' }
    $Branch = (git -C $ProjectRoot branch --show-current).Trim()
    $GitStatus = @(git -C $ProjectRoot status --short)
    $ScriptHashes = @(Get-ChildItem -LiteralPath $ScriptDir -Filter '*.ps1' | ForEach-Object {
        [PSCustomObject]@{ file = $_.Name; sha256 = (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant() }
    })
    [PSCustomObject]@{ runId = $RunId; baseSha = $BaseSha; branch = $Branch; gitStatus = $GitStatus;
        scriptHashes = $ScriptHashes; database = '127.0.0.1:15432/hotel_acceptance_db'; redis = '127.0.0.1:16379';
        providers = 'Gemini disabled; deterministic/synthetic only' } |
        ConvertTo-Json -Depth 6 | Set-Content -LiteralPath (Join-Path $RunDir 'run.json') -Encoding UTF8

    $OwnedInfrastructure = $true
    Invoke-ScriptStage -Name 'infrastructure' -Script 'Start.ps1' -Arguments @('-InfrastructureOnly')
    $SnapshotFile = Join-Path $RunDir 'snapshot.sql'
    Invoke-ScriptStage -Name 'snapshot' -Script 'Snapshot.ps1' -Arguments @('-SnapshotFile', $SnapshotFile)
    if (-not (Test-Path -LiteralPath $SnapshotFile) -or (Get-Item -LiteralPath $SnapshotFile).Length -eq 0) {
        throw 'Snapshot không có dữ liệu; dừng trước test.'
    }
    $SnapshotReady = $true
    $SnapshotHash = (Get-FileHash -LiteralPath $SnapshotFile -Algorithm SHA256).Hash

    Invoke-ScriptStage -Name 'maven' -Script 'Run-BackendTests.ps1' -Arguments @('-EvidenceDirectory', $RunDir)
    $Backend = Get-Content -LiteralPath (Join-Path $RunDir 'backend.json') -Raw | ConvertFrom-Json
    if ($Backend.status -ne 'PASS' -or $Backend.tests -lt 1) { throw 'Thiếu evidence Maven PASS.' }
    Invoke-Stage -Name 'source-audit' -Executable (Get-Command node -CommandType Application | Select-Object -First 1).Source `
        -Arguments @((Join-Path $ProjectRoot 'tools/t12/verify_lazy_route_reaudit.cjs'))
    Push-Location (Join-Path $ProjectRoot 'frontend')
    try {
        Invoke-Stage -Name 'dependency-audit' -Executable (Get-Command npm.cmd -CommandType Application | Select-Object -First 1).Source `
            -Arguments @('audit', '--audit-level=high')
    } finally { Pop-Location }
    Invoke-ScriptStage -Name 'applications' -Script 'Start.ps1' -Arguments @('-ApplicationsOnly')
    $UiResultFile = Join-Path $RunDir 'application-suites.json'
    # The child remains PARTIAL/exit 1 because Maven was run separately; validate every result below.
    Invoke-ScriptStage -Name 'application-suites' -Script 'Test.ps1' -Arguments @('-SkipMaven',
        '-ReportPath', (Join-Path $RunDir 'application-suites.md'), '-ResultPath', $UiResultFile) -AllowedExitCodes @(1)
    $UiResults = Get-Content -LiteralPath $UiResultFile -Raw | ConvertFrom-Json
    $ExpectedSuites = @('Maven Core Modules', 'T12 Contract Schema', 'Portability Validation',
        'ESLint Verification', 'Vite Build & Types', 'Playwright API Suite', 'Playwright Browser Suite')
    $ActualSuites = @($UiResults.suites | ForEach-Object { $_.Suite })
    if ($UiResults.status -ne 'PARTIAL' -or $ActualSuites.Count -ne 7 -or
        @(Compare-Object $ExpectedSuites $ActualSuites).Count -ne 0) { throw 'Danh sách suite không khớp kế hoạch.' }
    foreach ($Suite in $UiResults.suites) {
        $ExpectedStatus = if ($Suite.Suite -eq 'Maven Core Modules') { 'SKIPPED' } else { 'PASS' }
        if ($Suite.Status -ne $ExpectedStatus) { throw "Suite $($Suite.Suite) có trạng thái $($Suite.Status)." }
    }
} catch {
    $Problems.Add($_.Exception.Message)
    Write-Warning $_.Exception.Message
} finally {
    if ($OwnedInfrastructure) {
        try {
            if (-not (Stop-AcceptanceTrackedProcesses -StateFile (Join-Path $RuntimeDir 'processes.json'))) {
                throw 'Không xác minh được toàn bộ tiến trình; giữ hạ tầng, không restore khi app có thể còn ghi DB.'
            }
            if (Get-NetTCPConnection -State Listen -LocalPort 18080, 13000 -ErrorAction SilentlyContinue) {
                throw 'Ứng dụng còn listener sau khi dừng; giữ hạ tầng để kiểm tra.'
            }
            if ($SnapshotReady) {
                if ((Get-FileHash -LiteralPath $SnapshotFile -Algorithm SHA256).Hash -ne $SnapshotHash) {
                    throw 'Snapshot đã đổi trong lần chạy; không restore.'
                }
                Invoke-ScriptStage -Name 'restore' -Script 'Restore.ps1' -Arguments @('-SnapshotFile', $SnapshotFile)
                $Restored = $true
            }
            Invoke-ScriptStage -Name 'stop' -Script 'Stop.ps1'
        } catch {
            $Problems.Add("Cleanup: $($_.Exception.Message)")
            Write-Warning $Problems[$Problems.Count - 1]
        }
    }
    foreach ($Name in $EnvironmentBefore.Keys) {
        [Environment]::SetEnvironmentVariable($Name, $EnvironmentBefore[$Name], 'Process')
    }
    if ($RunLock) { $RunLock.Dispose() }
}

if (-not (Test-Path -LiteralPath $RunDir)) { exit 1 }
$Status = if ($Problems.Count -eq 0 -and $Restored -and $Backend -and $UiResults) { 'PASS' } else { 'FAIL' }
$ReportDirectory = Join-Path $ProjectRoot 'docs/member1/acceptance/runs'
New-Item -ItemType Directory -Path $ReportDirectory -Force | Out-Null
$ReportPath = Join-Path $ReportDirectory "acceptance-$RunId.md"
$Lines = @('# Local technical acceptance', '', "- **Result:** $Status", "- **Run:** $RunId",
    "- **Base SHA:** $BaseSha", "- **Branch:** $Branch", "- **Runtime evidence:** $RunDir",
    '- **Environment:** dedicated loopback PostgreSQL 15432, Redis 16379, Keycloak 18081; synthetic data only.',
    "- **Snapshot restored:** $Restored; Docker volumes retained.", '',
    '| Stage | Status | Exit | Seconds |', '|---|---|---:|---:|')
foreach ($Result in $Results) { $Lines += "| $($Result.stage) | $($Result.status) | $($Result.exitCode) | $($Result.seconds) |" }
if ($Backend) {
    $Lines += @('', "Maven: $($Backend.tests) tests, $($Backend.skipped) skipped; actual class counts in backend.json.",
        'PostgreSQL 3/3 and Redis 4/4 must execute without skips before Maven evidence is accepted.')
    foreach ($Skipped in @($Backend.suites | Where-Object skipped -gt 0)) {
        $Lines += "- Skipped: $($Skipped.name), $($Skipped.skipped) test(s)."
    }
}
if ($UiResults) {
    $Lines += @('', '| Application suite | Status | Command |', '|---|---|---|')
    foreach ($Suite in $UiResults.suites) { $Lines += "| $($Suite.Suite) | $($Suite.Status) | $($Suite.Command) |" }
    $Lines += @('', 'The child application report remains PARTIAL because it skips Maven. The supervisor combines this with the separate full Maven evidence; it does not treat a skipped suite as passed.')
}
$Lines += @('', '## Failures', '')
if ($Problems.Count) { foreach ($Problem in $Problems) { $Lines += "- $Problem" } } else { $Lines += 'None.' }
$Lines += @('', '## Scope and evidence', '',
    'Local technical acceptance only. T05 official export compatibility and real U14 consent/history remain unverified. T12 draft synthetic harness is not approved gold or live model evaluation.',
    '', 'Commands, exit codes, SHA, dirty/untracked paths and script hashes are preserved in run.json/results.json and stage logs. Runtime configuration and snapshot are ignored; do not commit them.', '',
    '| Script | SHA-256 at run |', '|---|---|')
foreach ($Hash in $ScriptHashes) { $Lines += "| $($Hash.file) | $($Hash.sha256) |" }
$Lines | Set-Content -LiteralPath $ReportPath -Encoding UTF8
[PSCustomObject]@{ status = $Status; problems = @($Problems.ToArray()); restored = $Restored;
    stages = @($Results.ToArray()); report = $ReportPath } |
    ConvertTo-Json -Depth 6 | Set-Content -LiteralPath (Join-Path $RunDir 'results.json') -Encoding UTF8
Write-Host "Kết quả $Status. Báo cáo: $ReportPath"
if ($Status -ne 'PASS') { exit 1 }
exit 0
