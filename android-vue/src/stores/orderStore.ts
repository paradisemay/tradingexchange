import { defineStore } from "pinia";
import { ref } from "vue";
import type { CreateOrderRequest, OrderResponse } from "../types/api";
import { ordersApi } from "../api/ordersApi";
import { mapApiError } from "./errors";

export const useOrderStore = defineStore("orderStore", () => {
  const lastOrder = ref<OrderResponse | null>(null);
  const orders = ref<OrderResponse[]>([]);
  const nextCursor = ref<string | null>(null);
  const loading = ref(false);
  const error = ref<string | null>(null);

  const createOrder = async (payload: CreateOrderRequest): Promise<void> => {
    loading.value = true;
    error.value = null;
    try {
      lastOrder.value = await ordersApi.create(payload);
    } catch (e) {
      error.value = mapApiError(e);
      throw e;
    } finally {
      loading.value = false;
    }
  };

  const loadOrders = async (loadMore = false): Promise<void> => {
    loading.value = true;
    error.value = null;
    try {
      const result = await ordersApi.list(50, loadMore ? nextCursor.value : null);
      orders.value = loadMore ? [...orders.value, ...result.orders] : result.orders;
      nextCursor.value = result.nextCursor;
    } catch (e) {
      error.value = mapApiError(e);
    } finally {
      loading.value = false;
    }
  };

  return { lastOrder, orders, nextCursor, loading, error, createOrder, loadOrders };
});
