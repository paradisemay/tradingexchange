import { createHash, randomUUID } from 'node:crypto';
import { createReadStream, existsSync, statSync } from 'node:fs';
import { readFile } from 'node:fs/promises';
import { createServer } from 'node:http';
import { extname, join, normalize, resolve } from 'node:path';
import { fileURLToPath, URL } from 'node:url';

const PORT = Number(process.env.API_MOCK_PORT || 8081);
const QUOTE_INTERVAL_MS = Number(process.env.QUOTE_INTERVAL_MS || 1000);
const MOCK_JWT = process.env.MOCK_JWT || 'mock-access-token';
const MOCK_REFRESH_TOKEN = process.env.MOCK_REFRESH_TOKEN || 'mock-refresh-token';
const MOCK_REFRESH_TOKEN_ROTATED = process.env.MOCK_REFRESH_TOKEN_ROTATED || 'mock-refresh-token-rotated';
const ROOT = fileURLToPath(new URL('..', import.meta.url));
const DOCS_DIR = join(ROOT, 'docs');

const nowIso = () => new Date().toISOString();
const jsonHeaders = {
  'content-type': 'application/json; charset=utf-8',
  'access-control-allow-origin': '*',
  'access-control-allow-headers': 'authorization,content-type',
  'access-control-allow-methods': 'GET,POST,OPTIONS'
};

const instruments = [
  { ticker: 'SBER', name: 'Sberbank', currency: 'RUB', lotSize: 1, isActive: true, lastPrice: '252.0000' },
  { ticker: 'GAZP', name: 'Gazprom', currency: 'RUB', lotSize: 1, isActive: true, lastPrice: '164.5000' },
  { ticker: 'YNDX', name: 'Yandex', currency: 'RUB', lotSize: 1, isActive: true, lastPrice: '4200.0000' },
  { ticker: 'LKOH', name: 'Lukoil', currency: 'RUB', lotSize: 1, isActive: true, lastPrice: '7350.0000' },
  { ticker: 'GMKN', name: 'Norilsk Nickel', currency: 'RUB', lotSize: 1, isActive: true, lastPrice: '156.4000' }
];

const prices = new Map(instruments.map((item) => [item.ticker, Number(item.lastPrice)]));
const orders = [
  {
    orderId: 'f84cdb05-2833-4c72-b1ee-bb5c964abf8f',
    ticker: 'SBER',
    side: 'BUY',
    orderType: 'MARKET',
    status: 'FILLED',
    quantity: '10',
    executedPrice: '252.0000',
    createdAt: '2026-04-11T12:30:00.000Z'
  }
];
const transactions = [
  {
    id: '4fd67edb-9ac1-4d66-9f3d-39b0e9c0d9be',
    type: 'BUY',
    ticker: 'SBER',
    amount: '2520.0000',
    quantity: '10',
    createdAt: '2026-04-11T12:30:01.000Z'
  }
];

const server = createServer(async (req, res) => {
  try {
    await handleRequest(req, res);
  } catch (error) {
    console.error(error);
    sendJson(res, 500, appError('INTERNAL_ERROR', 'An unexpected error occurred'));
  }
});

const wsClients = new Set();

server.on('upgrade', (req, socket) => {
  const url = new URL(req.url || '/', `http://${req.headers.host || 'localhost'}`);
  if (url.pathname !== '/api/v1/quotes/ws') {
    socket.write('HTTP/1.1 404 Not Found\r\n\r\n');
    socket.destroy();
    return;
  }
  if (!isWsAuthorized(req, url)) {
    socket.write('HTTP/1.1 401 Unauthorized\r\nContent-Length: 0\r\n\r\n');
    socket.destroy();
    return;
  }
  acceptWebSocket(req, socket);
});

setInterval(() => {
  for (const client of wsClients) {
    for (const ticker of client.subscriptions) {
      sendWsText(client.socket, JSON.stringify(nextQuote(ticker)));
    }
  }
}, QUOTE_INTERVAL_MS).unref();

server.listen(PORT, () => {
  console.log(`API mock server listening on http://localhost:${PORT}`);
});

