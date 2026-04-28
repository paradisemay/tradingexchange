import { http } from "./http";
import type { TransactionListResponse } from "../types/api";

export const transactionsApi = {
  async list(limit = 50, cursor: string | null = null): Promise<TransactionListResponse> {
    const response = await http.get<TransactionListResponse>("/api/v1/transactions", {
      params: { limit, cursor: cursor ?? undefined }
    });
    return response.data;
  }
};
