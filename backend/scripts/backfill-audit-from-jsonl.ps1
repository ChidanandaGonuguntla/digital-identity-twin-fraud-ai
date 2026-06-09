param(
    [string]$HostName = "localhost",
    [int]$Port = 5432,
    [string]$Database = "digital_twin_db",
    [string]$Username = "postgres",
    [string]$Password = "admin",
    [string]$Schema = "identity_twin",
    [string]$JsonlFile = "$PSScriptRoot\..\..\kafka-fraud-event-publisher\data\fraud-events-100k.jsonl",
    [int]$BatchSize = 1000
)

$ErrorActionPreference = "Stop"

function Get-PsqlPath
{
    $cmd = Get-Command psql -ErrorAction SilentlyContinue
    if ($cmd)
    {
        return $cmd.Source
    }
    $common = @(
        "C:\Program Files\PostgreSQL\16\bin\psql.exe",
        "C:\Program Files\PostgreSQL\15\bin\psql.exe",
        "C:\Program Files\PostgreSQL\14\bin\psql.exe"
    )
    foreach ($path in $common)
    {
        if (Test-Path $path)
        {
            return $path
        }
    }
    throw "psql not found. Install PostgreSQL client tools or add psql to PATH."
}

function Escape-SqlLiteral([string]$Value)
{
    "'" + ($Value -replace "'", "''") + "'"
}

if (-not (Test-Path $JsonlFile))
{
    throw "JSONL file not found: $JsonlFile"
}

$psql = Get-PsqlPath
$env:PGPASSWORD = $Password
$conn = "host=$HostName port=$Port dbname=$Database user=$Username"

Write-Host "Backfilling audit transaction fields from $JsonlFile"
Write-Host "Target: $HostName: $Port/$Database schema=$Schema"

$lines = Get-Content $JsonlFile
$total = $lines.Count
$updated = 0
$batch = New-Object System.Collections.Generic.List[string]

for ($index = 0; $index -lt $total; $index++) {
    $line = $lines[$index].Trim()
    if (-not $line)
    {
        continue
    }

    $event = $line | ConvertFrom-Json
    $transactionId = [string]$event.transactionId
    $amount = [string]$event.amount
    $merchantCategory = [string]$event.merchantCategory
    $deviceId = [string]$event.deviceId
    $snapshot = $line

    $batch.Add(
            "UPDATE $Schema.fraud_decision_audit SET " +
                    "amount = $amount, " +
                    "merchant_category = $( Escape-SqlLiteral $merchantCategory ), " +
                    "device_id = $( Escape-SqlLiteral $deviceId ), " +
                    "event_snapshot_json = $( Escape-SqlLiteral $snapshot )::jsonb " +
                    "WHERE transaction_id = $( Escape-SqlLiteral $transactionId ) " +
                    "AND (amount IS NULL OR amount = 0 OR merchant_category IS NULL OR merchant_category = '' OR jsonb_typeof(event_snapshot_json) <> 'object');"
    )

    if ($batch.Count -ge $BatchSize -or $index -eq ($total - 1))
    {
        $sql = "BEGIN;`n" + ($batch -join "`n") + "`nCOMMIT;"
        $sql | & $psql $conn -v ON_ERROR_STOP=1 -q
        $updated += $batch.Count
        Write-Host ("  processed {0}/{1}" -f $updated, $total)
        $batch.Clear()
    }
}

Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
Write-Host "Backfill complete. Restart backend and refresh Live Decisions."
