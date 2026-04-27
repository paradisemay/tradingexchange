# ClickHouse Runbook

## Local Start

```powershell
cd C:\Users\yarik\GolandProjects\tradingexchange\clickhouse
Copy-Item .env.example .env
docker compose up -d
docker compose ps
```

## Load Mock Data

```powershell
.\scripts\load-mock-data.ps1
```

## Smoke Test

```powershell
.\scripts\smoke-test.ps1 -Symbol SBER
```

## Stop

```powershell
docker compose down
```

## Remove Local Data

This deletes only the ClickHouse Docker volume:

```powershell
docker compose down -v
```

