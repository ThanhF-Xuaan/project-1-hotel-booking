# Pure validation: no files, connections or environment mutation.
function Resolve-AcceptanceRagRuntime {
    param([Parameter(Mandatory = $true)]$Config)
    $mode = [string]$Config.HOTEL_AI_RAG_STORE_TYPE
    if ([string]::IsNullOrEmpty($mode)) { $mode = 'in-memory' }
    if ($mode -cnotin @('in-memory', 'pgvector')) { throw 'Unsupported acceptance RAG mode.' }
    if ($mode -eq 'in-memory') {
        return @{ Mode = $mode; Demo = 'true'; Profile = $null; Url = $null; Username = $null; Password = $null; JvmArguments = $null }
    }
    if ([string]$Config.HOTEL_AI_RAG_DEMO_CORPUS_ENABLED -ne 'false') {
        throw 'Persistent acceptance requires explicit demo-corpus=false.'
    }
    $url = [string]$Config.RAG_PGVECTOR_JDBC_URL
    if ($url -cnotmatch '\Ajdbc:postgresql://127\.0\.0\.1:15435/t12_pgvector_http_[a-z0-9_]{1,40}\z') {
        throw 'Persistent acceptance requires its dedicated loopback database.'
    }
    if ([string]$Config.RAG_PGVECTOR_USERNAME -cne 'rag_http_writer' -or
        [string]$Config.RAG_PGVECTOR_PASSWORD -cnotmatch '\A[A-Za-z0-9]{48}\z') {
        throw 'Persistent acceptance requires its restricted runtime credential.'
    }
    return @{ Mode = $mode; Demo = 'false'; Profile = 'rag-pgvector-local'; Url = $url;
        Username = [string]$Config.RAG_PGVECTOR_USERNAME; Password = [string]$Config.RAG_PGVECTOR_PASSWORD;
        JvmArguments = '-Duser.timezone=Asia/Ho_Chi_Minh' }
}
