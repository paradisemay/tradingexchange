# Security Notes

## Rules

- No production tokens, passwords or ports are committed.
- Every runtime variable must be documented in `.env.example`.
- Mock tokens are test fixtures only.
- REST auth uses `Authorization: Bearer <accessToken>`.
- WebSocket query-token fallback is allowed only for `/api/v1/quotes/ws`.

## Early Checks

Run before handing the module to another team:

```powershell
npm run validate
npm run smoke
docker compose config
```

When external npm dependencies are introduced, add a lockfile and run:

```powershell
npm audit --audit-level=moderate
```

For Docker images, run an image scanner in CI when available. Suitable tools include Trivy, Docker Scout, Snyk or a university-approved equivalent.

## Failure Isolation

The API mock is independent from Ktor, PostgreSQL and Redis. If those services are down, the mock/docs service remains available for mobile integration work.
