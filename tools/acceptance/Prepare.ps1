# Prepare.ps1 — Sandbox Preflight and Runtime Secret Generation
[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Resolve-Path "$ScriptDir/../.."
$RuntimeDir = "$ScriptDir/.runtime"

Write-Host "=== [1/3] Kiểm tra tính khả dụng của các cổng local ===" -ForegroundColor Cyan

$RequiredPorts = @(
    @{ Name = "PostgreSQL"; Port = 15432 },
    @{ Name = "Redis"; Port = 16379 },
    @{ Name = "Keycloak"; Port = 18081 },
    @{ Name = "Backend"; Port = 18080 },
    @{ Name = "Frontend"; Port = 13000 }
)

$Conflict = $false
foreach ($p in $RequiredPorts) {
    try {
        $tcp = New-Object System.Net.Sockets.TcpClient
        $iar = $tcp.BeginConnect('127.0.0.1', $p.Port, $null, $null)
        $wait = $iar.AsyncWaitHandle.WaitOne(300, $false)
        if ($wait -and $tcp.Connected) {
            $tcp.EndConnect($iar)
            $tcp.Close()
            Write-Host "  [X] Cổng $($p.Port) ($($p.Name)) ĐANG BỊ CHIẾM DỤNG!" -ForegroundColor Red
            $Conflict = $true
        } else {
            $tcp.Close()
            Write-Host "  [OK] Cổng $($p.Port) ($($p.Name)) sẵn sàng." -ForegroundColor Green
        }
    } catch {
        Write-Host "  [OK] Cổng $($p.Port) ($($p.Name)) sẵn sàng." -ForegroundColor Green
    }
}

if ($Conflict) {
    throw "Phát hiện xung đột cổng! Vui lòng giải phóng các cổng trên trước khi khởi chạy sandbox acceptance."
}

Write-Host "=== [2/3] Khởi tạo thư mục runtime và cấu hình bảo mật ===" -ForegroundColor Cyan
if (-not (Test-Path $RuntimeDir)) {
    New-Item -ItemType Directory -Path $RuntimeDir -Force | Out-Null
}

$EnvConfig = @{
    DB_HOST = "localhost"
    DB_PORT = 15432
    DB_NAME = "hotel_acceptance_db"
    DB_USER = "postgres"
    DB_PASSWORD = "AcceptanceSecret123!"
    REDIS_HOST = "localhost"
    REDIS_PORT = 16379
    KEYCLOAK_SERVER_URL = "http://localhost:18081"
    KEYCLOAK_REALM = "hotel-acceptance"
    KEYCLOAK_CLIENT_ID = "hotel-backend"
    KEYCLOAK_CLIENT_SECRET = "hotel-acceptance-secret-key-123456"
    KEYCLOAK_PORT = 18081
    BACKEND_PORT = 18080
    FRONTEND_PORT = 13000
    HOTEL_SCHEDULING_ENABLED = "false"
    HOTEL_AI_PERSONALIZATION_SYNTHETIC_EVALUATION_ENABLED = "true"
    HOTEL_AI_ADVISOR_SECURITY_ANON_TOKEN_SECRET = "7f3c1a9e5b2d8460c4f19a3e7b2d5c80a1b2c3d4"
}

$EnvJsonPath = "$RuntimeDir/env.json"
$EnvConfig | ConvertTo-Json -Depth 5 | Set-Content -Path $EnvJsonPath -Encoding UTF8
Write-Host "  Đã lưu cấu hình runtime vào: $EnvJsonPath" -ForegroundColor Green

Write-Host "=== [3/3] Sinh manifest thông tin nghiệm thu ===" -ForegroundColor Cyan
$Manifest = @{
    generatedAt = (Get-Date).ToString("yyyy-MM-ddTHH:mm:sszzz")
    projectName = "hotel-m1-acceptance"
    environment = "local-sandbox"
    testDates = @{
        runDate = (Get-Date).ToString("yyyy-MM-dd")
        checkInDate = (Get-Date).AddDays(1).ToString("yyyy-MM-dd")
        checkOutDate = (Get-Date).AddDays(3).ToString("yyyy-MM-dd")
    }
    hotels = @(
        @{ id = 1; name = "Acceptance Luxury Hanoi"; region = "NORTH" },
        @{ id = 2; name = "Acceptance Grand Da Nang"; region = "CENTRAL" }
    )
    users = @(
        @{ username = "admin"; role = "CHAIN_ADMIN"; scope = "CHAIN" },
        @{ username = "region_mgr"; role = "REGION_MANAGER"; scope = "REGION (NORTH)" },
        @{ username = "manager_h1"; role = "PROPERTY_MANAGER"; scope = "PROPERTY (Hotel 1)" },
        @{ username = "manager_h2"; role = "PROPERTY_MANAGER"; scope = "PROPERTY (Hotel 2)" },
        @{ username = "reception_h1"; role = "RECEPTIONIST"; scope = "PROPERTY (Hotel 1)" },
        @{ username = "test_customer"; role = "CUSTOMER"; scope = "PUBLIC" }
    )
}

$ManifestPath = "$RuntimeDir/manifest.json"
$Manifest | ConvertTo-Json -Depth 5 | Set-Content -Path $ManifestPath -Encoding UTF8
Write-Host "  Đã lưu manifest vào: $ManifestPath" -ForegroundColor Green

Write-Host "Hoàn tất chuẩn bị Sandbox Acceptance thành công!" -ForegroundColor Cyan
