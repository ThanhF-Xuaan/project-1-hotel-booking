# Restore.ps1 — Restore Acceptance Database from Snapshot
[CmdletBinding()]
param([string]$SnapshotFile)

$ErrorActionPreference = 'Stop'
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$AcceptanceRuntime = $null
. (Join-Path $ScriptDir 'Runtime.ps1')
$AcceptanceRuntime = Initialize-AcceptanceRuntime -ScriptDirectory $ScriptDir
$ProjectRoot = $AcceptanceRuntime.ProjectRoot
$RuntimeDir = $AcceptanceRuntime.RuntimeDirectory
$EnvConfig = $AcceptanceRuntime.Environment
$ComposeArgs = $AcceptanceRuntime.ComposeArguments
if (-not $SnapshotFile) { $SnapshotFile = "$RuntimeDir/snapshot.sql" }

if (-not (Test-Path $SnapshotFile)) {
    throw "Không tìm thấy file snapshot: $SnapshotFile! Hãy chạy Snapshot.ps1 trước."
}

$PgContainerId = (docker compose @ComposeArgs ps -q acceptance_postgres).Trim()

if (-not $PgContainerId) {
    throw "PostgreSQL container không hoạt động. Hãy chạy Start.ps1 trước!"
}

Write-Host "=== Khôi phục Cơ sở dữ liệu Sandbox từ Snapshot ===" -ForegroundColor Cyan
docker cp $SnapshotFile "${PgContainerId}:/tmp/snapshot.sql"
if ($LASTEXITCODE -ne 0) {
    throw "Không sao chép được snapshot vào PostgreSQL container (exit code $LASTEXITCODE)."
}

docker exec $PgContainerId psql -v ON_ERROR_STOP=1 --single-transaction -U $($EnvConfig.DB_USER) -d $($EnvConfig.DB_NAME) -f /tmp/snapshot.sql

if ($LASTEXITCODE -ne 0) {
    throw "Khôi phục dữ liệu từ snapshot thất bại!"
}

Write-Host "  Khôi phục thành công! Đang kiểm tra tính toàn vẹn dữ liệu..." -ForegroundColor Green
$VerificationSql = "SELECT 'hotels' as tbl, count(*) from hotels UNION ALL SELECT 'hotel_room_types', count(*) from hotel_room_types UNION ALL SELECT 'room_availability', count(*) from room_availability UNION ALL SELECT 'staffs', count(*) from staffs;"
docker exec -i $PgContainerId psql -v ON_ERROR_STOP=1 -U $($EnvConfig.DB_USER) -d $($EnvConfig.DB_NAME) -c "$VerificationSql"
if ($LASTEXITCODE -ne 0) {
    throw 'Kiểm tra dữ liệu sau restore thất bại.'
}

Write-Host "Khôi phục hoàn tất và dữ liệu hợp lệ!" -ForegroundColor Green
