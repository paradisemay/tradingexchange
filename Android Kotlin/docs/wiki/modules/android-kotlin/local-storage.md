# Local Storage

## DataStore

DataStore stores only session tokens:

- `access_token`
- `refresh_token`

Tokens are cleared on logout or refresh failure.

## Room

Room caches screen data:

- portfolio positions;
- cash balance;
- instruments;
- orders;
- transactions;
- last quotes.

The cache is not the source of truth for trading. It is used for quick screen rendering and stale/offline display.

## Source of Truth

Trading operations are always sent to backend. After successful order creation, the app refreshes the portfolio and updates local Room rows.

## Money Values

Network DTOs store money and quantity as `String`, matching the API contract. Domain models use `BigDecimal`. UI renders formatted domain values.

`Double` and `Float` are not used for financial calculations.