async function handleRequest(req, res) {
  if (req.method === 'OPTIONS') {
    res.writeHead(204, jsonHeaders);
    res.end();
    return;
  }

  const url = new URL(req.url || '/', `http://${req.headers.host || 'localhost'}`);
  if (url.pathname === '/health') return sendJson(res, 200, { status: 'UP', module: 'api-mock' });
  if (url.pathname === '/docs') return sendHtml(res, docsIndex());
  if (url.pathname === '/docs/swagger') return sendHtml(res, swaggerUi());
  if (url.pathname === '/docs/openapi.yaml') return serveDocsFile('/docs/api/openapi.yaml', res);
  if (url.pathname === '/docs/asyncapi.yaml') return serveDocsFile('/docs/api/asyncapi.yaml', res);
  if (url.pathname.startsWith('/docs/')) return serveDocsFile(url.pathname, res);

  if (url.pathname === '/health/live' && req.method === 'GET') {
    return sendJson(res, 200, { status: 'UP' });
  }
  if (url.pathname === '/health/ready' && req.method === 'GET') {
    return sendJson(res, 200, { status: 'UP', db: 'UP', redis: 'UP' });
  }

  if (url.pathname.startsWith('/api/v1/auth/')) {
    return handleAuth(req, res, url);
  }

  if (!isRestAuthorized(req)) {
    return sendJson(res, 401, { errorCode: 'UNAUTHORIZED', message: 'Token is missing or invalid' });
  }

  if (url.pathname === '/api/v1/me' && req.method === 'GET') {
    return sendJson(res, 200, {
      userId: '8e2f8d1a-1d50-4fb3-b3ea-2f88d7cbb2b1',
      email: 'trader@example.com',
      fullName: 'Ivan Ivanov',
      role: 'CLIENT'
    });
  }
  if (url.pathname === '/api/v1/portfolio' && req.method === 'GET') {
    return sendJson(res, 200, {
      positions: [
        { ticker: 'SBER', quantity: '10', avgPrice: '250.1200', currentPrice: '252.0000', currency: 'RUB' },
        { ticker: 'GAZP', quantity: '5', avgPrice: '160.0000', currentPrice: '164.5000', currency: 'RUB' }
      ],
      cash: { currency: 'RUB', available: '100000.0000' }
    });
  }
  if (url.pathname === '/api/v1/instruments' && req.method === 'GET') {
    const query = (url.searchParams.get('query') || '').toUpperCase();
    const result = query
      ? instruments.filter((item) => item.ticker.includes(query) || item.name.toUpperCase().includes(query))
      : instruments;
    return sendJson(res, 200, result);
  }
  if (url.pathname === '/api/v1/orders' && req.method === 'POST') {
    return handleCreateOrder(req, res);
  }
  if (url.pathname === '/api/v1/orders' && req.method === 'GET') {
    return sendJson(res, 200, paginateByCreatedAt(orders, url, 'orders'));
  }
  if (url.pathname === '/api/v1/transactions' && req.method === 'GET') {
    return sendJson(res, 200, paginateByCreatedAt(transactions, url, 'transactions'));
  }

  sendJson(res, 404, appError('VALIDATION_ERROR', `No mock route for ${req.method} ${url.pathname}`));
}

async function handleAuth(req, res, url) {
  if (url.pathname === '/api/v1/auth/register' && req.method === 'POST') {
    const body = await readJson(req);
    if (!body.email || !String(body.email).includes('@')) {
      return sendJson(res, 400, appError('VALIDATION_ERROR', 'Invalid email format'));
    }
    if (!body.password || String(body.password).length < 8) {
      return sendJson(res, 400, appError('VALIDATION_ERROR', 'Password must be at least 8 characters'));
    }
    return sendJson(res, 201, {
      userId: '8e2f8d1a-1d50-4fb3-b3ea-2f88d7cbb2b1',
      accessToken: MOCK_JWT,
      refreshToken: MOCK_REFRESH_TOKEN
    });
  }
  if (url.pathname === '/api/v1/auth/login' && req.method === 'POST') {
    return sendJson(res, 200, { accessToken: MOCK_JWT, refreshToken: MOCK_REFRESH_TOKEN });
  }
  if (url.pathname === '/api/v1/auth/refresh' && req.method === 'POST') {
    return sendJson(res, 200, { accessToken: MOCK_JWT, refreshToken: MOCK_REFRESH_TOKEN_ROTATED });
  }
  if (url.pathname === '/api/v1/auth/logout' && req.method === 'POST') {
    if (!isRestAuthorized(req)) {
      return sendJson(res, 401, { errorCode: 'UNAUTHORIZED', message: 'Token is missing or invalid' });
    }
    res.writeHead(204, jsonHeaders);
    res.end();
    return;
  }
  sendJson(res, 404, appError('VALIDATION_ERROR', `No auth mock route for ${req.method} ${url.pathname}`));
}

