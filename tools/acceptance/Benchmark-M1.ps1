# Synthetic HTTP benchmark on the owned acceptance sandbox.
[CmdletBinding()]
param([Parameter(Mandatory = $true)][string]$OutputDirectory, [switch]$Smoke)
$ErrorActionPreference = 'Stop'
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
. (Join-Path $ScriptDir 'Runtime.ps1')
$Runtime = Initialize-AcceptanceRuntime -ScriptDirectory $ScriptDir
if ([string]$Runtime.Environment.HOTEL_AI_OCR_ENABLED -ne 'true') { throw 'Benchmark requires real local OCR enabled.' }
if (-not (Test-Path -LiteralPath (Join-Path $Runtime.RuntimeDirectory 'processes.json'))) { throw 'Owned application manifest is missing.' }
if (Test-Path -LiteralPath $OutputDirectory) { throw 'Do not overwrite benchmark evidence.' }
New-Item -ItemType Directory -Path $OutputDirectory | Out-Null
$Arguments = @((Join-Path $ScriptDir 'benchmark-m1.mjs'), '--output', [IO.Path]::GetFullPath($OutputDirectory))
if ($Smoke) { $Arguments += '--smoke' }
& node @Arguments
if ($LASTEXITCODE -ne 0) { throw "Benchmark failed (exit $LASTEXITCODE); retain all samples." }
