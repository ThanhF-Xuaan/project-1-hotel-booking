$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'Rag-Runtime.ps1')
$script:Checks = 0
function Check($Condition) {
    if (-not $Condition) { throw 'RAG runtime configuration assertion failed.' }
    $script:Checks++
}
function Reject($Config) {
    $rejected = $false
    try { $null = Resolve-AcceptanceRagRuntime $Config } catch { $rejected = $true }
    Check $rejected
}
Check ((Resolve-AcceptanceRagRuntime ([PSCustomObject]@{})).Mode -eq 'in-memory')
Reject ([PSCustomObject]@{ HOTEL_AI_RAG_STORE_TYPE = 'postgres' })
Reject ([PSCustomObject]@{ HOTEL_AI_RAG_STORE_TYPE = 'pgvector' })
$valid = @{ HOTEL_AI_RAG_STORE_TYPE = 'pgvector'; HOTEL_AI_RAG_DEMO_CORPUS_ENABLED = 'false';
    RAG_PGVECTOR_JDBC_URL = 'jdbc:postgresql://127.0.0.1:15435/t12_pgvector_http_synthetic';
    RAG_PGVECTOR_USERNAME = 'rag_http_writer'; RAG_PGVECTOR_PASSWORD = ('a' * 48) }
$resolved = Resolve-AcceptanceRagRuntime ([PSCustomObject]$valid)
Check ($resolved.Mode -eq 'pgvector' -and $resolved.Profile -eq 'rag-pgvector-local' -and $resolved.Demo -eq 'false')
Check ($resolved.JvmArguments -eq '-Duser.timezone=Asia/Ho_Chi_Minh')
Check ($null -eq (Resolve-AcceptanceRagRuntime ([PSCustomObject]@{})).JvmArguments)
foreach ($url in @('jdbc:postgresql://remote:15435/t12_pgvector_http_synthetic',
    'jdbc:postgresql://127.0.0.1:5432/hotel_booking',
    'jdbc:postgresql://127.0.0.1:15435/t12_pgvector_http_synthetic?password=secret',
    'jdbc:postgresql://127.0.0.1:15435/t12_pgvector_http_synthetic/other',
    "jdbc:postgresql://127.0.0.1:15435/t12_pgvector_http_synthetic`n")) {
    $bad = $valid.Clone(); $bad.RAG_PGVECTOR_JDBC_URL = $url; Reject ([PSCustomObject]$bad)
}
foreach ($property in @('HOTEL_AI_RAG_DEMO_CORPUS_ENABLED', 'RAG_PGVECTOR_USERNAME', 'RAG_PGVECTOR_PASSWORD')) {
    $bad = $valid.Clone(); $bad[$property] = 'invalid'; Reject ([PSCustomObject]$bad)
}
Check ($valid.RAG_PGVECTOR_JDBC_URL -eq 'jdbc:postgresql://127.0.0.1:15435/t12_pgvector_http_synthetic')
Write-Host "PASS: $script:Checks RAG runtime configuration checks; no connections or credentials emitted."
