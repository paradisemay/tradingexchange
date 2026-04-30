export type ErrorCode =
  | "VALIDATION_ERROR"
  | "UNAUTHORIZED"
  | "FORBIDDEN"
  | "INSTRUMENT_NOT_FOUND"
  | "INSUFFICIENT_FUNDS"
  | "INSUFFICIENT_POSITION"
  | "QUOTE_UNAVAILABLE"
  | "CONFLICT"
  | "INTERNAL_ERROR";

export interface ApiErrorResponse {
  errorCode: ErrorCode;
  message: string;
  details: Record<string, string>;
  traceId: string;
}

export interface AuthErrorResponse {
  errorCode: "UNAUTHORIZED";
  message: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  fullName: string | null;
}
export interface LoginRequest {
  email: string;
  password: string;
}
export interface RefreshRequest {
  refreshToken: string;
}
export interface LogoutRequest {
  refreshToken: string;
}
export interface TokenPair {
  accessToken: string;
  refreshToken: string;
}
export interface RegisterResponse extends TokenPair {
  userId: string;
}

export interface UserProfile {
  userId: string;
  email: string;
  fullName: string | null;
  role: "CLIENT" | "ADMIN";
}

export interface PortfolioPosition {
  ticker: string;
  quantity: string;
  avgPrice: string;
  currentPrice: string | null;
  currency: string;
}
export interface Cash {
  currency: string;
  available: string;
}
export interface PortfolioResponse {
  positions: PortfolioPosition[];
  cash: Cash;
}

export interface Instrument {
  ticker: string;
  name: string;
  currency: string;
  lotSize: number;
  isActive: boolean;
  lastPrice: string | null;
}

export interface CreateOrderRequest {
  ticker: string;
  side: "BUY" | "SELL";
  orderType: "MARKET" | "LIMIT";
  quantity: string;
  limitPrice: string | null;
}
export interface OrderResponse {
  orderId: string;
  ticker: string;
  side: "BUY" | "SELL";
  orderType: "MARKET" | "LIMIT";
  status: "NEW" | "FILLED" | "CANCELLED" | "REJECTED";
  quantity: string;
  executedPrice: string | null;
  createdAt: string;
}
export interface OrderListResponse {
  orders: OrderResponse[];
  nextCursor: string | null;
}

export interface TransactionResponse {
  id: string;
  type: "DEPOSIT" | "WITHDRAW" | "BUY" | "SELL" | "FEE";
  ticker: string | null;
  amount: string;
  quantity: string | null;
  createdAt: string;
}
export interface TransactionListResponse {
  transactions: TransactionResponse[];
  nextCursor: string | null;
}

export interface WsSubscribeCommand {
  type: "subscribe";
  tickers: string[];
}
export interface WsUnsubscribeCommand {
  type: "unsubscribe";
  tickers: string[];
}
export interface WsQuoteEvent {
  type: "quote";
  ticker: string;
  price: string;
  currency: string;
  timestampMs: number;
}
export interface WsErrorEvent {
  type: "error";
  errorCode: string;
  message: string;
  traceId: string;
  details: Record<string, string>;
}

export type WsIncoming = WsQuoteEvent | WsErrorEvent;
