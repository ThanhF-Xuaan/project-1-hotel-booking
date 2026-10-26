# Start.ps1 — Launch the isolated Acceptance Sandbox and local applications
[CmdletBinding()]
param(
    [switch]$InfrastructureOnly,
    [switch]$ApplicationsOnly,
    [switch]$ServicesOnly
)

$ErrorActionPreference = 'Stop'
$SelectedModes = @()
if ($InfrastructureOnly) { $SelectedModes += 'InfrastructureOnly' }
if ($ApplicationsOnly) { $SelectedModes += 'ApplicationsOnly' }
if ($ServicesOnly) { $SelectedModes += 'ServicesOnly' }
if ($SelectedModes.Count -gt 1) {
    throw 'Chỉ chọn một trong InfrastructureOnly, ApplicationsOnly hoặc ServicesOnly.'
}
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RuntimeDir = Join-Path $ScriptDir '.runtime'
$RuntimeFile = Join-Path $RuntimeDir 'env.json'

if (-not (Test-Path -LiteralPath $RuntimeFile)) {
    Write-Host "Chưa có cấu hình runtime. Đang chạy Prepare.ps1..." -ForegroundColor Yellow
    & (Join-Path $ScriptDir 'Prepare.ps1')
    if (-not $?) {
        throw 'Prepare.ps1 thất bại.'
    }
}

. (Join-Path $ScriptDir 'Runtime.ps1')
$AcceptanceRuntime = Initialize-AcceptanceRuntime -ScriptDirectory $ScriptDir
$ProjectRoot = $AcceptanceRuntime.ProjectRoot
$EnvConfig = $AcceptanceRuntime.Environment
$ComposeArgs = $AcceptanceRuntime.ComposeArguments
$ProcessStateFile = Join-Path $RuntimeDir 'processes.json'
$MavenWrapper = Join-Path $ProjectRoot 'backend/mvnw.cmd'
$ViteExecutable = Join-Path $ProjectRoot 'frontend/node_modules/.bin/vite.cmd'

if (-not (Test-Path -LiteralPath $MavenWrapper)) {
    throw "Không tìm thấy Maven wrapper: $MavenWrapper"
}
if (-not (Test-Path -LiteralPath $ViteExecutable)) {
    throw "Thiếu frontend dependencies. Chạy 'npm ci' trong thư mục frontend rồi thử lại."
}

if (Test-Path -LiteralPath $ProcessStateFile) {
    throw "Đã có manifest tiến trình sandbox. Chạy Stop.ps1 trước khi khởi động lại; không tiến trình nào bị dừng tự động."
}

function Test-LocalPortOpen {
    param([int]$Port)
    $client = [System.Net.Sockets.TcpClient]::new()
    try {
        $async = $client.BeginConnect('127.0.0.1', $Port, $null, $null)
        if ($async.AsyncWaitHandle.WaitOne(300, $false) -and $client.Connected) {
            $client.EndConnect($async)
            return $true
        }
        return $false
    } catch {
        return $false
    } finally {
        $client.Close()
    }
}

