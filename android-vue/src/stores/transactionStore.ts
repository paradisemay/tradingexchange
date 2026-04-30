import { defineStore } from "pinia";
import { ref } from "vue";
import type { TransactionResponse } from "../types/api";
import { transactionsApi } from "../api/transactionsApi";
import { mapApiError } from "./errors";

export const useTransactionStore = defineStore("transactionStore", () => {
  const transactions = ref<TransactionResponse[]>([]);
  const nextCursor = ref<string | null>(null);
  const loading = ref(false);
  const error = ref<string | null>(null);

  const loadTransactions = async (loadMore = false): Promise<void> => {
    loading.value = true;
    error.value = null;
    try {
      const result = await transactionsApi.list(50, loadMore ? nextCursor.value : null);
      transactions.value = loadMore ? [...transactions.value, ...result.transactions] : result.transactions;
      nextCursor.value = result.nextCursor;
    } catch (e) {
      error.value = mapApiError(e);
    } finally {
      loading.value = false;
    }
  };

  return { transactions, nextCursor, loading, error, loadTransactions };
});
