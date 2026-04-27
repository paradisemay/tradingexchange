INSERT INTO trading.quotes_raw (symbol, event_time, price, ingested_at)
WITH ['SBER', 'GAZP', 'YNDX', 'LKOH', 'ROSN'] AS symbols
SELECT
    symbols[(number % length(symbols)) + 1] AS symbol,
    now64(3, 'UTC') - toIntervalMillisecond((100000 - number) * 50) AS event_time,
    round(100 + (number % 1000) * 0.13 + randCanonical() * 3, 4) AS price,
    now64(3, 'UTC') AS ingested_at
FROM numbers(100000) AS generated_quotes;