function Ensure-U14AcceptanceCustomer {
    param([Parameter(Mandatory = $true)][string]$Realm, [Parameter(Mandatory = $true)][string]$ServerUrl)

    $Root = $ServerUrl.TrimEnd('/')
    $AdminTokenResponse = Invoke-RestMethod -Uri "$Root/realms/master/protocol/openid-connect/token" `
        -Method Post -ContentType 'application/x-www-form-urlencoded' -Body @{
            grant_type = 'password'
            client_id = 'admin-cli'
            username = [string]$env:KEYCLOAK_ADMIN
            password = [string]$env:KEYCLOAK_ADMIN_PASSWORD
        } -TimeoutSec 20
    if (-not $AdminTokenResponse.access_token) { throw 'Không lấy được token quản trị Keycloak sandbox.' }
    $Headers = @{ Authorization = "Bearer $($AdminTokenResponse.access_token)" }
    $UsersUri = "$Root/admin/realms/$Realm/users?username=test_customer_b&exact=true"
    $UsersResponse = Invoke-WebRequest -Uri $UsersUri -Headers $Headers -TimeoutSec 20 -UseBasicParsing
    $Users = ConvertFrom-Json -InputObject $UsersResponse.Content -NoEnumerate
    $Created = $false

    if ($Users.Count -eq 0) {
        $UserBody = @{
            username = 'test_customer_b'
            email = 'u14-customer-b@acceptance.local'
            firstName = 'U14 Synthetic'
            lastName = 'Customer B'
            emailVerified = $true
            enabled = $true
            requiredActions = @()
        } | ConvertTo-Json -Depth 4
        $CreateResponse = Invoke-WebRequest -Uri "$Root/admin/realms/$Realm/users" -Method Post `
            -Headers $Headers -ContentType 'application/json' -Body $UserBody -TimeoutSec 20 -UseBasicParsing
        if ([int]$CreateResponse.StatusCode -ne 201) { throw 'Không tạo được CUSTOMER fixture U14 trong Keycloak sandbox.' }
        $UsersResponse = Invoke-WebRequest -Uri $UsersUri -Headers $Headers -TimeoutSec 20 -UseBasicParsing
        $Users = ConvertFrom-Json -InputObject $UsersResponse.Content -NoEnumerate
        $Created = $true
    }
    if ($Users.Count -ne 1 -or -not $Users[0].id) {
        throw 'Không thể phân giải duy nhất tài khoản test_customer_b trong Keycloak sandbox.'
    }

    $Customer = $Users[0]
    $ProfileBody = @{
        email = 'u14-customer-b@acceptance.local'
        firstName = 'U14 Synthetic'
        lastName = 'Customer B'
        emailVerified = $true
        enabled = $true
        requiredActions = @()
    } | ConvertTo-Json -Depth 4
    $null = Invoke-RestMethod -Uri "$Root/admin/realms/$Realm/users/$($Customer.id)" -Method Put `
        -Headers $Headers -ContentType 'application/json' -Body $ProfileBody -TimeoutSec 20

    $Role = Invoke-RestMethod -Uri "$Root/admin/realms/$Realm/roles/CUSTOMER" -Headers $Headers -TimeoutSec 20
    $MappedRoles = @(Invoke-RestMethod -Uri "$Root/admin/realms/$Realm/users/$($Customer.id)/role-mappings/realm" `
        -Headers $Headers -TimeoutSec 20)
    if ($MappedRoles.name -notcontains 'CUSTOMER') {
        $RoleBody = ConvertTo-Json -InputObject @($Role) -Depth 5
        $null = Invoke-RestMethod -Uri "$Root/admin/realms/$Realm/users/$($Customer.id)/role-mappings/realm" `
            -Method Post -Headers $Headers -ContentType 'application/json' -Body $RoleBody -TimeoutSec 20
    }

    try {
        $CustomerTokenResponse = Invoke-RestMethod -Uri "$Root/realms/$Realm/protocol/openid-connect/token" `
            -Method Post -ContentType 'application/x-www-form-urlencoded' -Body @{
                grant_type = 'password'
                client_id = 'hotel-frontend'
                username = 'test_customer_b'
                password = 'Password123!'
            } -TimeoutSec 20
    } catch {
        $PasswordBody = @{
            type = 'password'
            value = 'Password123!'
            temporary = $false
        } | ConvertTo-Json -Depth 4
        $null = Invoke-RestMethod -Uri "$Root/admin/realms/$Realm/users/$($Customer.id)/reset-password" `
            -Method Put -Headers $Headers -ContentType 'application/json' -Body $PasswordBody -TimeoutSec 20
        $CustomerTokenResponse = Invoke-RestMethod -Uri "$Root/realms/$Realm/protocol/openid-connect/token" `
            -Method Post -ContentType 'application/x-www-form-urlencoded' -Body @{
                grant_type = 'password'
                client_id = 'hotel-frontend'
                username = 'test_customer_b'
                password = 'Password123!'
            } -TimeoutSec 20
    }
    if (-not $CustomerTokenResponse.access_token) {
        throw 'CUSTOMER fixture test_customer_b không lấy được token với thông tin acceptance.'
    }

    $Subject = [string]$Customer.id
    if ($Subject -notmatch '^[0-9a-fA-F-]{36}$') { throw 'Subject CUSTOMER fixture U14 không phải UUID hợp lệ.' }
    Write-Host '  CUSTOMER fixture test_customer_b sẵn sàng trong Keycloak sandbox.' -ForegroundColor Green
    return $Subject
}

