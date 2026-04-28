import { http } from "./http";
import type { CreateOrderRequest, OrderListResponse, OrderResponse } from "../types/api";

export const ordersApi = {
  async create(payload: CreateOrderRequest): Promise<OrderResponse> {
    const response = await http.post<OrderResponse>("/api/v1/orders", payload);
    return response.data;
  },
  async list(limit = 50, cursor: string | null = null): Promise<OrderListResponse> {
    const response = await http.get<OrderListResponse>("/api/v1/orders", {
      params: { limit, cursor: cursor ?? undefined }
    });
    return response.data;
  }
};
