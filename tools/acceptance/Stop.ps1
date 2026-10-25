# Stop.ps1 — Stop only acceptance-owned application processes and Compose resources
[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
. (Join-Path $ScriptDir 'Runtime.ps1')
$AcceptanceRuntime = Initialize-AcceptanceRuntime -ScriptDirectory $ScriptDir
$ProcessStateFile = Join-Path $AcceptanceRuntime.RuntimeDirectory 'processes.json'
$ComposeArgs = $AcceptanceRuntime.ComposeArguments

Write-Host '=== Dừng các ứng dụng thuộc sandbox acceptance ===' -ForegroundColor Cyan
$appsStopped = Stop-AcceptanceTrackedProcesses -StateFile $ProcessStateFile
if (-not $appsStopped) {
    throw 'Có tiến trình không khớp manifest. Đã bỏ qua tiến trình đó và chưa dừng container để tránh làm gián đoạn ứng dụng khác.'
}

Write-Host '=== Dừng các container thuộc project hotel-m1-acceptance ===' -ForegroundColor Cyan
docker compose @ComposeArgs down
if ($LASTEXITCODE -ne 0) {
    throw "Không dừng được Docker Compose acceptance (exit code $LASTEXITCODE)."
}

Write-Host 'Sandbox Acceptance đã dừng.' -ForegroundColor Green