function Ensure-U14UnlinkedAcceptanceCustomer {
    param([Parameter(Mandatory = $true)][string]$Realm, [Parameter(Mandatory = $true)][string]$ServerUrl)

    $Root = $ServerUrl.TrimEnd('/')
    $AdminTokenResponse = Invoke-RestMethod -Uri "$Root/realms/master/protocol/openid-connect/token" `
        -Method Post -ContentType 'application/x-www-form-urlencoded' -Body @{
            grant_type = 'password'
            client_id = 'admin-cli'
            username = [string]$env:KEYCLOAK_ADMIN
            password = [string]$env:KEYCLOAK_ADMIN_PASSWORD
        } -TimeoutSec 20
    if (-not $AdminTokenResponse.access_token) { throw 'Không lấy được token quản trị Keycloak sandbox.' }
    $Headers = @{ Authorization = "Bearer $($AdminTokenResponse.access_token)" }
    $UsersUri = "$Root/admin/realms/$Realm/users?username=test_customer_unlinked&exact=true"
    $UsersResponse = Invoke-WebRequest -Uri $UsersUri -Headers $Headers -TimeoutSec 20 -UseBasicParsing
    $Users = ConvertFrom-Json -InputObject $UsersResponse.Content -NoEnumerate

    if ($Users.Count -eq 0) {
        $UserBody = @{
            username = 'test_customer_unlinked'
            email = 'u14-unlinked-customer@acceptance.local'
            firstName = 'U14 Synthetic'
            lastName = 'Unlinked Customer'
            emailVerified = $true
            enabled = $true
            requiredActions = @()
        } | ConvertTo-Json -Depth 4
        $CreateResponse = Invoke-WebRequest -Uri "$Root/admin/realms/$Realm/users" -Method Post `
            -Headers $Headers -ContentType 'application/json' -Body $UserBody -TimeoutSec 20 -UseBasicParsing
        if ([int]$CreateResponse.StatusCode -ne 201) { throw 'Không tạo được CUSTOMER fixture U14 chưa liên kết.' }
        $UsersResponse = Invoke-WebRequest -Uri $UsersUri -Headers $Headers -TimeoutSec 20 -UseBasicParsing
        $Users = ConvertFrom-Json -InputObject $UsersResponse.Content -NoEnumerate
    }
    if ($Users.Count -ne 1 -or -not $Users[0].id) {
        throw 'Không thể phân giải duy nhất tài khoản test_customer_unlinked trong Keycloak sandbox.'
    }

    $Customer = $Users[0]
    $ProfileBody = @{
        email = 'u14-unlinked-customer@acceptance.local'
        firstName = 'U14 Synthetic'
        lastName = 'Unlinked Customer'
        emailVerified = $true
        enabled = $true
        requiredActions = @()
    } | ConvertTo-Json -Depth 4
    $null = Invoke-RestMethod -Uri "$Root/admin/realms/$Realm/users/$($Customer.id)" -Method Put `
        -Headers $Headers -ContentType 'application/json' -Body $ProfileBody -TimeoutSec 20

    $Role = Invoke-RestMethod -Uri "$Root/admin/realms/$Realm/roles/CUSTOMER" -Headers $Headers -TimeoutSec 20
    $MappedRoles = @(Invoke-RestMethod -Uri "$Root/admin/realms/$Realm/users/$($Customer.id)/role-mappings/realm" `
        -Headers $Headers -TimeoutSec 20)
    if ($MappedRoles.name -notcontains 'CUSTOMER') {
        $RoleBody = ConvertTo-Json -InputObject @($Role) -Depth 5
        $null = Invoke-RestMethod -Uri "$Root/admin/realms/$Realm/users/$($Customer.id)/role-mappings/realm" `
            -Method Post -Headers $Headers -ContentType 'application/json' -Body $RoleBody -TimeoutSec 20
    }

    $PasswordBody = @{
        type = 'password'
        value = 'Password123!'
        temporary = $false
    } | ConvertTo-Json -Depth 4
    $null = Invoke-RestMethod -Uri "$Root/admin/realms/$Realm/users/$($Customer.id)/reset-password" `
        -Method Put -Headers $Headers -ContentType 'application/json' -Body $PasswordBody -TimeoutSec 20
    $CustomerTokenResponse = Invoke-RestMethod -Uri "$Root/realms/$Realm/protocol/openid-connect/token" `
        -Method Post -ContentType 'application/x-www-form-urlencoded' -Body @{
            grant_type = 'password'
            client_id = 'hotel-frontend'
            username = 'test_customer_unlinked'
            password = 'Password123!'
        } -TimeoutSec 20
    if (-not $CustomerTokenResponse.access_token) {
        throw 'CUSTOMER fixture test_customer_unlinked không lấy được token acceptance.'
    }

    $Subject = [string]$Customer.id
    if ($Subject -notmatch '^[0-9a-fA-F-]{36}$') { throw 'Subject CUSTOMER chưa liên kết không phải UUID hợp lệ.' }
    Write-Host '  CUSTOMER fixture test_customer_unlinked sẵn sàng, không có binding nghiệp vụ.' -ForegroundColor Green
    return $Subject
}

foreach ($applicationPort in @([int]$EnvConfig.BACKEND_PORT, [int]$EnvConfig.FRONTEND_PORT)) {
    if (Test-LocalPortOpen -Port $applicationPort) {
        throw "Cổng ứng dụng $applicationPort đang được dùng. Start.ps1 không dừng tiến trình ngoài sandbox."
    }
}

if (-not $ApplicationsOnly) {
Write-Host "=== [1/6] Khởi động PostgreSQL, Redis và Keycloak của sandbox ===" -ForegroundColor Cyan
docker compose @ComposeArgs up -d
if ($LASTEXITCODE -ne 0) {
    throw "Không khởi động được Docker Compose acceptance (exit code $LASTEXITCODE)."
}

Write-Host "=== [2/6] Chờ các container đạt trạng thái Healthy ===" -ForegroundColor Cyan
$MaxWaitSeconds = 180
$Elapsed = 0

function Check-ServiceHealthy {
    param([string]$ServiceName)
    $statusText = docker compose @ComposeArgs ps --format json $ServiceName
    if ($LASTEXITCODE -ne 0 -or -not $statusText) {
        return $false
    }
    try {
        $status = $statusText | ConvertFrom-Json
        return ($status.Health -eq 'healthy')
    } catch {
        return $false
    }
}

while ($Elapsed -lt $MaxWaitSeconds) {
    $PgHealthy = Check-ServiceHealthy -ServiceName 'acceptance_postgres'
    $RedisHealthy = Check-ServiceHealthy -ServiceName 'acceptance_redis'
    $KcHealthy = Check-ServiceHealthy -ServiceName 'acceptance_keycloak'

    Write-Host "  PG=$PgHealthy, Redis=$RedisHealthy, Keycloak=$KcHealthy ($Elapsed s)..."
    if ($PgHealthy -and $RedisHealthy -and $KcHealthy) {
        Write-Host '  Tất cả dịch vụ hạ tầng đã Healthy.' -ForegroundColor Green
        break
    }

    Start-Sleep -Seconds 5
    $Elapsed += 5
}

if ($Elapsed -ge $MaxWaitSeconds) {
    throw "Quá thời gian chờ container healthy ($MaxWaitSeconds s)."
}

if ($ServicesOnly) {
    Write-Host 'Dịch vụ sandbox đã sẵn sàng; chưa chạy migration hoặc nạp fixture.' -ForegroundColor Green
    return
}

$U14CustomerBSubject = $null
$U14UnlinkedCustomerSubject = $null
if (-not $ApplicationsOnly) {
    $U14CustomerBSubject = Ensure-U14AcceptanceCustomer -Realm ([string]$EnvConfig.KEYCLOAK_REALM) `
        -ServerUrl ([string]$EnvConfig.KEYCLOAK_SERVER_URL)
    $U14UnlinkedCustomerSubject = Ensure-U14UnlinkedAcceptanceCustomer -Realm ([string]$EnvConfig.KEYCLOAK_REALM) `
        -ServerUrl ([string]$EnvConfig.KEYCLOAK_SERVER_URL)
}

Write-Host "=== [3/6] Chạy Liquibase migration (context: master-data) ===" -ForegroundColor Cyan
$NetworkName = 'hotel-m1-acceptance_hotel_acceptance_net'
$DbChangelogPath = Join-Path $ProjectRoot 'backend/src/main/resources/db'

docker run --rm `
    --network $NetworkName `
    -v "${DbChangelogPath}:/liquibase/changelog/db:ro" `
    -e LIQUIBASE_SEARCH_PATH=/liquibase/changelog `
    -e LIQUIBASE_COMMAND_URL="jdbc:postgresql://acceptance_postgres:5432/$($EnvConfig.DB_NAME)" `
    -e LIQUIBASE_COMMAND_USERNAME="$($EnvConfig.DB_USER)" `
    -e LIQUIBASE_COMMAND_PASSWORD="$($EnvConfig.DB_PASSWORD)" `
    -e LIQUIBASE_COMMAND_CHANGELOG_FILE='db/db-changelog-root.xml' `
    -e LIQUIBASE_COMMAND_CONTEXTS='master-data' `
    -e LIQUIBASE_LOG_LEVEL=INFO `
    liquibase/liquibase:4.31-alpine `
    --search-path=/liquibase/changelog --changelog-file=db/db-changelog-root.xml update

if ($LASTEXITCODE -ne 0) {
    throw "Liquibase migration thất bại (exit code $LASTEXITCODE)."
}

Write-Host "=== [4/6] Nạp dữ liệu acceptance giả lập ===" -ForegroundColor Cyan
$FixturesSqlPath = Join-Path $ScriptDir 'fixtures/acceptance-fixtures.sql'
$PgContainerId = (docker compose @ComposeArgs ps -q acceptance_postgres).Trim()
if ($LASTEXITCODE -ne 0 -or -not $PgContainerId) {
    throw 'Không tìm thấy container PostgreSQL acceptance.'
}

docker cp $FixturesSqlPath "${PgContainerId}:/tmp/acceptance-fixtures.sql"
if ($LASTEXITCODE -ne 0) {
    throw 'Không chép được fixture SQL vào container PostgreSQL.'
}

docker exec $PgContainerId psql -v ON_ERROR_STOP=1 -v "u14_customer_b_subject=$U14CustomerBSubject" `
    -U $($EnvConfig.DB_USER) -d $($EnvConfig.DB_NAME) -f /tmp/acceptance-fixtures.sql
if ($LASTEXITCODE -ne 0) {
    throw "Nạp fixture SQL thất bại (exit code $LASTEXITCODE)."
}

$U14BindingVerificationSql = "SELECT COUNT(*) FROM personalization_account_bindings binding JOIN booking_guests guest ON guest.id = binding.guest_id WHERE binding.issuer = 'http://localhost:18081/realms/hotel-acceptance' AND binding.status = 'ACTIVE' AND binding.source = 'SYNTHETIC_FIXTURE' AND ((binding.subject = '20000000-0000-0000-0000-000000000006' AND guest.public_id = '30000000-0000-0000-0000-000000000001') OR (binding.subject = '$U14CustomerBSubject' AND guest.public_id = '30000000-0000-0000-0000-000000000002'));"
$U14BindingCount = docker exec $PgContainerId psql -v ON_ERROR_STOP=1 -U $($EnvConfig.DB_USER) `
    -d $($EnvConfig.DB_NAME) -Atc "$U14BindingVerificationSql"
if ($LASTEXITCODE -ne 0 -or [string]$U14BindingCount -ne '2') {
    throw 'Hai CUSTOMER fixture U14 không được bind đúng Keycloak subject.'
}
$U14UnlinkedBindingCount = docker exec $PgContainerId psql -v ON_ERROR_STOP=1 -U $($EnvConfig.DB_USER) `
    -d $($EnvConfig.DB_NAME) -Atc "SELECT COUNT(*) FROM personalization_account_bindings WHERE issuer = 'http://localhost:18081/realms/hotel-acceptance' AND subject = '$U14UnlinkedCustomerSubject' AND status = 'ACTIVE';"
if ($LASTEXITCODE -ne 0 -or [string]$U14UnlinkedBindingCount -ne '0') {
    throw 'CUSTOMER fixture chưa liên kết không được có active binding.'
}

$VerificationSql = "SELECT 'hotels' AS tbl, count(*) FROM hotels UNION ALL SELECT 'hotel_room_types', count(*) FROM hotel_room_types UNION ALL SELECT 'room_availability', count(*) FROM room_availability UNION ALL SELECT 'staffs', count(*) FROM staffs;"
docker exec $PgContainerId psql -v ON_ERROR_STOP=1 -U $($EnvConfig.DB_USER) -d $($EnvConfig.DB_NAME) -c "$VerificationSql"
if ($LASTEXITCODE -ne 0) {
    throw 'Kiểm tra dữ liệu fixture thất bại.'
}
}

if ($InfrastructureOnly) {
    Write-Host 'Hạ tầng và fixture đã sẵn sàng; ứng dụng chưa chạy.' -ForegroundColor Green
    return
}

Write-Host "=== [5/6] Khởi chạy backend và frontend trên host ===" -ForegroundColor Cyan
$LauncherPath = (Resolve-Path (Join-Path $ScriptDir 'Run-Application.ps1')).Path
$PowerShellPath = (Get-Process -Id $PID).Path
$RunId = (Get-Date).ToString('yyyyMMdd-HHmmss')
$StartedApps = [System.Collections.Generic.List[object]]::new()

function Start-AcceptanceApplication {
    param([ValidateSet('backend', 'frontend')][string]$Component)
    $arguments = "-NoLogo -NoProfile -ExecutionPolicy Bypass -File `"$LauncherPath`" -Component $Component -RuntimeFile `"$RuntimeFile`" -ProjectRoot `"$ProjectRoot`""
    $stdout = Join-Path $RuntimeDir "$Component-$RunId.stdout.log"
    $stderr = Join-Path $RuntimeDir "$Component-$RunId.stderr.log"
    $process = Start-Process -FilePath $PowerShellPath -ArgumentList $arguments -WorkingDirectory $ProjectRoot `
        -WindowStyle Hidden -PassThru -RedirectStandardOutput $stdout -RedirectStandardError $stderr
    $StartedApps.Add([PSCustomObject]@{
        Component = $Component
        ProcessId = $process.Id
        StartedAt = $process.StartTime.ToUniversalTime().ToString('o')
        LauncherPath = $LauncherPath
    })
    $state = [PSCustomObject]@{ processes = @($StartedApps.ToArray()) }
    $state | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath $ProcessStateFile -Encoding UTF8
    return $process
}

