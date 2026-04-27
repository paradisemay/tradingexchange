param(
    [string]$EnvFile = ".env",
    [string]$Symbol = "SBER"
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

$from = (Get-Date).ToUniversalTime().AddHours(-2).ToString("yyyy-MM-dd HH:mm:ss")
$to = (Get-Date).ToUniversalTime().AddMinutes(1).ToString("yyyy-MM-dd HH:mm:ss")

docker exec $container clickhouse-client --user $user --password $password --query "SELECT count() AS total_points FROM trading.quotes_raw"

docker exec $container clickhouse-client --user $user --password $password --param_symbol $Symbol --param_from "$from" --param_to "$to" --param_limit 10 --query @"
SELECT event_time AS time, price
FROM trading.quotes_raw
WHERE symbol = {symbol:String}
  AND event_time >= parseDateTime64BestEffort({from:String}, 3, 'UTC')
  AND event_time < parseDateTime64BestEffort({to:String}, 3, 'UTC')
ORDER BY event_time
LIMIT {limit:UInt32}
"@

docker exec $container clickhouse-client --user $user --password $password --param_symbol $Symbol --param_from "$from" --param_to "$to" --param_limit 10 --query @"
SELECT bucket AS time, open, high, low, close, volume
FROM trading.quotes_ohlc_1m_read
WHERE symbol = {symbol:String}
  AND bucket >= parseDateTimeBestEffort({from:String}, 'UTC')
  AND bucket < parseDateTimeBestEffort({to:String}, 'UTC')
ORDER BY bucket
LIMIT {limit:UInt32}
"@

docker exec $container clickhouse-client --user $user --password $password --param_symbol $Symbol --param_from "$from" --param_to "$to" --query @"
SELECT
    min(price) AS min_price,
    max(price) AS max_price,
    argMin(price, event_time) AS first_price,
    argMax(price, event_time) AS last_price,
    count() AS points
FROM trading.quotes_raw
WHERE symbol = {symbol:String}
  AND event_time >= parseDateTime64BestEffort({from:String}, 3, 'UTC')
  AND event_time < parseDateTime64BestEffort({to:String}, 3, 'UTC')
"@

