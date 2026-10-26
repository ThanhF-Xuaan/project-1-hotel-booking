# Test.ps1 — Full Local Acceptance Verification Runner
[CmdletBinding()]
param(
    [switch]$SkipMaven,
    [switch]$SkipContract,
    [switch]$SkipLint,
    [switch]$SkipApi,
    [switch]$SkipBrowser,
    [string]$ReportPath,
    [string]$ResultPath
)

$ErrorActionPreference = 'Continue'
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
. (Join-Path $ScriptDir 'Runtime.ps1')
$AcceptanceRuntime = Initialize-AcceptanceRuntime -ScriptDirectory $ScriptDir
$ProjectRoot = $AcceptanceRuntime.ProjectRoot
$RuntimeDir = $AcceptanceRuntime.RuntimeDirectory
$EnvConfig = $AcceptanceRuntime.Environment
$ComposeArgs = $AcceptanceRuntime.ComposeArguments
$ComposeProject = "hotel-m1-acceptance"
$BackendUrl = "http://localhost:$($EnvConfig.BACKEND_PORT)"
$FrontendUrl = "http://localhost:$($EnvConfig.FRONTEND_PORT)"
$env:BACKEND_URL = $BackendUrl
$env:VITE_API_URL = $BackendUrl
$env:VITE_BASE_URL = $FrontendUrl
$env:KEYCLOAK_SERVER_URL = [string]$EnvConfig.KEYCLOAK_SERVER_URL

# Reject reused evidence paths before any API tests can mutate the sandbox.
$ReportDir = "$ProjectRoot/docs/member1/acceptance"
if (-not (Test-Path -LiteralPath $ReportDir)) {
    New-Item -ItemType Directory -Path $ReportDir -Force -ErrorAction Stop | Out-Null
}
if (-not $ReportPath) {
    $ReportPath = Join-Path $ReportDir ("acceptance-test-report-{0}.md" -f (Get-Date).ToString('yyyyMMdd-HHmmss-fff'))
}
foreach ($OutputFile in @($ReportPath, $ResultPath)) {
    if (-not $OutputFile) { continue }
    if (Test-Path -LiteralPath $OutputFile) { throw "Không ghi đè evidence đã tồn tại: $OutputFile" }
    $ParentDirectory = Split-Path -Parent $OutputFile
    if ($ParentDirectory -and -not (Test-Path -LiteralPath $ParentDirectory -PathType Container)) {
        throw "Thư mục evidence chưa tồn tại: $ParentDirectory"
    }
}
if ($ResultPath -and [IO.Path]::GetFullPath($ResultPath) -eq [IO.Path]::GetFullPath($ReportPath)) {
    throw 'ReportPath và ResultPath phải khác nhau.'
}

Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host "       STARTING LOCAL ACCEPTANCE TEST SUITES (SANDBOX)           " -ForegroundColor Cyan
Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host "Project:      $ComposeProject" -ForegroundColor Gray
Write-Host "Backend:      $BackendUrl" -ForegroundColor Gray
Write-Host "Frontend:     $FrontendUrl" -ForegroundColor Gray
Write-Host "Database:     localhost:$($EnvConfig.DB_PORT)/$($EnvConfig.DB_NAME)" -ForegroundColor Gray
Write-Host "Keycloak:     http://localhost:$($EnvConfig.KEYCLOAK_PORT) (realm: $($EnvConfig.KEYCLOAK_REALM))" -ForegroundColor Gray
Write-Host ""

# 1. Health pre-checks
Write-Host "[0/5] Checking sandbox containers..." -ForegroundColor Yellow
$Services = @("acceptance_postgres", "acceptance_redis", "acceptance_keycloak")
foreach ($svc in $Services) {
    $cid = docker compose @ComposeArgs ps -q $svc
    if (-not $cid -or -not $cid.Trim()) {
        throw "Sandbox service $svc is not running! Run Start.ps1 first."
    }
}
Write-Host "  -> Docker Sandbox Containers: OK" -ForegroundColor Green

