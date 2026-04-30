import { http } from "./http";
import type {
  LoginRequest,
  LogoutRequest,
  RefreshRequest,
  RegisterRequest,
  RegisterResponse,
  TokenPair
} from "../types/api";

export const authApi = {
  async register(payload: RegisterRequest): Promise<RegisterResponse> {
    const response = await http.post<RegisterResponse>("/api/v1/auth/register", payload);
    return response.data;
  },
  async login(payload: LoginRequest): Promise<TokenPair> {
    const response = await http.post<TokenPair>("/api/v1/auth/login", payload);
    return response.data;
  },
  async refresh(payload: RefreshRequest): Promise<TokenPair> {
    const response = await http.post<TokenPair>("/api/v1/auth/refresh", payload);
    return response.data;
  },
  async logout(payload: LogoutRequest): Promise<void> {
    await http.post("/api/v1/auth/logout", payload);
  }
};