async function handleCreateOrder(req, res) {
  const body = await readJson(req);
  const ticker = String(body.ticker || '').toUpperCase();
  const side = String(body.side || '');
  const orderType = String(body.orderType || '');
  const quantity = String(body.quantity || '');
  const instrument = instruments.find((item) => item.ticker === ticker);

  if (!ticker || !side || !orderType || !quantity) {
    return sendJson(res, 400, appError('VALIDATION_ERROR', 'ticker, side, orderType and quantity are required'));
  }
  if (!instrument) return sendJson(res, 404, appError('INSTRUMENT_NOT_FOUND', `Instrument ${ticker} not found`));
  if (!['BUY', 'SELL'].includes(side)) return sendJson(res, 400, appError('VALIDATION_ERROR', 'side must be BUY or SELL'));
  if (!['MARKET', 'LIMIT'].includes(orderType)) return sendJson(res, 400, appError('VALIDATION_ERROR', 'orderType must be MARKET or LIMIT'));
  if (!(Number(quantity) > 0)) return sendJson(res, 400, appError('VALIDATION_ERROR', 'quantity must be positive'));
  if (Number(quantity) > 100000) {
    return sendJson(res, 422, appError('INSUFFICIENT_FUNDS', 'Insufficient funds', { required: '999999.0000', available: '100000.0000' }));
  }

  const executedPrice = orderType === 'LIMIT' && body.limitPrice ? String(body.limitPrice) : instrument.lastPrice;
  const order = {
    orderId: randomUUID(),
    ticker,
    side,
    orderType,
    status: 'FILLED',
    quantity,
    executedPrice,
    createdAt: nowIso()
  };
  orders.unshift(order);
  transactions.unshift({
    id: randomUUID(),
    type: side,
    ticker,
    amount: (Number(executedPrice) * Number(quantity)).toFixed(4),
    quantity,
    createdAt: nowIso()
  });
  sendJson(res, 201, order);
}

function paginateByCreatedAt(items, url, field) {
  const limit = Math.min(Math.max(Number(url.searchParams.get('limit') || 50), 1), 100);
  const cursor = url.searchParams.get('cursor');
  const filtered = cursor ? items.filter((item) => Date.parse(item.createdAt) < Date.parse(cursor)) : items;
  const page = filtered.slice(0, limit);
  return {
    [field]: page,
    nextCursor: page.length === limit ? page[page.length - 1].createdAt : null
  };
}

function isRestAuthorized(req) {
  const header = req.headers.authorization || '';
  return header === `Bearer ${MOCK_JWT}` || header.startsWith('Bearer ');
}

function isWsAuthorized(req, url) {
  const header = req.headers.authorization || '';
  return header === `Bearer ${MOCK_JWT}` || header.startsWith('Bearer ') || Boolean(url.searchParams.get('accessToken'));
}

function appError(errorCode, message, details = {}) {
  return { errorCode, message, details, traceId: '' };
}

async function readJson(req) {
  const chunks = [];
  for await (const chunk of req) chunks.push(chunk);
  if (chunks.length === 0) return {};
  const text = Buffer.concat(chunks).toString('utf8');
  return text ? JSON.parse(text) : {};
}

function sendJson(res, statusCode, payload) {
  res.writeHead(statusCode, jsonHeaders);
  res.end(JSON.stringify(payload, null, 2));
}

function sendHtml(res, html) {
  res.writeHead(200, { ...jsonHeaders, 'content-type': 'text/html; charset=utf-8' });
  res.end(html);
}

function serveDocsFile(pathname, res) {
  const relative = pathname.replace(/^\/docs\/?/, '');
  const filePath = normalize(join(DOCS_DIR, relative));
  if (!filePath.startsWith(DOCS_DIR) || !existsSync(filePath) || !statSync(filePath).isFile()) {
    sendJson(res, 404, appError('VALIDATION_ERROR', 'Documentation file not found'));
    return;
  }
  const type = contentType(filePath);
  res.writeHead(200, { ...jsonHeaders, 'content-type': type });
  createReadStream(filePath).pipe(res);
}

function contentType(filePath) {
  const ext = extname(filePath);
  if (ext === '.yaml' || ext === '.yml') return 'application/yaml; charset=utf-8';
  if (ext === '.json') return 'application/json; charset=utf-8';
  if (ext === '.md') return 'text/markdown; charset=utf-8';
  if (ext === '.html') return 'text/html; charset=utf-8';
  return 'text/plain; charset=utf-8';
}

function docsIndex() {
  return `<!doctype html>
<html lang="en">
<head><meta charset="utf-8"><title>TradingExchange API</title><style>
body{font-family:system-ui,sans-serif;max-width:960px;margin:40px auto;padding:0 20px;line-height:1.5}
a{color:#0f766e} code{background:#f1f5f9;padding:2px 5px;border-radius:4px}
li{margin:8px 0}
</style></head>
<body>
<h1>TradingExchange API Spec+Mock</h1>
<p>This service exposes the current Ktor-compatible contract, documentation files, REST mock endpoints and a WebSocket quotes mock.</p>
<ul>
<li><a href="/docs/swagger">Swagger UI</a></li>
<li><a href="/docs/openapi.yaml">OpenAPI 3.1</a></li>
<li><a href="/docs/asyncapi.yaml">AsyncAPI WebSocket contract</a></li>
<li><a href="/docs/wiki/modules/api/README.md">API wiki</a></li>
<li><a href="/health">Mock health</a></li>
</ul>
<p>Use <code>Authorization: Bearer ${MOCK_JWT}</code> for protected REST requests.</p>
</body></html>`;
}

