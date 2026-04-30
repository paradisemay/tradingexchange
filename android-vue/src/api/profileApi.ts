import { http } from "./http";
import type { UserProfile } from "../types/api";

export const profileApi = {
  async getMe(): Promise<UserProfile> {
    const response = await http.get<UserProfile>("/api/v1/me");
    return response.data;
  }
};
