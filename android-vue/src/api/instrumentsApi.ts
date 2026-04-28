import { http } from "./http";
import type { Instrument } from "../types/api";

export const instrumentsApi = {
  async search(query: string): Promise<Instrument[]> {
    const response = await http.get<Instrument[]>("/api/v1/instruments", { params: { query } });
    return response.data;
  }
};
