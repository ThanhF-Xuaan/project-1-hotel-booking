[CmdletBinding()]
param(
 [ValidateSet('seed','inspect','rollback')][string]$Action='inspect',
 [string]$Container='hotel_postgres',
 [ValidateSet('hotel_booking_db','hotel_acceptance_db','m1_analytics_test')][string]$Database='hotel_booking_db',
 [ValidateSet('all','occupancy-v1','group-v1')][string]$Dataset='all',
 [Parameter(Mandatory=$true)][string]$EvidenceDirectory,
 [switch]$NewReader
)
$ErrorActionPreference='Stop'
$extra = if ($NewReader) { @('--new-reader') } else { @() }
& python (Join-Path $PSScriptRoot 'analytics_demo_db.py') --action $Action --container $Container --database $Database --dataset $Dataset --evidence-directory $EvidenceDirectory @extra
if ($LASTEXITCODE -ne 0) { throw 'Analytics demo provisioning failed; inspect bounded evidence.' }
