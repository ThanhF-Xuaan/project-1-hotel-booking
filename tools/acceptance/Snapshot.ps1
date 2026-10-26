# Snapshot.ps1 — Backup Acceptance DB and State
[CmdletBinding()]
param([string]$SnapshotFile)

$ErrorActionPreference = 'Stop'
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
. (Join-Path $ScriptDir 'Runtime.ps1')
$AcceptanceRuntime = Initialize-AcceptanceRuntime -ScriptDirectory $ScriptDir
$ProjectRoot = $AcceptanceRuntime.ProjectRoot
$RuntimeDir = $AcceptanceRuntime.RuntimeDirectory
$EnvConfig = $AcceptanceRuntime.Environment
$ComposeArgs = $AcceptanceRuntime.ComposeArguments

$PgContainerId = (docker compose @ComposeArgs ps -q acceptance_postgres).Trim()

if (-not $PgContainerId) {
    throw "PostgreSQL container không hoạt động. Không thể tạo snapshot!"
}

Write-Host "=== Tạo Snapshot cho Cơ sở dữ liệu Sandbox ===" -ForegroundColor Cyan
$CustomSnapshot = -not [string]::IsNullOrWhiteSpace($SnapshotFile)
if (-not $CustomSnapshot) { $SnapshotFile = "$RuntimeDir/snapshot.sql" }
if ($CustomSnapshot -and (Test-Path -LiteralPath $SnapshotFile)) {
    throw "Snapshot đã tồn tại, không ghi đè: $SnapshotFile"
}

# Dump inside container to /tmp/snapshot.sql and copy to host to preserve raw UTF-8 binary integrity
docker exec $PgContainerId pg_dump -U $($EnvConfig.DB_USER) -d $($EnvConfig.DB_NAME) --clean --if-exists -f /tmp/snapshot.sql
if ($LASTEXITCODE -ne 0) {
    throw "Tạo DB snapshot thất bại bên trong container!"
}
docker cp "${PgContainerId}:/tmp/snapshot.sql" $SnapshotFile

if ($LASTEXITCODE -ne 0 -or -not (Test-Path $SnapshotFile)) {
    throw "Sao chép DB snapshot từ container thất bại!"
}

$Size = (Get-Item $SnapshotFile).Length
Write-Host "  Đã lưu snapshot thành công ($Size bytes) vào: $SnapshotFile" -ForegroundColor Green

$SnapshotMeta = @{
    createdAt = (Get-Date).ToString("yyyy-MM-ddTHH:mm:sszzz")
    snapshotFile = $SnapshotFile
    fileSize = $Size
    database = $EnvConfig.DB_NAME
}

$MetadataFile = if ($CustomSnapshot) { [IO.Path]::ChangeExtension($SnapshotFile, 'metadata.json') } else { "$RuntimeDir/snapshot-metadata.json" }
$SnapshotMeta | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath $MetadataFile -Encoding UTF8
Write-Host "Hoàn tất Snapshot!" -ForegroundColor Green
