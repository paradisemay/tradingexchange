import { createHash, randomBytes } from 'node:crypto';
import http from 'node:http';
import net from 'node:net';

const baseUrl = process.env.API_BASE_URL || 'http://localhost:8081';
const wsUrl = process.env.API_WS_URL || baseUrl.replace(/^http/, 'ws') + '/api/v1/quotes/ws?accessToken=mock-access-token';
const token = process.env.MOCK_JWT || 'mock-access-token';

const authHeaders = {
  authorization: `Bearer ${token}`,
  'content-type': 'application/json'
};

await step('health', () => request('GET', '/health'));
await step('health/live', () => request('GET', '/health/live'));
await step('health/ready', () => request('GET', '/health/ready'));

const register = await step('auth/register', () => request('POST', '/api/v1/auth/register', {
  email: 'trader@example.com',
  password: 'secret123',
  fullName: 'Ivan Ivanov'
}));
assert(register.accessToken, 'register returns accessToken');

const login = await step('auth/login', () => request('POST', '/api/v1/auth/login', {
  email: 'trader@example.com',
  password: 'secret123'
}));
assert(login.refreshToken, 'login returns refreshToken');

await step('auth/refresh', () => request('POST', '/api/v1/auth/refresh', { refreshToken: login.refreshToken }));
await step('me', () => request('GET', '/api/v1/me', null, authHeaders));
await step('portfolio', () => request('GET', '/api/v1/portfolio', null, authHeaders));
await step('instruments', () => request('GET', '/api/v1/instruments?query=SBER', null, authHeaders));
await step('orders/create', () => request('POST', '/api/v1/orders', {
  ticker: 'SBER',
  side: 'BUY',
  orderType: 'MARKET',
  quantity: '1',
  limitPrice: null
}, authHeaders));
await step('orders/list', () => request('GET', '/api/v1/orders?limit=1', null, authHeaders));
await step('transactions/list', () => request('GET', '/api/v1/transactions?limit=1', null, authHeaders));
await step('auth/logout', () => request('POST', '/api/v1/auth/logout', { refreshToken: login.refreshToken }, authHeaders, 204));

const quote = await step('websocket', () => websocketSmoke(wsUrl));
assert(quote.type === 'quote' && quote.ticker === 'SBER', 'websocket receives SBER quote');

console.log('Smoke test passed.');

function request(method, path, body = null, headers = { 'content-type': 'application/json' }, expectedStatus = null) {
  const url = new URL(path, baseUrl);
  const payload = body ? JSON.stringify(body) : null;
  return new Promise((resolve, reject) => {
    const req = http.request(url, {
      method,
      headers: {
        ...headers,
        ...(payload ? { 'content-length': Buffer.byteLength(payload) } : {})
      }
    }, (res) => {
      const chunks = [];
      res.on('data', (chunk) => chunks.push(chunk));
      res.on('end', () => {
        const text = Buffer.concat(chunks).toString('utf8');
        const statusOk = expectedStatus ? res.statusCode === expectedStatus : res.statusCode >= 200 && res.statusCode < 300;
        if (!statusOk) {
          reject(new Error(`${method} ${path} returned ${res.statusCode}: ${text}`));
          return;
        }
        if (!text) {
          resolve(null);
          return;
        }
        try {
          resolve(JSON.parse(text));
        } catch {
          resolve(text);
        }
      });
    });
    req.on('error', reject);
    if (payload) req.write(payload);
    req.end();
  });
}

async function step(name, fn) {
  process.stdout.write(`SMOKE ${name}... `);
  const result = await fn();
  process.stdout.write('OK\n');
  return result;
}

function websocketSmoke(targetUrl) {
  const url = new URL(targetUrl);
  const key = randomBytes(16).toString('base64');
  const expectedAccept = createHash('sha1')
    .update(`${key}258EAFA5-E914-47DA-95CA-C5AB0DC85B11`)
    .digest('base64');

  return new Promise((resolve, reject) => {
    const socket = net.createConnection({ host: url.hostname, port: Number(url.port || 80) });
    let handshake = '';
    let upgraded = false;
    const timer = setTimeout(() => {
      socket.destroy();
      reject(new Error('WebSocket smoke timeout'));
    }, 5000);

    socket.on('connect', () => {
      socket.write([
        `GET ${url.pathname}${url.search} HTTP/1.1`,
        `Host: ${url.host}`,
        'Upgrade: websocket',
        'Connection: Upgrade',
        `Sec-WebSocket-Key: ${key}`,
        'Sec-WebSocket-Version: 13',
        '\r\n'
      ].join('\r\n'));
    });

    socket.on('data', (chunk) => {
      if (!upgraded) {
        handshake += chunk.toString('binary');
        if (!handshake.includes('\r\n\r\n')) return;
        if (!handshake.includes('101 Switching Protocols') || !handshake.includes(expectedAccept)) {
          clearTimeout(timer);
          reject(new Error(`Invalid WebSocket handshake: ${handshake}`));
          socket.destroy();
          return;
        }
        upgraded = true;
        socket.write(encodeClientFrame(JSON.stringify({ type: 'subscribe', tickers: ['SBER'] })));
        const remainder = chunk.subarray(Buffer.from(handshake.split('\r\n\r\n')[0] + '\r\n\r\n', 'binary').length);
        if (remainder.length === 0) return;
        chunk = remainder;
      }
      const text = decodeServerFrame(chunk);
      if (!text) return;
      const message = JSON.parse(text);
      if (message.type === 'quote') {
        socket.write(encodeClientFrame(JSON.stringify({ type: 'unsubscribe', tickers: ['SBER'] })));
        clearTimeout(timer);
        socket.destroy();
        resolve(message);
      }
    });

    socket.on('error', (error) => {
      clearTimeout(timer);
      reject(error);
    });
  });
}

function encodeClientFrame(text) {
  const payload = Buffer.from(text, 'utf8');
  const mask = randomBytes(4);
  const header = payload.length < 126
    ? Buffer.from([0x81, 0x80 | payload.length])
    : Buffer.from([0x81, 0xfe, payload.length >> 8, payload.length & 0xff]);
  const masked = Buffer.alloc(payload.length);
  for (let i = 0; i < payload.length; i += 1) masked[i] = payload[i] ^ mask[i % 4];
  return Buffer.concat([header, mask, masked]);
}

function decodeServerFrame(buffer) {
  if (buffer.length < 2) return null;
  let length = buffer[1] & 0x7f;
  let offset = 2;
  if (length === 126) {
    length = buffer.readUInt16BE(offset);
    offset += 2;
  } else if (length === 127) {
    length = Number(buffer.readBigUInt64BE(offset));
    offset += 8;
  }
  if (buffer.length < offset + length) return null;
  return buffer.subarray(offset, offset + length).toString('utf8');
}

function assert(condition, message) {
  if (!condition) throw new Error(message);
}
