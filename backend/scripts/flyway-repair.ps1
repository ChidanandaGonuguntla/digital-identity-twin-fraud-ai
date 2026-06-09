$ErrorActionPreference = "Stop"
$backendRoot = Split-Path -Parent $PSScriptRoot
$migrations = Join-Path $backendRoot "src\main\resources\db\migration"
$url = $env:SPRING_DATASOURCE_URL
if (-not $url) { $url = "jdbc:postgresql://localhost:5432/digital_twin_db" }
$user = if ($env:SPRING_DATASOURCE_USERNAME) { $env:SPRING_DATASOURCE_USERNAME } else { "postgres" }
$pass = if ($env:SPRING_DATASOURCE_PASSWORD) { $env:SPRING_DATASOURCE_PASSWORD } else { "admin" }

docker run --rm `
  -v "${migrations}:/flyway/sql" `
  --network host `
  flyway/flyway:10 `
  -url="$url" `
  -schemas=identity_twin `
  -defaultSchema=identity_twin `
  -user="$user" `
  -password="$pass" `
  -locations=filesystem:/flyway/sql `
  repair

Write-Host "Flyway repair completed. Restart the backend without APP_FLYWAY_REPAIR."