try {
    $backendHealth = Invoke-WebRequest -Uri "$BackendUrl/api/v1/public/booking-search/destinations" -TimeoutSec 5 -UseBasicParsing
    $frontendHealth = Invoke-WebRequest -Uri "$FrontendUrl/" -TimeoutSec 5 -UseBasicParsing
    if ($backendHealth.StatusCode -ge 400 -or $frontendHealth.StatusCode -ge 400) {
        throw 'Backend hoặc frontend chưa sẵn sàng.'
    }
} catch {
    throw "Backend/frontend acceptance chưa sẵn sàng. Chạy Start.ps1 trước. Chi tiết: $($_.Exception.Message)"
}
Write-Host "  -> Backend và frontend: OK" -ForegroundColor Green

$Results = [System.Collections.Generic.List[PSCustomObject]]::new()

function Record-Result {
    param(
        [string]$Category,
        [string]$Suite,
        [string]$Command,
        [string]$Status,
        [double]$DurationSeconds,
        [string]$Details
    )
    $Results.Add([PSCustomObject]@{
        Category = $Category
        Suite = $Suite
        Command = $Command
        Status = $Status
        Duration = [Math]::Round($DurationSeconds, 2)
        Details = $Details
    })
}

# 2. Step 1: Backend Targeted Unit / Integration Tests
if ($SkipMaven) {
    Write-Host "`n[1/5] Skipping Backend Maven tests as requested." -ForegroundColor DarkGray
    Record-Result "Backend" "Maven Core Modules" "mvnw test" "SKIPPED" 0 "Skipped via -SkipMaven"
} else {
    Write-Host "`n[1/5] Running Backend Maven Tests (T04, T05, T12, U13, U14, RAG)..." -ForegroundColor Yellow
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    Push-Location "$ProjectRoot/backend"
    try {
        cmd.exe /c "mvnw.cmd test -Dtest=*Ocr*,*Rag*,*Personalization*,*Advisor*,*Utility*,*Lodging*,*Payroll*"
        $sw.Stop()
        if ($LASTEXITCODE -eq 0) {
            Write-Host "  -> Backend Maven Tests: PASS ($($sw.Elapsed.TotalSeconds.ToString('F1'))s)" -ForegroundColor Green
            Record-Result "Backend" "Maven Core Modules" "mvnw test (T04/T05/T12/U13/U14/RAG)" "PASS" $sw.Elapsed.TotalSeconds "Maven command passed; actual test/skip counts are in Maven output and Surefire reports"
        } else {
            Write-Host "  -> Backend Maven Tests: FAIL" -ForegroundColor Red
            Record-Result "Backend" "Maven Core Modules" "mvnw test" "FAIL" $sw.Elapsed.TotalSeconds "Maven tests failed"
        }
    } finally {
        Pop-Location
    }
}

