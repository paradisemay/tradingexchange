import { http } from "./http";
import type { PortfolioResponse } from "../types/api";

export const portfolioApi = {
  async getPortfolio(): Promise<PortfolioResponse> {
    const response = await http.get<PortfolioResponse>("/api/v1/portfolio");
    return response.data;
  }
};
