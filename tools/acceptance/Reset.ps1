# Reset.ps1 — Snapshot first, then clean sandbox resources labeled with project
[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
. (Join-Path $ScriptDir 'Runtime.ps1')
$AcceptanceRuntime = Initialize-AcceptanceRuntime -ScriptDirectory $ScriptDir
$ComposeArgs = $AcceptanceRuntime.ComposeArguments

$appsStopped = Stop-AcceptanceTrackedProcesses -StateFile (Join-Path $AcceptanceRuntime.RuntimeDirectory 'processes.json')
if (-not $appsStopped) {
    throw 'Không reset sandbox khi chưa thể xác minh tiến trình ứng dụng thuộc acceptance.'
}

Write-Host "=== [1/2] Chụp Snapshot trước khi Reset để đảm bảo an toàn dữ liệu ===" -ForegroundColor Cyan
& "$ScriptDir/Snapshot.ps1"
if ($LASTEXITCODE -and $LASTEXITCODE -ne 0) {
    throw "Không tạo được snapshot; dừng reset để bảo toàn Docker volume."
}

Write-Host "=== [2/2] Xóa tài nguyên gắn nhãn hotel-m1-acceptance ===" -ForegroundColor Cyan
docker compose @ComposeArgs down -v
if ($LASTEXITCODE -ne 0) {
    throw "Không reset được Docker Compose acceptance (exit code $LASTEXITCODE)."
}

Write-Host "Reset Sandbox Acceptance hoàn tất." -ForegroundColor Green