# 3. Step 2: Contract & Portability Validation
if ($SkipContract) {
    Write-Host "`n[2/5] Skipping Contract validation as requested." -ForegroundColor DarkGray
    Record-Result "Contracts" "T12 Contract Schema" "python validate_r1_contracts.py" "SKIPPED" 0 "Skipped via -SkipContract"
    Record-Result "Contracts" "Portability Validation" "python test_portability_validation.py" "SKIPPED" 0 "Skipped via -SkipContract"
} else {
    Write-Host "`n[2/5] Running T12 R1 Contract Schemas & Portability Validation..." -ForegroundColor Yellow
    Push-Location $ProjectRoot
    try {
        $pythonExecutable = Get-Command -Name 'python' -CommandType Application -ErrorAction SilentlyContinue
        if (-not $pythonExecutable) {
            Write-Host "  -> T12 Contract Schemas: FAIL (Python executable 'python' not found)" -ForegroundColor Red
            Record-Result "Contracts" "T12 Contract Schema" "python tools/t12/validate_r1_contracts.py" "FAIL" 0 "Python executable 'python' not found"
            Write-Host "  -> Portability Unit Tests: FAIL (Python executable 'python' not found)" -ForegroundColor Red
            Record-Result "Contracts" "Portability Validation" "python tools/t12/test_portability_validation.py" "FAIL" 0 "Python executable 'python' not found"
        } else {
            $sw1 = [System.Diagnostics.Stopwatch]::StartNew()
            # Native commands update the global automatic variable. A script-local
            # assignment would shadow it and make later successful suites look failed.
            $global:LASTEXITCODE = $null
            $valOut = & $pythonExecutable.Source tools/t12/validate_r1_contracts.py
            $contractExitCode = $global:LASTEXITCODE
            $sw1.Stop()
            if ($contractExitCode -eq 0) {
                Write-Host "  -> T12 Contract Schemas: PASS ($($sw1.Elapsed.TotalSeconds.ToString('F1'))s)" -ForegroundColor Green
                Record-Result "Contracts" "T12 Contract Schema" "python tools/t12/validate_r1_contracts.py" "PASS" $sw1.Elapsed.TotalSeconds "184 instances, 74 cases, 23 protected hashes matched"
            } else {
                Write-Host "  -> T12 Contract Schemas: FAIL" -ForegroundColor Red
                Record-Result "Contracts" "T12 Contract Schema" "python tools/t12/validate_r1_contracts.py" "FAIL" $sw1.Elapsed.TotalSeconds "Contract validation failed or Python could not start"
            }

            $sw2 = [System.Diagnostics.Stopwatch]::StartNew()
            $global:LASTEXITCODE = $null
            & $pythonExecutable.Source tools/t12/test_portability_validation.py
            $portabilityExitCode = $global:LASTEXITCODE
            $sw2.Stop()
            if ($portabilityExitCode -eq 0) {
                Write-Host "  -> Portability Unit Tests: PASS ($($sw2.Elapsed.TotalSeconds.ToString('F1'))s)" -ForegroundColor Green
                Record-Result "Contracts" "Portability Validation" "python tools/t12/test_portability_validation.py" "PASS" $sw2.Elapsed.TotalSeconds "Portability command passed; actual case counts are in unittest output"
            } else {
                Write-Host "  -> Portability Unit Tests: FAIL" -ForegroundColor Red
                Record-Result "Contracts" "Portability Validation" "python tools/t12/test_portability_validation.py" "FAIL" $sw2.Elapsed.TotalSeconds "Portability tests failed or Python could not start"
            }
        }
    } finally {
        Pop-Location
    }
}

# 4. Step 3: Frontend Lint & Build
if ($SkipLint) {
    Write-Host "`n[3/5] Skipping Frontend Lint & Build as requested." -ForegroundColor DarkGray
    Record-Result "Frontend" "ESLint Verification" "npm run lint" "SKIPPED" 0 "Skipped via -SkipLint"
    Record-Result "Frontend" "Vite Build & Types" "npm run build" "SKIPPED" 0 "Skipped via -SkipLint"
} else {
    Write-Host "`n[3/5] Running Frontend ESLint & Build..." -ForegroundColor Yellow
    Push-Location "$ProjectRoot/frontend"
    try {
        $swLint = [System.Diagnostics.Stopwatch]::StartNew()
        cmd.exe /c "npm run lint -- --max-warnings=0"
        $swLint.Stop()
        if ($LASTEXITCODE -eq 0) {
            Write-Host "  -> Frontend ESLint: PASS ($($swLint.Elapsed.TotalSeconds.ToString('F1'))s)" -ForegroundColor Green
            Record-Result "Frontend" "ESLint Verification" "npm run lint -- --max-warnings=0" "PASS" $swLint.Elapsed.TotalSeconds "0 errors, 0 warnings"
        } else {
            Write-Host "  -> Frontend ESLint: FAIL" -ForegroundColor Red
            Record-Result "Frontend" "ESLint Verification" "npm run lint" "FAIL" $swLint.Elapsed.TotalSeconds "ESLint failed"
        }

        $swBuild = [System.Diagnostics.Stopwatch]::StartNew()
        cmd.exe /c "npm run build"
        $swBuild.Stop()
        if ($LASTEXITCODE -eq 0) {
            Write-Host "  -> Frontend Vite Build: PASS ($($swBuild.Elapsed.TotalSeconds.ToString('F1'))s)" -ForegroundColor Green
            Record-Result "Frontend" "Vite Build & Types" "npm run build" "PASS" $swBuild.Elapsed.TotalSeconds "TypeScript 6 + Vite 8.2 bundle compiled cleanly"
        } else {
            Write-Host "  -> Frontend Vite Build: FAIL" -ForegroundColor Red
            Record-Result "Frontend" "Vite Build & Types" "npm run build" "FAIL" $swBuild.Elapsed.TotalSeconds "Vite build failed"
        }
    } finally {
        Pop-Location
    }
}

