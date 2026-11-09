<#
.SYNOPSIS
Prepare private draft configuration only. No Docker, database, IAM or model calls.
Passwords are for NEW sandbox roles/services and do not authenticate existing services.
ImportGeminiFromUserEnvironment copies a locally configured key without printing it.
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$RuntimeDirectory,
    [switch]$ImportGeminiFromUserEnvironment
)
$ErrorActionPreference='Stop'
if (-not $IsWindows) { throw 'WINDOWS_PRIVATE_ACL_REQUIRED' }
$repo=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
if (-not [IO.Path]::IsPathFullyQualified($RuntimeDirectory) -or $RuntimeDirectory.StartsWith('\\')) { throw 'LOCAL_ABSOLUTE_PATH_REQUIRED' }
$target=[IO.Path]::GetFullPath($RuntimeDirectory).TrimEnd('\','/')
if ($target.Equals($repo,[StringComparison]::OrdinalIgnoreCase) -or
    $target.StartsWith($repo+'\',[StringComparison]::OrdinalIgnoreCase) -or
    $target.Length -le [IO.Path]::GetPathRoot($target).Length) { throw 'PRIVATE_DIRECTORY_OUTSIDE_REPOSITORY_REQUIRED' }
if (Test-Path -LiteralPath $target) { throw 'REFUSING_TO_OVERWRITE_EXISTING_DIRECTORY' }
$ancestor=Split-Path -Parent $target
while ($ancestor) {
    if ((Test-Path -LiteralPath $ancestor) -and ((Get-Item -LiteralPath $ancestor -Force).Attributes -band [IO.FileAttributes]::ReparsePoint)) {
        throw 'REPARSE_POINT_NOT_ALLOWED'
    }
    $ancestor=Split-Path -Parent $ancestor
}
$key=''
if ($ImportGeminiFromUserEnvironment) {
    $key=$env:RAG_GEMINI_API_KEY
    if ([string]::IsNullOrWhiteSpace($key)) { $key=$env:GEMINI_API_KEY }
    if ([string]::IsNullOrWhiteSpace($key) -or $key -match '\s') { throw 'LOCAL_GEMINI_KEY_MISSING_OR_INVALID' }
}
# Create the leaf exclusively; concurrent initialization cannot overwrite its secrets.
$parent=Split-Path -Parent $target
if (-not (Test-Path -LiteralPath $parent)) { throw 'PRIVATE_PARENT_DIRECTORY_MUST_EXIST' }
$lockFile=Join-Path $parent ((Split-Path -Leaf $target)+'.prepare.lock')
$lock=$null
try {
    $lock=[IO.File]::Open($lockFile,[IO.FileMode]::CreateNew,[IO.FileAccess]::Write,[IO.FileShare]::None)
    if (Test-Path -LiteralPath $target) { throw 'REFUSING_TO_OVERWRITE_EXISTING_DIRECTORY' }
    $null=[IO.Directory]::CreateDirectory($target)
    $sid=[Security.Principal.WindowsIdentity]::GetCurrent().User
    $acl=[Security.AccessControl.DirectorySecurity]::new()
    $acl.SetOwner($sid)
    $acl.SetAccessRuleProtection($true,$false)
    foreach ($principal in @($sid,[Security.Principal.SecurityIdentifier]::new('S-1-5-18'))) {
        $acl.AddAccessRule([Security.AccessControl.FileSystemAccessRule]::new($principal,'FullControl',
            [Security.AccessControl.InheritanceFlags]'ContainerInherit,ObjectInherit',
            [Security.AccessControl.PropagationFlags]::None,[Security.AccessControl.AccessControlType]::Allow))
    }
    Set-Acl -LiteralPath $target -AclObject $acl
    function New-PrivateToken { return [Convert]::ToHexString([Security.Cryptography.RandomNumberGenerator]::GetBytes(32)).ToLowerInvariant() }
    $secret=[ordered]@{geminiKey=$key;ragPassword=(New-PrivateToken);adminPassword=(New-PrivateToken);
        primaryPassword=(New-PrivateToken);keycloakClientSecret=(New-PrivateToken);anonTokenSecret=(New-PrivateToken)}
    $run=[Guid]::NewGuid().ToString('N')
    $config=[ordered]@{SchemaVersion='t12-chatbot-draft-v1';State='PREPARED_NOT_PROVISIONED';RunId=$run;CampaignId=$run;
        RuntimeDirectory=$target;SecretFile=(Join-Path $target 'secrets.json');DockerProject='hotel-m1-t12-live';
        BindAddress='127.0.0.1';CorePort=15437;CoreDatabase='hotel_t12_demo';CoreReaderRole=('m1_chat_reader_'+$run.Substring(0,10));
        RagPort=15436;RagDatabase='t12_pgvector_gemini_pilot_hl';RagRole=('m1_chat_rag_'+$run.Substring(0,10));
        RedisPort=16390;KeycloakPort=18091;KeycloakRealm='hotel-t12-demo';KeycloakClientId='hotel-backend';
        BackendPort=18090;FrontendPort=13090;PilotHotelId=4;CoreFixtureLabel='SYNTHETIC';
        ChatModel='';EmbeddingModel='';EmbeddingDimension=0;RetrievalThreshold=$null;
        GenerationLedgerLimitUsd='5';Provisioned=$false;LiveEnabled=$false;
        Note='Draft preparation only. Exact model, prices, quota, images, targets, backups and provisioning must be verified before use.'}
    $utf8=[Text.UTF8Encoding]::new($false)
    [IO.File]::WriteAllText($config.SecretFile,($secret|ConvertTo-Json),$utf8)
    [IO.File]::WriteAllText((Join-Path $target 'configuration.json'),($config|ConvertTo-Json),$utf8)
    [ordered]@{status='PASS_PRIVATE_PREPARATION_ONLY';runtimeDirectory=$target;
        keyImported=[bool]$ImportGeminiFromUserEnvironment;privateAcl=$true;generatedSecrets=5;
        databaseOperations=0;geminiCalls=0;provisioned=$false}|ConvertTo-Json
} finally {
    $key=$null;$secret=$null
    if ($lock) { $lock.Dispose(); Remove-Item -LiteralPath $lockFile }
}
