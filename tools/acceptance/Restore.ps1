# Restore.ps1 — Restore Acceptance Database from Snapshot
[CmdletBinding()]
param([string]$SnapshotFile)

$ErrorActionPreference = 'Stop'
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$AcceptanceRuntime = $null
$UseDefaultSnapshot = [string]::IsNullOrWhiteSpace($SnapshotFile)
. (Join-Path $ScriptDir 'Runtime.ps1')
$AcceptanceRuntime = Initialize-AcceptanceRuntime -ScriptDirectory $ScriptDir
$ProjectRoot = $AcceptanceRuntime.ProjectRoot
$RuntimeDir = $AcceptanceRuntime.RuntimeDirectory
$EnvConfig = $AcceptanceRuntime.Environment
$ComposeArgs = $AcceptanceRuntime.ComposeArguments
if (-not $SnapshotFile) { $SnapshotFile = "$RuntimeDir/snapshot.sql" }

$MetadataFile = if ($UseDefaultSnapshot) {
    Join-Path $RuntimeDir 'snapshot-metadata.json'
} else {
    [IO.Path]::ChangeExtension($SnapshotFile, 'metadata.json')
}
if (-not (Test-Path -LiteralPath $MetadataFile)) {
    throw "Không tìm thấy metadata snapshot: $MetadataFile. Không thể xác minh restore."
}
$SnapshotMetadata = Get-Content -LiteralPath $MetadataFile -Raw | ConvertFrom-Json
if ([string]$SnapshotMetadata.database -ne [string]$EnvConfig.DB_NAME -or -not $SnapshotMetadata.expectedTables) {
    throw 'Metadata snapshot không khớp DB acceptance hoặc thiếu bằng chứng bảng trước snapshot.'
}

if (-not (Test-Path $SnapshotFile)) {
    throw "Không tìm thấy file snapshot: $SnapshotFile! Hãy chạy Snapshot.ps1 trước."
}
if ([IO.Path]::GetFullPath([string]$SnapshotMetadata.snapshotFile) -ne [IO.Path]::GetFullPath($SnapshotFile) -or
    (Get-Item -LiteralPath $SnapshotFile).Length -ne [long]$SnapshotMetadata.fileSize) {
    throw 'Snapshot không khớp file/size đã ghi trong metadata.'
}

$PgContainers = @(docker compose @ComposeArgs ps -q acceptance_postgres)

if ($LASTEXITCODE -ne 0 -or $PgContainers.Count -ne 1 -or -not $PgContainers[0]) {
    throw "PostgreSQL container không hoạt động. Hãy chạy Start.ps1 trước!"
}
$PgContainerId = ([string]$PgContainers[0]).Trim()
$PublishedPort = docker port $PgContainerId '5432/tcp'
if ($LASTEXITCODE -ne 0 -or $PublishedPort -ne '127.0.0.1:15432') {
    throw 'Container PostgreSQL không khớp cổng sandbox loopback đã duyệt.'
}

Write-Host "=== Khôi phục Cơ sở dữ liệu Sandbox từ Snapshot ===" -ForegroundColor Cyan
Write-Host "  Target: hotel-m1-acceptance / hotel_acceptance_db @ 127.0.0.1:15432" -ForegroundColor Gray
docker cp $SnapshotFile "${PgContainerId}:/tmp/snapshot.sql"
if ($LASTEXITCODE -ne 0) {
    throw "Không sao chép được snapshot vào PostgreSQL container (exit code $LASTEXITCODE)."
}

$SchemaResetSql = "DROP SCHEMA IF EXISTS public CASCADE; CREATE SCHEMA public AUTHORIZATION $($EnvConfig.DB_USER); GRANT ALL ON SCHEMA public TO $($EnvConfig.DB_USER); GRANT USAGE ON SCHEMA public TO public;"
docker exec $PgContainerId psql -v ON_ERROR_STOP=1 --single-transaction -U $($EnvConfig.DB_USER) -d $($EnvConfig.DB_NAME) -c $SchemaResetSql
if ($LASTEXITCODE -ne 0) {
    throw "Không thể thay schema public của sandbox trước khi restore (exit code $LASTEXITCODE)."
}

docker exec $PgContainerId psql -v ON_ERROR_STOP=1 --single-transaction -U $($EnvConfig.DB_USER) -d $($EnvConfig.DB_NAME) -f /tmp/snapshot.sql

if ($LASTEXITCODE -ne 0) {
    throw "Khôi phục dữ liệu từ snapshot thất bại!"
}

Write-Host "  Khôi phục thành công! Đang đối chiếu các bảng đã snapshot..." -ForegroundColor Green
foreach ($Table in @('hotels', 'hotel_room_types', 'room_availability', 'staffs')) {
    $ExpectedProperty = $SnapshotMetadata.expectedTables.PSObject.Properties[$Table]
    if (-not $ExpectedProperty) { throw "Metadata snapshot thiếu bảng $Table." }

    $ExistsOutput = docker exec $PgContainerId psql -U $($EnvConfig.DB_USER) -d $($EnvConfig.DB_NAME) -Atc `
        "SELECT to_regclass('public.$Table') IS NOT NULL;"
    if ($LASTEXITCODE -ne 0) { throw "Không thể xác nhận bảng $Table sau restore." }
    $Exists = ([string]$ExistsOutput).Trim()
    if ($null -eq $ExpectedProperty.Value) {
        if ($Exists -ne 'f') { throw "Bảng $Table xuất hiện sau restore nhưng không có trong snapshot." }
        continue
    }
    if ($Exists -ne 't') { throw "Bảng $Table bị thiếu sau restore." }

    $CountOutput = docker exec $PgContainerId psql -U $($EnvConfig.DB_USER) -d $($EnvConfig.DB_NAME) -Atc `
        "SELECT count(*) FROM public.$Table;"
    if ($LASTEXITCODE -ne 0) { throw "Không thể đếm bảng $Table sau restore." }
    [long]$ActualCount = 0
    if (-not [long]::TryParse(([string]$CountOutput).Trim(), [ref]$ActualCount) -or
        $ActualCount -ne [long]$ExpectedProperty.Value) {
        throw "Số dòng bảng $Table sau restore không khớp snapshot."
    }
}

Write-Host "Khôi phục hoàn tất; trạng thái bảng khớp snapshot!" -ForegroundColor Green
