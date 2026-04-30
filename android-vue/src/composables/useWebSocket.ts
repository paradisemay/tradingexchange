import { ref } from "vue";
import { tokenStorage } from "../utils/preferences";
import type { WsIncoming, WsQuoteEvent, WsSubscribeCommand, WsUnsubscribeCommand } from "../types/api";
import { useQuotesStore } from "../stores/quotesStore";

const baseHttp = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";
const baseWs = baseHttp.startsWith("https://") ? baseHttp.replace("https://", "wss://") : baseHttp.replace("http://", "ws://");

export const useWebSocket = () => {
  const quotesStore = useQuotesStore();
  const socket = ref<WebSocket | null>(null);
  const subscriptions = ref<Set<string>>(new Set());
  const reconnectAttempt = ref(0);
  let pingIntervalId: number | null = null;

  const setState = (state: "CONNECTING" | "OPEN" | "CLOSED" | "RECONNECTING"): void => {
    quotesStore.wsState = state;
  };

  const send = (payload: WsSubscribeCommand | WsUnsubscribeCommand): void => {
    if (socket.value?.readyState === WebSocket.OPEN) {
      socket.value.send(JSON.stringify(payload));
    }
  };

  const clearPing = (): void => {
    if (pingIntervalId !== null) {
      window.clearInterval(pingIntervalId);
      pingIntervalId = null;
    }
  };

  const scheduleReconnect = (): void => {
    reconnectAttempt.value += 1;
    const backoff = [1000, 2000, 5000, 10000, 30000];
    const delay = backoff[Math.min(reconnectAttempt.value - 1, backoff.length - 1)];
    setState("RECONNECTING");
    window.setTimeout(() => {
      void connect();
    }, delay);
  };

  const connect = async (): Promise<void> => {
    const token = await tokenStorage.getAccessToken();
    if (!token) {
      return;
    }
    setState("CONNECTING");
    socket.value = new WebSocket(`${baseWs}/api/v1/quotes/ws?accessToken=${encodeURIComponent(token)}`);

    socket.value.onopen = () => {
      reconnectAttempt.value = 0;
      setState("OPEN");
      subscriptions.value.forEach((ticker) => {
        send({ type: "subscribe", tickers: [ticker] });
      });
      pingIntervalId = window.setInterval(() => {
        if (socket.value?.readyState === WebSocket.OPEN) {
          socket.value.send(JSON.stringify({ type: "ping" }));
        }
      }, 30000);
    };

    socket.value.onmessage = (event) => {
      const payload = JSON.parse(String(event.data)) as WsIncoming;
      if (payload.type === "quote") {
        void quotesStore.applyQuote(payload as WsQuoteEvent);
      }
    };

    socket.value.onclose = () => {
      clearPing();
      setState("CLOSED");
      scheduleReconnect();
    };

    socket.value.onerror = () => {
      socket.value?.close();
    };
  };

  const subscribe = (tickers: string[]): void => {
    tickers.forEach((ticker) => subscriptions.value.add(ticker));
    send({ type: "subscribe", tickers });
  };

  const unsubscribe = (tickers: string[]): void => {
    tickers.forEach((ticker) => subscriptions.value.delete(ticker));
    send({ type: "unsubscribe", tickers });
  };

  const disconnect = (): void => {
    clearPing();
    socket.value?.close();
    socket.value = null;
    setState("CLOSED");
  };

  return { connect, disconnect, subscribe, unsubscribe, wsState: quotesStore.wsState };
};