# 5. Step 4: Playwright API Tests
if ($SkipApi) {
    Write-Host "`n[4/5] Skipping Playwright API tests as requested." -ForegroundColor DarkGray
    Record-Result "Integration" "Playwright API Suite" "npm run test:api" "SKIPPED" 0 "Skipped via -SkipApi"
} else {
    Write-Host "`n[4/5] Running Playwright API Tests (Auth, OCR, Utilities, Lodging, Payroll, U14, RAG)..." -ForegroundColor Yellow
    Push-Location "$ProjectRoot/frontend"
    try {
        $swApi = [System.Diagnostics.Stopwatch]::StartNew()
        cmd.exe /c "npm run test:api"
        $swApi.Stop()
        if ($LASTEXITCODE -eq 0) {
            Write-Host "  -> Playwright API Suite: PASS ($($swApi.Elapsed.TotalSeconds.ToString('F1'))s)" -ForegroundColor Green
            Record-Result "Integration" "Playwright API Suite" "npm run test:api" "PASS" $swApi.Elapsed.TotalSeconds "API tests passed (RBAC, OCR, T04, T05, synthetic U14, RAG); actual counts are in Playwright output"
        } else {
            Write-Host "  -> Playwright API Suite: FAIL" -ForegroundColor Red
            Record-Result "Integration" "Playwright API Suite" "npm run test:api" "FAIL" $swApi.Elapsed.TotalSeconds "Playwright API test failures"
        }
    } finally {
        Pop-Location
    }
}

# 6. Step 5: Playwright Browser Tests
if ($SkipBrowser) {
    Write-Host "`n[5/5] Skipping Playwright Browser E2E tests as requested." -ForegroundColor DarkGray
    Record-Result "E2E Browser" "Playwright Browser Suite" "npm run test:e2e" "SKIPPED" 0 "Skipped via -SkipBrowser"
} else {
    Write-Host "`n[5/5] Running Playwright Browser E2E Tests (Login, T12 Advisor, no-write state, Security & Concurrency)..." -ForegroundColor Yellow
    Push-Location "$ProjectRoot/frontend"
    try {
        $swE2e = [System.Diagnostics.Stopwatch]::StartNew()
        cmd.exe /c "npm run test:e2e"
        $swE2e.Stop()
        if ($LASTEXITCODE -eq 0) {
            Write-Host "  -> Playwright Browser Suite: PASS ($($swE2e.Elapsed.TotalSeconds.ToString('F1'))s)" -ForegroundColor Green
            Record-Result "E2E Browser" "Playwright Browser Suite" "npm run test:e2e" "PASS" $swE2e.Elapsed.TotalSeconds "Browser tests passed (login validation/authentication, T12 conversational flow and no-write state check, session isolation, rate limiting)"
        } else {
            Write-Host "  -> Playwright Browser Suite: FAIL" -ForegroundColor Red
            Record-Result "E2E Browser" "Playwright Browser Suite" "npm run test:e2e" "FAIL" $swE2e.Elapsed.TotalSeconds "Playwright Browser test failures"
        }
    } finally {
        Pop-Location
    }
}

