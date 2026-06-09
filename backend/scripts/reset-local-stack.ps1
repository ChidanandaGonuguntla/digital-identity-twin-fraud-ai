param(
    [string]$PgHost = "localhost",
    [int]$PgPort = 5432,
    [string]$PgDatabase = "digital_twin_db",
    [string]$PgUsername = "postgres",
    [string]$PgPassword = "admin",
    [string]$KafkaContainer = "dti-kafka",
    [string]$ConsumerGroup = "digital-twin-fraud-service",
    [switch]$SkipConfirm
)

$ErrorActionPreference = "Stop"

$topics = @(
    "dti.transaction-events",
    "dti.fraud-decisions",
    "dti.fraud-audit-events",
    "dti.step-up-challenges",
    "dti.twin-drift-events",
    "dti.transaction-events.dlt",
    "dti.fraud-decisions.dlt"
)

if (-not $SkipConfirm)
{
    Write-Host "This will:"
    Write-Host "  - TRUNCATE all identity_twin tables on ${PgHost}:${PgPort}/${PgDatabase}"
    Write-Host "  - DELETE Kafka topics and consumer group on container ${KafkaContainer}"
    $answer = Read-Host "Type RESET to continue"
    if ($answer -ne "RESET")
    {
        Write-Host "Cancelled."
        exit 0
    }
}

Write-Host "Resetting local Postgres..."
& "$PSScriptRoot\reset-local-db.ps1" -HostName $PgHost -Port $PgPort -Database $PgDatabase -Username $PgUsername -Password $PgPassword -SkipConfirm

$kafkaRunning = docker ps --format "{{.Names}}" | Select-String -Pattern "^${KafkaContainer}$" -Quiet
if (-not $kafkaRunning)
{
    Write-Host "Kafka container '${KafkaContainer}' is not running. Postgres reset done; start Kafka before publishing."
    exit 0
}

Write-Host "Deleting Kafka consumer group ${ConsumerGroup}..."
docker exec $KafkaContainer /opt/kafka/bin/kafka-consumer-groups.sh `
    --bootstrap-server localhost:9092 `
    --delete `
    --group $ConsumerGroup 2> $null

foreach ($topic in $topics)
{
    $exists = docker exec $KafkaContainer /opt/kafka/bin/kafka-topics.sh `
        --bootstrap-server localhost:9092 `
        --list 2> $null | Select-String -Pattern "^${topic}$" -Quiet
    if ($exists)
    {
        Write-Host "Deleting Kafka topic ${topic}..."
        docker exec $KafkaContainer /opt/kafka/bin/kafka-topics.sh `
            --bootstrap-server localhost:9092 `
            --delete `
            --topic $topic | Out-Null
    }
}

Write-Host "Kafka purge complete. Topics recreate automatically on next publish."
Write-Host "Local stack reset complete."
