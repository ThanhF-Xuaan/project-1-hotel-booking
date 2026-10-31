function Initialize-AcceptanceRuntime {
    param([Parameter(Mandatory = $true)][string]$ScriptDirectory)

    $runtimeDirectory = Join-Path $ScriptDirectory '.runtime'
    $runtimeFile = Join-Path $runtimeDirectory 'env.json'
    if (-not (Test-Path -LiteralPath $runtimeFile)) {
        throw "Runtime configuration is missing: $runtimeFile. Run Prepare.ps1 first."
    }

    $environment = Get-Content -LiteralPath $runtimeFile -Raw | ConvertFrom-Json
    $composeValues = [ordered]@{
        DB_NAME = $environment.DB_NAME
        DB_USER = $environment.DB_USER
        DB_PASSWORD = $environment.DB_PASSWORD
        DB_PORT = $environment.DB_PORT
        REDIS_PORT = $environment.REDIS_PORT
        KEYCLOAK_ADMIN = if ($environment.KEYCLOAK_ADMIN) { $environment.KEYCLOAK_ADMIN } else { 'admin' }
        KEYCLOAK_ADMIN_PASSWORD = if ($environment.KEYCLOAK_ADMIN_PASSWORD) { $environment.KEYCLOAK_ADMIN_PASSWORD } else { 'admin' }
        KEYCLOAK_PORT = $environment.KEYCLOAK_PORT
    }

    $composeLines = [System.Collections.Generic.List[string]]::new()
    foreach ($name in $composeValues.Keys) {
        $value = [string]$composeValues[$name]
        if ([string]::IsNullOrWhiteSpace($value) -or $value -notmatch '^[A-Za-z0-9._!:+/-]+$') {
            throw "Invalid Compose runtime value for $name. Use letters, digits, and . _ ! : + / - only."
        }

        # Set process values as well as --env-file values so shell variables cannot override the sandbox.
        Set-Item -Path "Env:$name" -Value $value
        $composeLines.Add("$name=$value")
    }

    if (-not (Test-Path -LiteralPath $runtimeDirectory)) {
        New-Item -ItemType Directory -Path $runtimeDirectory -Force | Out-Null
    }
    $composeEnvFile = Join-Path $runtimeDirectory 'compose.env'
    [System.IO.File]::WriteAllLines(
        $composeEnvFile,
        $composeLines,
        (New-Object System.Text.UTF8Encoding($false))
    )

    $projectRoot = (Resolve-Path (Join-Path $ScriptDirectory '../..')).Path
    $composeArguments = @(
        '--env-file', $composeEnvFile,
        '-p', 'hotel-m1-acceptance',
        '-f', (Join-Path $projectRoot 'docker-compose.acceptance.yml')
    )
    # Optional per-run volume names keep a fresh disposable sandbox separate from retained evidence.
    if ($environment.ACCEPTANCE_VOLUME_PREFIX) {
        $prefix = [string]$environment.ACCEPTANCE_VOLUME_PREFIX
        if ($prefix -notmatch '^[a-z][a-z0-9-]{0,62}$') { throw 'Invalid acceptance volume prefix.' }
        $overrideFile = Join-Path $runtimeDirectory 'volumes.override.yml'
        $volumeYaml = "volumes:`n  acceptance_pg_data:`n    name: ${prefix}-pg`n  acceptance_redis_data:`n    name: ${prefix}-redis`n  acceptance_kc_data:`n    name: ${prefix}-kc`n"
        [IO.File]::WriteAllText($overrideFile, $volumeYaml, (New-Object System.Text.UTF8Encoding($false)))
        $composeArguments += @('-f', $overrideFile)
    }

    return [PSCustomObject]@{
        RuntimeDirectory = $runtimeDirectory
        RuntimeFile = $runtimeFile
        Environment = $environment
        ComposeEnvFile = $composeEnvFile
        ComposeArguments = $composeArguments
        ProjectRoot = $projectRoot
    }
}

function Get-AcceptanceTrackedProcess {
    param([Parameter(Mandatory = $true)]$Entry)

    $trackedId = [int]$Entry.ProcessId
    $processInfo = Get-CimInstance Win32_Process -Filter "ProcessId = $trackedId" -ErrorAction SilentlyContinue
    if (-not $processInfo -or [string]::IsNullOrWhiteSpace($processInfo.CommandLine)) {
        return $null
    }

    $launcherPath = [string]$Entry.LauncherPath
    $componentMarker = "-Component $($Entry.Component)"
    if ($processInfo.CommandLine.IndexOf($launcherPath, [StringComparison]::OrdinalIgnoreCase) -lt 0 -or
        $processInfo.CommandLine.IndexOf($componentMarker, [StringComparison]::OrdinalIgnoreCase) -lt 0) {
        return $null
    }

    $trackedProcess = Get-Process -Id $trackedId -ErrorAction SilentlyContinue
    if (-not $trackedProcess) {
        return $null
    }

    if ($Entry.StartedAt -is [DateTime]) {
        $expectedStart = $Entry.StartedAt.ToUniversalTime()
    } elseif ($Entry.StartedAt -is [DateTimeOffset]) {
        $expectedStart = $Entry.StartedAt.UtcDateTime
    } else {
        $expectedStart = [DateTimeOffset]::Parse([string]$Entry.StartedAt).UtcDateTime
    }
    $actualStart = $trackedProcess.StartTime.ToUniversalTime()
    if ([Math]::Abs(($actualStart - $expectedStart).TotalSeconds) -gt 5) {
        return $null
    }

    return $processInfo
}

function Stop-AcceptanceTrackedProcesses {
    param([Parameter(Mandatory = $true)][string]$StateFile)

    if (-not (Test-Path -LiteralPath $StateFile)) {
        return $true
    }

    $state = Get-Content -LiteralPath $StateFile -Raw | ConvertFrom-Json
    $allStopped = $true
    foreach ($entry in @($state.processes)) {
        $processInfo = Get-AcceptanceTrackedProcess -Entry $entry
        if (-not $processInfo) {
            $existingProcess = Get-Process -Id ([int]$entry.ProcessId) -ErrorAction SilentlyContinue
            if ($existingProcess) {
                Write-Warning "Bỏ qua PID $($entry.ProcessId): tiến trình không khớp manifest acceptance, không dừng tiến trình này."
                $allStopped = $false
            }
            continue
        }

        & taskkill.exe /PID ([int]$entry.ProcessId) /T /F | Out-Null
        if ($LASTEXITCODE -ne 0) {
            Write-Warning "Không dừng được tiến trình acceptance $($entry.Component), PID $($entry.ProcessId)."
            $allStopped = $false
        } else {
            Write-Host "Đã dừng $($entry.Component) (PID $($entry.ProcessId))."
        }
    }

    if ($allStopped) {
        Remove-Item -LiteralPath $StateFile -Force -ErrorAction SilentlyContinue
    }
    return $allStopped
}
