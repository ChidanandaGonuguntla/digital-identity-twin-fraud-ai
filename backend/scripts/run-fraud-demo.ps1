param(
    [string]$BaseUrl = "http://localhost:9997",
    [string]$DataFile = "$PSScriptRoot\data\fraud-demo.json",
    [switch]$SkipSeed
)

$ErrorActionPreference = "Stop"

if (-not $SkipSeed)
{
    & "$PSScriptRoot\seed-baseline.ps1" -BaseUrl $BaseUrl
    Write-Host ""
}

if (-not (Test-Path $DataFile))
{
    throw "Demo data file not found: $DataFile"
}

$scenarios = Get-Content $DataFile -Raw | ConvertFrom-Json
$endpoint = "$BaseUrl/api/v1/fraud/decisions"

Write-Host "Running $( $scenarios.Count ) fraud demo scenarios"

foreach ($scenario in $scenarios)
{
    $body = $scenario.request | ConvertTo-Json -Depth 6 -Compress
    try
    {
        $response = Invoke-RestMethod -Uri $endpoint -Method Post -Body $body -ContentType "application/json"
        Write-Host ("  [{0}] {1} -> {2} (score {3})" -f $scenario.label, $scenario.request.transactionId, $response.decision, $response.finalScore)
    }
    catch
    {
        $detail = $_.ErrorDetails.Message
        if (-not $detail -and $_.Exception.Response)
        {
            $reader = [System.IO.StreamReader]::new($_.Exception.Response.GetResponseStream())
            $detail = $reader.ReadToEnd()
        }
        throw "Failed on $( $scenario.label ): $detail"
    }
}

Write-Host ""
Write-Host "Recent audit:"
Write-Host "  GET $BaseUrl/api/v1/audit/decisions/recent"
