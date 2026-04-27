param(
    [string]$EnvFile = ".env"
)

$ErrorActionPreference = "Stop"

if (Test-Path $EnvFile) {
    Get-Content $EnvFile | Where-Object { $_ -match "^\s*[^#].*=" } | ForEach-Object {
        $name, $value = $_ -split "=", 2
        [Environment]::SetEnvironmentVariable($name.Trim(), $value.Trim(), "Process")
    }
}

$container = if ($env:CLICKHOUSE_CONTAINER_NAME) { $env:CLICKHOUSE_CONTAINER_NAME } else { "tradingexchange-clickhouse" }
$user = if ($env:CLICKHOUSE_USER) { $env:CLICKHOUSE_USER } else { "trading_app" }
$password = $env:CLICKHOUSE_PASSWORD

if ([string]::IsNullOrWhiteSpace($password)) {
    throw "CLICKHOUSE_PASSWORD is required. Copy .env.example to .env and set a local password."
}

Get-Content -Raw ".\scripts\mock-data.sql" |
    docker exec -i $container clickhouse-client --user $user --password $password --multiquery

docker exec $container clickhouse-client --user $user --password $password --query "SELECT symbol, count() AS points FROM trading.quotes_raw GROUP BY symbol ORDER BY symbol"

