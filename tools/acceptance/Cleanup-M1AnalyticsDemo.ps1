[CmdletBinding()]
param([Parameter(Mandatory=$true)][string]$SeedDirectory,[Parameter(Mandatory=$true)][string]$EvidenceDirectory)
$ErrorActionPreference='Stop'
$pg = @(docker ps --filter 'label=com.docker.compose.project=hotel-m1-acceptance' --filter 'label=com.docker.compose.service=acceptance_postgres' --format '{{.ID}}')
if ($LASTEXITCODE -ne 0 -or $pg.Count -ne 1) { throw 'Owned acceptance DB required.' }
$receipt=Get-Content -LiteralPath (Join-Path $SeedDirectory 'receipt.json') -Raw | ConvertFrom-Json
$role=[string]$receipt.runtimeRole
if ($receipt.database -ne 'hotel_acceptance_db' -or $role -cnotmatch '^m1_demo_reader_[a-f0-9]{10}$') { throw 'Invalid owned seed receipt.' }
& (Join-Path $PSScriptRoot 'Seed-M1AnalyticsDemo.ps1') -Action rollback -Container $pg[0] -Database hotel_acceptance_db -EvidenceDirectory $EvidenceDirectory
if (-not $?) { throw 'Dataset rollback failed; preserve schema.' }
$cleanup=@'
BEGIN;
SET LOCAL lock_timeout='5s';
DO $$ BEGIN
 IF EXISTS(SELECT 1 FROM m1_demo.datasets) OR
 EXISTS(SELECT 1 FROM pg_tables WHERE schemaname='m1_demo' AND tablename NOT IN ('datasets','occupancy_samples','group_scenarios','group_candidates','group_quotas','group_night_eligibility','databasechangelog','databasechangeloglock')) OR
 EXISTS(SELECT 1 FROM pg_class WHERE relnamespace='m1_demo'::regnamespace AND relkind IN ('v','m','S','f')) OR
 EXISTS(SELECT 1 FROM pg_proc WHERE pronamespace='m1_demo'::regnamespace AND proname NOT IN ('dataset_payload','touch_updated_at')) THEN RAISE EXCEPTION 'Namespace changed'; END IF;
END $$;
DROP SCHEMA m1_demo CASCADE;
DROP OWNED BY __ROLE__;
DROP ROLE __ROLE__;
COMMIT;
'@
$cleanup=$cleanup.Replace('__ROLE__',$role)
$cleanup | docker exec -i $pg[0] psql -X -q -U postgres -d hotel_acceptance_db -v ON_ERROR_STOP=1
if ($LASTEXITCODE -ne 0) { throw 'Analytics schema cleanup failed.' }
