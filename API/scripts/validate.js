import { readdir, readFile } from 'node:fs/promises';
import { join } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = fileURLToPath(new URL('..', import.meta.url));
const requiredFiles = [
  'plan.md',
  'docs/api/openapi.yaml',
  'docs/api/asyncapi.yaml',
  'docs/wiki/modules/api/README.md',
  'docs/wiki/modules/api/contracts.md',
  'docs/wiki/modules/api/runbook.md',
  'docs/wiki/modules/api/mock-server.md',
  'docs/wiki/modules/api/security.md',
  'docs/wiki/modules/api/adr/README.md',
  'docs/wiki/modules/api/adr/ADR-0001-spec-and-mock-module.md',
  'docs/wiki/modules/api/adr/ADR-0002-openapi-and-asyncapi-split.md',
  'docs/wiki/modules/api/adr/ADR-0003-environment-configuration.md',
  'docs/wiki/modules/api/adr/ADR-0004-chart-history-contract.md'
];

const checks = [];

for (const file of requiredFiles) {
  const content = await readFile(join(root, file), 'utf8');
  checks.push([file, content.length > 0]);
}

const openapi = await readFile(join(root, 'docs/api/openapi.yaml'), 'utf8');
const asyncapi = await readFile(join(root, 'docs/api/asyncapi.yaml'), 'utf8');
const envExample = await readFile(join(root, '.env.example'), 'utf8');

checks.push(['OpenAPI version is 3.1.0', openapi.includes('openapi: 3.1.0')]);
checks.push(['OpenAPI contains auth register', openapi.includes('/api/v1/auth/register:')]);
checks.push(['OpenAPI contains current Ktor order list shape', openapi.includes('OrderListResponse') && openapi.includes('orders:')]);
checks.push(['OpenAPI contains current Ktor transaction id field', openapi.includes('TransactionResponse') && openapi.includes('id: { type: string, format: uuid }')]);
checks.push(['OpenAPI contains current Ktor 422/503 errors', openapi.includes('"422":') && openapi.includes('"503":')]);
checks.push(['OpenAPI contains chart history endpoints', openapi.includes('/api/v1/instruments/{ticker}/chart/line:') && openapi.includes('/api/v1/instruments/{ticker}/chart/candles:')]);
checks.push(['OpenAPI contains line and candle schemas', openapi.includes('LineChartResponse') && openapi.includes('CandleChartResponse')]);
checks.push(['AsyncAPI version is 2.6.0', asyncapi.includes('asyncapi: 2.6.0')]);
checks.push(['AsyncAPI contains quotes WebSocket channel', asyncapi.includes('/api/v1/quotes/ws:')]);
checks.push(['AsyncAPI contains subscribe/unsubscribe/quote', asyncapi.includes('SubscribeCommand') && asyncapi.includes('UnsubscribeCommand') && asyncapi.includes('QuoteEvent')]);
checks.push(['Env example contains mock runtime vars', ['API_MOCK_PORT', 'QUOTE_INTERVAL_MS', 'MOCK_JWT', 'MOCK_REFRESH_TOKEN', 'MOCK_REFRESH_TOKEN_ROTATED'].every((name) => envExample.includes(name))]);

const examplesDir = join(root, 'docs/api/examples');
const exampleFiles = (await readdir(examplesDir)).filter((name) => name.endsWith('.json'));
checks.push(['At least 15 JSON examples exist', exampleFiles.length >= 15]);

for (const file of exampleFiles) {
  const content = await readFile(join(examplesDir, file), 'utf8');
  JSON.parse(content);
}
checks.push(['All JSON examples are valid JSON', true]);

const failed = checks.filter(([, ok]) => !ok);
for (const [name, ok] of checks) {
  console.log(`${ok ? 'OK' : 'FAIL'} ${name}`);
}

if (failed.length > 0) {
  console.error(`Validation failed: ${failed.map(([name]) => name).join(', ')}`);
  process.exit(1);
}

console.log('Validation passed.');
