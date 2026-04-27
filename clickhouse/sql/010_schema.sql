CREATE DATABASE IF NOT EXISTS trading;

CREATE TABLE IF NOT EXISTS trading.quotes_raw
(
    symbol LowCardinality(String),
    event_time DateTime64(3, 'UTC'),
    price Float64,
    ingested_at DateTime64(3, 'UTC') DEFAULT now64(3)
)
ENGINE = MergeTree
PARTITION BY toYYYYMM(event_time)
ORDER BY (symbol, event_time)
TTL event_time + INTERVAL 180 DAY
SETTINGS index_granularity = 8192;

CREATE TABLE IF NOT EXISTS trading.quotes_ohlc_1m
(
    symbol LowCardinality(String),
    bucket DateTime('UTC'),
    open_state AggregateFunction(argMin, Float64, DateTime64(3, 'UTC')),
    high_state AggregateFunction(max, Float64),
    low_state AggregateFunction(min, Float64),
    close_state AggregateFunction(argMax, Float64, DateTime64(3, 'UTC')),
    volume_state AggregateFunction(count, UInt64)
)
ENGINE = AggregatingMergeTree
PARTITION BY toYYYYMM(bucket)
ORDER BY (symbol, bucket)
TTL bucket + INTERVAL 180 DAY
SETTINGS index_granularity = 8192;

CREATE MATERIALIZED VIEW IF NOT EXISTS trading.quotes_ohlc_1m_mv
TO trading.quotes_ohlc_1m
AS
SELECT
    symbol,
    toStartOfMinute(event_time) AS bucket,
    argMinState(price, event_time) AS open_state,
    maxState(price) AS high_state,
    minState(price) AS low_state,
    argMaxState(price, event_time) AS close_state,
    countState() AS volume_state
FROM trading.quotes_raw
GROUP BY
    symbol,
    bucket;

CREATE VIEW IF NOT EXISTS trading.quotes_ohlc_1m_read
AS
SELECT
    symbol,
    bucket,
    argMinMerge(open_state) AS open,
    maxMerge(high_state) AS high,
    minMerge(low_state) AS low,
    argMaxMerge(close_state) AS close,
    countMerge(volume_state) AS volume
FROM trading.quotes_ohlc_1m
GROUP BY
    symbol,
    bucket;

