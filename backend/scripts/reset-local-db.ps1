param(
    [string]$HostName = "localhost",
    [int]$Port = 5432,
    [string]$Database = "digital_twin_db",
    [string]$Username = "postgres",
    [string]$Password = "admin",
    [switch]$SkipConfirm
)

$ErrorActionPreference = "Stop"
$sqlFile = Join-Path $PSScriptRoot "reset-local-db.sql"

function Get-PsqlPath
{
    $cmd = Get-Command psql -ErrorAction SilentlyContinue
    if ($cmd)
    {
        return $cmd.Source
    }
    $common = @(
        "C:\Program Files\PostgreSQL\17\bin\psql.exe",
        "C:\Program Files\PostgreSQL\16\bin\psql.exe",
        "C:\Program Files\PostgreSQL\15\bin\psql.exe"
    )
    foreach ($path in $common)
    {
        if (Test-Path $path)
        {
            return $path
        }
    }
    return $null
}

if (-not $SkipConfirm)
{
    Write-Host "This will DELETE ALL DATA in schema identity_twin on ${HostName}:${Port}/${Database}"
    Write-Host "Flyway history is kept. Model registry baseline is re-seeded."
    $answer = Read-Host "Type RESET to continue"
    if ($answer -ne "RESET")
    {
        Write-Host "Cancelled."
        exit 0
    }
}

$env:PGPASSWORD = $Password
$psql = Get-PsqlPath

if ($psql)
{
    & $psql -h $HostName -p $Port -U $Username -d $Database -v ON_ERROR_STOP=1 -f $sqlFile
}
else
{
    $sqlMount = ($sqlFile -replace '\\', '/')
    if ($sqlMount -match '^([A-Z]):/(.*)$')
    {
        $sqlMount = "/$($Matches[1].ToLower() )/$( $Matches[2] )"
    }
    docker run --rm `
        -e PGPASSWORD=$Password `
        -v "${sqlMount}:/reset.sql:ro" `
        postgres:16-alpine `
        psql -h host.docker.internal -p $Port -U $Username -d $Database -v ON_ERROR_STOP=1 -f /reset.sql
}

Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
Write-Host "Local database reset complete."
