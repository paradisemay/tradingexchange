# ADR-0005: Compose Canvas Charts

## Status

Accepted

## Context

The app needs two chart types similar to an investment terminal:

- regular line price chart;
- candlestick OHLC chart.

The API returns prepared point/candle snapshot data. WebSocket quote messages provide live updates for the currently open ticker. The first MVP does not require indicators, gestures, zoom, crosshair or a full trading chart engine.

## Decision

Render charts with Jetpack Compose `Canvas` instead of adding a charting dependency. Load chart history through REST, then update the current line/candlestick data from `QuotesRepository`.

## Alternatives

- Add a third-party chart library. Deferred because the MVP needs simple, inspectable line/candle rendering and no advanced indicators yet.
- Render chart as WebView. Rejected because the app is native Compose and should not embed a web chart for core UX.

## Consequences

- No new runtime dependency.
- Chart code remains under app control and easy to tune.
- Open chart screens can move in near real time without REST polling.
- Future work can replace Canvas with a specialized library if requirements grow to gestures, indicators and high-density intraday data.