function swaggerUi() {
  return `<!doctype html>
<html lang="en">
<head><meta charset="utf-8"><title>Swagger UI</title><link rel="stylesheet" href="https://unpkg.com/swagger-ui-dist@5/swagger-ui.css"></head>
<body><div id="swagger-ui"></div><script src="https://unpkg.com/swagger-ui-dist@5/swagger-ui-bundle.js"></script>
<script>window.ui=SwaggerUIBundle({url:'/docs/openapi.yaml',dom_id:'#swagger-ui'});</script></body></html>`;
}

function acceptWebSocket(req, socket) {
  const key = req.headers['sec-websocket-key'];
  const accept = createHash('sha1')
    .update(`${key}258EAFA5-E914-47DA-95CA-C5AB0DC85B11`)
    .digest('base64');
  socket.write([
    'HTTP/1.1 101 Switching Protocols',
    'Upgrade: websocket',
    'Connection: Upgrade',
    `Sec-WebSocket-Accept: ${accept}`,
    '\r\n'
  ].join('\r\n'));

  const client = { socket, subscriptions: new Set() };
  wsClients.add(client);
  socket.on('data', (buffer) => handleWsData(client, buffer));
  socket.on('close', () => wsClients.delete(client));
  socket.on('error', () => wsClients.delete(client));
}

function handleWsData(client, buffer) {
  const frame = decodeWsFrame(buffer);
  if (!frame) return;
  if (frame.opcode === 8) {
    client.socket.end();
    wsClients.delete(client);
    return;
  }
  if (frame.opcode === 9) {
    sendWsFrame(client.socket, Buffer.from(frame.payload), 10);
    return;
  }
  if (frame.opcode !== 1) return;

  try {
    const message = JSON.parse(frame.payload);
    const tickers = Array.isArray(message.tickers) ? message.tickers.map((item) => String(item).toUpperCase()) : [];
    if (message.type === 'subscribe') {
      for (const ticker of tickers) client.subscriptions.add(ticker);
    }
    if (message.type === 'unsubscribe') {
      for (const ticker of tickers) client.subscriptions.delete(ticker);
    }
  } catch {
    sendWsText(client.socket, JSON.stringify(appError('VALIDATION_ERROR', 'Invalid WebSocket JSON frame')));
  }
}

function decodeWsFrame(buffer) {
  if (buffer.length < 2) return null;
  const opcode = buffer[0] & 0x0f;
  const masked = (buffer[1] & 0x80) === 0x80;
  let length = buffer[1] & 0x7f;
  let offset = 2;
  if (length === 126) {
    length = buffer.readUInt16BE(offset);
    offset += 2;
  } else if (length === 127) {
    length = Number(buffer.readBigUInt64BE(offset));
    offset += 8;
  }
  let mask;
  if (masked) {
    mask = buffer.subarray(offset, offset + 4);
    offset += 4;
  }
  const data = Buffer.from(buffer.subarray(offset, offset + length));
  if (masked && mask) {
    for (let i = 0; i < data.length; i += 1) data[i] ^= mask[i % 4];
  }
  return { opcode, payload: data.toString('utf8') };
}

function sendWsText(socket, text) {
  sendWsFrame(socket, Buffer.from(text, 'utf8'), 1);
}

function sendWsFrame(socket, payload, opcode) {
  const length = payload.length;
  let header;
  if (length < 126) {
    header = Buffer.from([0x80 | opcode, length]);
  } else if (length < 65536) {
    header = Buffer.alloc(4);
    header[0] = 0x80 | opcode;
    header[1] = 126;
    header.writeUInt16BE(length, 2);
  } else {
    header = Buffer.alloc(10);
    header[0] = 0x80 | opcode;
    header[1] = 127;
    header.writeBigUInt64BE(BigInt(length), 2);
  }
  socket.write(Buffer.concat([header, payload]));
}

function nextQuote(ticker) {
  const current = prices.get(ticker) || 100;
  const updated = Math.max(1, current + (Math.random() - 0.5) * 2);
  prices.set(ticker, updated);
  return {
    type: 'quote',
    ticker,
    price: updated.toFixed(4),
    currency: 'RUB',
    timestampMs: Date.now()
  };
}