function Wait-ForEndpoint {
    param([string]$Uri, [int]$ProcessId, [string]$Component)
    $maxSeconds = 240
    for ($second = 0; $second -lt $maxSeconds; $second += 3) {
        if (-not (Get-Process -Id $ProcessId -ErrorAction SilentlyContinue)) {
            $stdout = Get-ChildItem -LiteralPath $RuntimeDir -Filter "$Component-*.stdout.log" | Sort-Object LastWriteTime -Descending | Select-Object -First 1
            $stderr = Get-ChildItem -LiteralPath $RuntimeDir -Filter "$Component-*.stderr.log" | Sort-Object LastWriteTime -Descending | Select-Object -First 1
            throw "$Component đã thoát khi khởi động. Xem log runtime: $($stdout.FullName), $($stderr.FullName)."
        }
        try {
            $response = Invoke-WebRequest -Uri $Uri -Method Get -TimeoutSec 4 -UseBasicParsing
            if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 400) {
                return
            }
        } catch {
            # Endpoint chưa sẵn sàng; tiếp tục chờ trong giới hạn đã đặt.
        }
        Start-Sleep -Seconds 3
    }
    throw "$Component không sẵn sàng sau $maxSeconds giây: $Uri"
}

try {
    $BackendProcess = Start-AcceptanceApplication -Component 'backend'
    $BackendUrl = "http://localhost:$($EnvConfig.BACKEND_PORT)"
    Wait-ForEndpoint -Uri "$BackendUrl/api/v1/public/booking-search/destinations" -ProcessId $BackendProcess.Id -Component 'backend'
    Write-Host "  Backend sẵn sàng tại $BackendUrl" -ForegroundColor Green

    $FrontendProcess = Start-AcceptanceApplication -Component 'frontend'
    $FrontendUrl = "http://localhost:$($EnvConfig.FRONTEND_PORT)"
    Wait-ForEndpoint -Uri "$FrontendUrl/" -ProcessId $FrontendProcess.Id -Component 'frontend'
    Write-Host "  Frontend sẵn sàng tại $FrontendUrl" -ForegroundColor Green
} catch {
    Stop-AcceptanceTrackedProcesses -StateFile $ProcessStateFile | Out-Null
    throw
}

Write-Host '=== [6/6] Acceptance sandbox đã sẵn sàng ===' -ForegroundColor Green
Write-Host "Backend:  $BackendUrl"
Write-Host "Frontend: $FrontendUrl"
Write-Host "Dừng ứng dụng và container bằng: $ScriptDir/Stop.ps1"
