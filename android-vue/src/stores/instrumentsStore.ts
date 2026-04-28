import { defineStore } from "pinia";
import { ref } from "vue";
import { instrumentsApi } from "../api/instrumentsApi";
import type { Instrument } from "../types/api";
import { mapApiError } from "./errors";
import { offlineCache } from "../utils/indexedDb";

export const useInstrumentsStore = defineStore("instrumentsStore", () => {
  const items = ref<Instrument[]>([]);
  const loading = ref(false);
  const error = ref<string | null>(null);
  const isOffline = ref(false);

  const search = async (query: string): Promise<void> => {
    loading.value = true;
    error.value = null;
    try {
      const result = await instrumentsApi.search(query);
      items.value = result;
      isOffline.value = false;
      await offlineCache.setInstruments(query, result);
    } catch (e) {
      error.value = mapApiError(e);
      const cached = await offlineCache.getInstruments(query);
      if (cached) {
        items.value = cached;
        isOffline.value = true;
      }
    } finally {
      loading.value = false;
    }
  };

  return { items, loading, error, isOffline, search };
});
