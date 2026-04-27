-- Raw points for line chart.
SELECT
    event_time AS time,
    price
FROM trading.quotes_raw
WHERE symbol = {symbol:String}
  AND event_time >= parseDateTime64BestEffort({from:String}, 3, 'UTC')
  AND event_time < parseDateTime64BestEffort({to:String}, 3, 'UTC')
ORDER BY event_time
LIMIT {limit:UInt32};

-- One-minute OHLC candles for candlestick chart.
SELECT
    bucket AS time,
    open,
    high,
    low,
    close,
    volume
FROM trading.quotes_ohlc_1m_read
WHERE symbol = {symbol:String}
  AND bucket >= parseDateTimeBestEffort({from:String}, 'UTC')
  AND bucket < parseDateTimeBestEffort({to:String}, 'UTC')
ORDER BY bucket
LIMIT {limit:UInt32};

-- Summary for a selected range.
SELECT
    min(price) AS min_price,
    max(price) AS max_price,
    argMin(price, event_time) AS first_price,
    argMax(price, event_time) AS last_price,
    count() AS points
FROM trading.quotes_raw
WHERE symbol = {symbol:String}
  AND event_time >= parseDateTime64BestEffort({from:String}, 3, 'UTC')
  AND event_time < parseDateTime64BestEffort({to:String}, 3, 'UTC');

-- Latest historical price.
SELECT
    symbol,
    event_time AS time,
    price
FROM trading.quotes_raw
WHERE symbol = {symbol:String}
ORDER BY event_time DESC
LIMIT 1;

