param(
    [string]$BaseUrl = "http://localhost:9997",
    [string]$DataFile = "$PSScriptRoot\data\baseline-seed.json"
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path $DataFile))
{
    throw "Seed data file not found: $DataFile"
}

$transactions = Get-Content $DataFile -Raw | ConvertFrom-Json
$endpoint = "$BaseUrl/api/v1/fraud/decisions"

Write-Host "Seeding $( $transactions.Count ) baseline transactions to $endpoint"

foreach ($txn in $transactions)
{
    $body = $txn | ConvertTo-Json -Depth 6 -Compress
    try
    {
        $response = Invoke-RestMethod -Uri $endpoint -Method Post -Body $body -ContentType "application/json"
        Write-Host ("  {0} -> {1} (score {2})" -f $txn.transactionId, $response.decision, $response.finalScore)
    }
    catch
    {
        $detail = $_.ErrorDetails.Message
        if (-not $detail -and $_.Exception.Response)
        {
            $reader = [System.IO.StreamReader]::new($_.Exception.Response.GetResponseStream())
            $detail = $reader.ReadToEnd()
        }
        throw "Failed on $( $txn.transactionId ): $detail"
    }
}

Write-Host "Baseline seed complete. Inspect twin:"
Write-Host "  GET $BaseUrl/api/v1/twins/CUST-001"
