import { defineStore } from "pinia";
import { ref } from "vue";
import type { WsQuoteEvent } from "../types/api";
import { offlineCache } from "../utils/indexedDb";

export const useQuotesStore = defineStore("quotesStore", () => {
  const byTicker = ref<Record<string, WsQuoteEvent>>({});
  const wsState = ref<"CONNECTING" | "OPEN" | "CLOSED" | "RECONNECTING">("CLOSED");

  const applyQuote = async (quote: WsQuoteEvent): Promise<void> => {
    byTicker.value[quote.ticker] = quote;
    await offlineCache.setQuote(quote);
  };

  return { byTicker, wsState, applyQuote };
});
