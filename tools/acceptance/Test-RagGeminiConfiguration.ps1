<# Offline structural tests do not create containers, call APIs, read real secrets or approve deployment. #>
[CmdletBinding()]
param([string]$ConfigurationFile,[switch]$SelfTest,[string]$PackagedJar)
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'Start-RagGeminiDemo.ps1') -ConfigurationFile $ConfigurationFile
function Assert-Check([bool]$Condition,[string]$Name) { if (-not $Condition) { throw "OFFLINE_CHECK_FAILED: $Name" }; $script:CheckCount++ }
function Assert-Rejected([scriptblock]$Work,[string]$Name) {
    $rejected=$false; try { & $Work } catch { $rejected=$true }; Assert-Check $rejected $Name
}
$script:CheckCount=0
if ($SelfTest) {
    $config=Get-RagTemplate
    $config.Image='pgvector/pgvector@sha256:'+('c'*64); $config.PrimaryContainerId='d'*64
    $config.PrimaryReaderRole='m1_gemini_reader_0123456789'; $config.ChatModel='synthetic-chat'; $config.EmbeddingModel='synthetic-embedding'
    $config.EmbeddingDimension=3; $config.JavaExecutable=[IO.Path]::GetFullPath((Get-Command java).Source)
    $config.BackendJar=Join-Path ([IO.Path]::GetTempPath()) 'synthetic-not-executed.jar'
    foreach ($key in @('BackendJarSha256','ManifestSha256','PolicySha256','ProvisionTemplateSha256')) { $config[$key]='e'*64 }
    Assert-RagConfig $config -StructuralOnly; Assert-Check $true 'valid synthetic structure'
    $mutations=@(
        @('RagDatabase','hotel_booking_db'),@('RagDatabase','t12_pgvector_http_existing'),@('RagRole','postgres'),
        @('RunId','../foreign'),@('RagPort',15432),@('BackendPort',18080),@('BackendPort',15436),@('RagPort',80),
        @('Image','pgvector/pgvector:latest'),@('DockerContext','remote-ssh'),@('PrimaryContainerId','foreign'),@('PrimaryReaderRole','postgres'),
        @('KeycloakUrl','https://external.invalid'),@('ChatModel','https://external.invalid/model'),@('EmbeddingDimension',0),
        @('EmbeddingDimension',3073),@('EmbeddingDimension',3.5),@('RetrievalThreshold',0.10),@('BackendJar','relative.jar'),@('BackendJar','\\foreign\share\app.jar'),@('PolicySha256','unlocked')
    )
    foreach ($change in $mutations) {
        $copy=@{}; foreach($key in $config.Keys) { $copy[$key]=$config[$key] }; $copy[$change[0]]=$change[1]
        Assert-Rejected { Assert-RagConfig $copy -StructuralOnly } ('reject '+$change[0])
    }
    $secret=@{geminiKey='SYNTHETIC_KEY_SENTINEL';ragPassword=('1'*48);adminPassword=('2'*48);primaryPassword='SYNTHETIC_DB_PASSWORD';keycloakClientSecret='SYNTHETIC_AUTH_SECRET';anonTokenSecret=('3'*64)}
    $before=[Environment]::GetEnvironmentVariable('RAG_GEMINI_API_KEY','Process')
    $child=Get-RagChildEnvironment $config $secret
    Assert-Check ($child.RAG_GEMINI_API_KEY -ceq $secret.geminiKey) 'dedicated child key'
    Assert-Check ([Environment]::GetEnvironmentVariable('RAG_GEMINI_API_KEY','Process') -ceq $before) 'parent environment untouched'
    Assert-Check (@($child.Keys | Where-Object { $_ -match '^(VITE_|GEMINI_API_KEY$|GOOGLE_API_KEY$|JAVA_TOOL_OPTIONS$|SPRING_APPLICATION_JSON$)' }).Count -eq 0) 'no frontend or inherited key/override'
    Assert-Check ($child.HOTEL_AI_ADVISOR_PROVIDER_GEMINI_ENABLED -ceq 'false' -and $child.RAG_GEMINI_LEDGER_REQUIRED -ceq 'true') 'R1 off and ledger required'
    Assert-Check ($child.RAG_PGVECTOR_JDBC_URL -ceq "jdbc:postgresql://127.0.0.1:15436/$($config.RagDatabase)") 'dedicated loopback target'
    $commandLine=(Get-RagBackendArguments $config) -join ' '
    Assert-Check (-not $commandLine.Contains($secret.geminiKey) -and -not $commandLine.Contains($secret.primaryPassword) -and
        $commandLine.Contains('--spring.config.location=classpath:')) 'secrets absent from actual Java arguments and external config excluded'
    $sql=Get-RagProvisionSql $config '__PRIVATE_RAG_PASSWORD__'
    Assert-Check ($sql.Contains('AS $$') -and -not $sql.Contains('\$')) 'valid trigger delimiter'
    Assert-Check ($sql.Contains('NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS') -and -not $sql.Contains('GRANT ALL')) 'least privilege template'
    $launcher=[IO.File]::ReadAllText((Join-Path $PSScriptRoot 'Start-RagGeminiDemo.ps1'))
    Assert-Check (-not $launcher.Contains('& npm') -and -not $launcher.Contains('& $mavenWrapper')) 'no frontend or offline launcher invocation'
    Assert-Check ($launcher.Contains('Environment.Clear()') -and -not $launcher.Contains('$env:RAG_GEMINI_API_KEY')) 'child environment isolation'
    $approvalTemplate=Get-RagApprovalTemplate
    Assert-Check (@($approvalTemplate.gates.Values | Where-Object { $_.status -cne 'PENDING' -or $_.reviewer -or $_.reference }).Count -eq 0) 'templates never fabricate owner approvals'
    $policyTemplate=Get-RagPolicyTemplate
    Assert-Check (-not $policyTemplate.allBillableTokensCovered -and -not $policyTemplate.embedding.boundsAndWorstCaseRatesApproved -and
        -not $policyTemplate.generation.boundsAndWorstCaseRatesApproved -and -not $policyTemplate.capturedAtUtc) 'unreviewed prices/bounds cannot authorize live'
    foreach ($file in @('Start-RagGeminiDemo.ps1','Test-RagGeminiConfiguration.ps1','Test-RagGeminiLive.ps1')) {
        $tokens=$null; $errors=$null
        $null=[Management.Automation.Language.Parser]::ParseFile((Join-Path $PSScriptRoot $file),[ref]$tokens,[ref]$errors)
        Assert-Check ($errors.Count -eq 0) ('parse '+$file)
    }
    if($PackagedJar) {
        # Synthetic private fixture only: never create an APPROVED owner record or perform native/DB/API work.
        $fixture=Join-Path ([IO.Path]::GetTempPath()) ('rag-gemini-offline-'+[Guid]::NewGuid().ToString('N'))
        $null=[IO.Directory]::CreateDirectory($fixture)
        $identity=[Security.Principal.WindowsIdentity]::GetCurrent().User
        $acl=[Security.AccessControl.DirectorySecurity]::new();$acl.SetOwner($identity);$acl.SetAccessRuleProtection($true,$false)
        $rule=[Security.AccessControl.FileSystemAccessRule]::new($identity,'FullControl','ContainerInherit,ObjectInherit','None','Allow');$acl.AddAccessRule($rule)
        Set-Acl -LiteralPath $fixture -AclObject $acl
        $config.RuntimeDirectory=$fixture;$config.SecretFile=Join-Path $fixture 'synthetic-secrets.json';$config.PolicyFile=Join-Path $fixture 'policy.json'
        [IO.File]::WriteAllText($config.SecretFile,($secret|ConvertTo-Json),[Text.UTF8Encoding]::new($false))
        $config.BackendJar=[IO.Path]::GetFullPath($PackagedJar);$config.BackendJarSha256=Get-RagHash $config.BackendJar
        $config.ManifestSha256=Get-RagHash $config.Manifest
        $snapshot=Join-Path $fixture 'pricing-snapshot.html';[IO.File]::WriteAllText($snapshot,'SYNTHETIC OFFLINE TEST RATES - NOT A PROVIDER PRICE QUOTE',[Text.UTF8Encoding]::new($false))
        $policy=Get-RagPolicyTemplate;$policy.capturedAtUtc=[DateTimeOffset]::UtcNow.AddMinutes(-1).ToString('o');$policy.expiresAtUtc=[DateTimeOffset]::UtcNow.AddHours(1).ToString('o')
        $policy.pricingSnapshotSha256=Get-RagHash $snapshot;$policy.allBillableTokensCovered=$true
        $policy.embedding=@{model='synthetic-embedding';inputTokenBound=8192;inputUsdPerMillion='0.15';outputUsdPerMillion='0';boundsAndWorstCaseRatesApproved=$true}
        $policy.generation=@{model='synthetic-chat';inputTokenBound=65536;outputTokenBound=768;inputUsdPerMillion='0.15';outputUsdPerMillion='0.60';boundsAndWorstCaseRatesApproved=$true}
        [IO.File]::WriteAllText($config.PolicyFile,($policy|ConvertTo-Json -Depth 10),[Text.UTF8Encoding]::new($false))
        $config.PolicySha256=Get-RagHash $config.PolicyFile;$config.ProvisionTemplateSha256=Get-RagTextHash (Get-RagProvisionSql $config '__PRIVATE_RAG_PASSWORD__')
        Assert-RagConfig $config;Assert-Check $true 'full private config plus actual packaged profile/ledger class validated offline'
        $policy.generation.inputUsdPerMillion='';Assert-Rejected { Assert-RagPricePolicy $config $policy } 'missing price blocks before startup'
        $policy.generation.inputUsdPerMillion='0.15';$policy.expiresAtUtc=[DateTimeOffset]::UtcNow.AddHours(-1).ToString('o')
        Assert-Rejected { Assert-RagPricePolicy $config $policy } 'expired snapshot blocks before startup'
        $pending=Get-RagApprovalTemplate;$pending.approvedConfigSha256='f'*64
        $pendingFile=Join-Path $fixture 'pending-approvals.json';[IO.File]::WriteAllText($pendingFile,($pending|ConvertTo-Json -Depth 10),[Text.UTF8Encoding]::new($false))
        Assert-Rejected { Assert-RagApproval $pendingFile ('f'*64) @('G04','G05','G06','G09') } 'pending human approvals block execution'
        $config.BackendJarSha256='0'*64;Assert-Rejected { Assert-RagConfig $config } 'stale jar hash rejected'
    }
    [ordered]@{status='PASS';checks=$script:CheckCount;paidCalls=0;databaseOperations=0;scope='offline synthetic configuration tests only'} | ConvertTo-Json
} else {
    try { $config=Read-RagJson $ConfigurationFile; Assert-RagConfig $config
        [ordered]@{status='CONFIRMED_CONFIG_ONLY';configSha256=(Get-RagHash $ConfigurationFile);paidCalls=0;databaseOperations=0;approvals='NOT VERIFIED BY THIS CHECK'} | ConvertTo-Json
    } catch { throw 'RAG_CONFIGURATION_CHECK_FAILED: inspect paths, hashes, private ACLs and reviewed model/target settings.' }
}
