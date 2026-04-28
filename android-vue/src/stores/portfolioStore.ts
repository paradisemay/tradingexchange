import { defineStore } from "pinia";
import { computed, ref } from "vue";
import { portfolioApi } from "../api/portfolioApi";
import type { PortfolioResponse } from "../types/api";
import { mapApiError } from "./errors";
import { offlineCache } from "../utils/indexedDb";
import { formatMoney } from "../utils/money";
import { useQuotesStore } from "./quotesStore";

export const usePortfolioStore = defineStore("portfolioStore", () => {
  const data = ref<PortfolioResponse | null>(null);
  const loading = ref(false);
  const error = ref<string | null>(null);
  const isOffline = ref(false);

  const quotes = useQuotesStore();

  const loadPortfolio = async (): Promise<void> => {
    loading.value = true;
    error.value = null;
    try {
      const response = await portfolioApi.getPortfolio();
      data.value = response;
      isOffline.value = false;
      await offlineCache.setPortfolio(response);
    } catch (e) {
      error.value = mapApiError(e);
      const cached = await offlineCache.getPortfolio();
      if (cached) {
        data.value = cached;
        isOffline.value = true;
      }
    } finally {
      loading.value = false;
    }
  };

  const enrichedPositions = computed(() =>
    data.value?.positions.map((position) => {
      const live = quotes.byTicker[position.ticker];
      const currentPrice = live?.price ?? position.currentPrice;
      return {
        ...position,
        currentPrice,
        currentPriceFormatted: formatMoney(currentPrice, position.currency),
        avgPriceFormatted: formatMoney(position.avgPrice, position.currency)
      };
    }) ?? []
  );

  return { data, loading, error, isOffline, loadPortfolio, enrichedPositions };
});