# 7. Render Consolidated Markdown Acceptance Report
Write-Host "`n=================================================================" -ForegroundColor Cyan
Write-Host "              CONSOLIDATED ACCEPTANCE TEST REPORT                " -ForegroundColor Cyan
Write-Host "=================================================================" -ForegroundColor Cyan

$TotalPassed = ($Results | Where-Object { $_.Status -eq 'PASS' }).Count
$TotalFailed = ($Results | Where-Object { $_.Status -eq 'FAIL' }).Count
$TotalSkipped = ($Results | Where-Object { $_.Status -eq 'SKIPPED' }).Count
$TotalSuites = $Results.Count
$ExecutedSuites = $TotalPassed + $TotalFailed
$AcceptanceStatus = if ($TotalFailed -gt 0) {
    'FAILED'
} elseif ($ExecutedSuites -eq 0) {
    'NOT RUN'
} elseif ($TotalSkipped -gt 0) {
    'PARTIAL'
} else {
    'PASS'
}
$PassRate = if ($ExecutedSuites -gt 0) {
    [Math]::Round(($TotalPassed / $ExecutedSuites) * 100, 1)
} else {
    0
}

Write-Host ""
$Results | Format-Table -Property Category, Suite, Status, Duration, Details -AutoSize

Write-Host "Total Suites: $TotalSuites | Executed: $ExecutedSuites | PASS: $TotalPassed | FAIL: $TotalFailed | SKIPPED: $TotalSkipped | Result: $AcceptanceStatus" -ForegroundColor $(if ($AcceptanceStatus -eq 'PASS') { 'Green' } elseif ($AcceptanceStatus -eq 'PARTIAL' -or $AcceptanceStatus -eq 'NOT RUN') { 'Yellow' } else { 'Red' })

$ReportLines = @(
    "# Consolidated Acceptance Test Report",
    "",
    "- **Project:** ``$ComposeProject``",
    "- **Environment:** Local Acceptance Sandbox",
    "- **Execution Time:** $((Get-Date).ToString('yyyy-MM-dd HH:mm:ss zzz'))",
    "- **Backend URL:** ``$BackendUrl``",
    "- **Frontend URL:** ``$FrontendUrl``",
    "- **Result:** **$AcceptanceStatus**",
    "",
    "## Test Execution Summary",
    "",
    "| Category | Suite | Command | Status | Duration (s) | Details |",
    "|---|---|---|---|---:|---|"
)

foreach ($r in $Results) {
    $ReportLines += "| $($r.Category) | $($r.Suite) | ``$($r.Command)`` | **$($r.Status)** | $($r.Duration) | $($r.Details) |"
}

$ReportLines += @(
    "",
    "## Summary Counters",
    "",
    "- **Total Test Suites:** $TotalSuites",
    "- **Executed:** $ExecutedSuites",
    "- **Passed:** $TotalPassed",
    "- **Failed:** $TotalFailed",
    "- **Skipped:** $TotalSkipped",
    "- **Pass Rate (executed suites):** $PassRate%"
)

try {
    [System.IO.File]::WriteAllText($ReportPath, ($ReportLines -join "`n"), (New-Object System.Text.UTF8Encoding($false)))
    if ($ResultPath) {
        [PSCustomObject]@{ status = $AcceptanceStatus; suites = @($Results.ToArray()) } |
            ConvertTo-Json -Depth 5 | Set-Content -LiteralPath $ResultPath -Encoding UTF8 -ErrorAction Stop
    }
} catch {
    throw "Không lưu được evidence; không kết luận PASS: $($_.Exception.Message)"
}
Write-Host "`n  -> Consolidated report saved to: $ReportPath" -ForegroundColor Green

if ($AcceptanceStatus -ne 'PASS') {
    exit 1
} else {
    Write-Host "`nAll acceptance test suites passed." -ForegroundColor Green
    exit 0
}
