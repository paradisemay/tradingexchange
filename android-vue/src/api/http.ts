import axios, { AxiosError, type AxiosRequestConfig } from "axios";
import type { ApiErrorResponse, TokenPair } from "../types/api";
import { tokenStorage } from "../utils/preferences";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

type RefreshHandler = (refreshToken: string) => Promise<TokenPair>;
type LogoutHandler = () => Promise<void>;

let refreshHandler: RefreshHandler | null = null;
let logoutHandler: LogoutHandler | null = null;
let isRefreshing = false;
let refreshQueue: Array<(token: string | null) => void> = [];

export const http = axios.create({
  baseURL: API_BASE_URL,
  headers: { "Content-Type": "application/json" }
});

const processQueue = (token: string | null): void => {
  refreshQueue.forEach((resolve) => resolve(token));
  refreshQueue = [];
};

export const configureAuthInterceptors = (handlers: {
  onRefresh: RefreshHandler;
  onLogout: LogoutHandler;
}): void => {
  refreshHandler = handlers.onRefresh;
  logoutHandler = handlers.onLogout;
};

http.interceptors.request.use(async (config) => {
  const token = await tokenStorage.getAccessToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

http.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ApiErrorResponse>) => {
    const originalRequest = error.config as AxiosRequestConfig & { _retry?: boolean };
    const requestUrl = originalRequest?.url ?? "";
    const isRefreshCall = requestUrl.includes("/api/v1/auth/refresh");
    if (error.response?.status !== 401 || originalRequest?._retry || isRefreshCall || !refreshHandler || !logoutHandler) {
      return Promise.reject(error);
    }

    if (isRefreshing) {
      const newToken = await new Promise<string | null>((resolve) => {
        refreshQueue.push(resolve);
      });
      if (!newToken) {
        return Promise.reject(error);
      }
      originalRequest.headers = {
        ...originalRequest.headers,
        Authorization: `Bearer ${newToken}`
      };
      originalRequest._retry = true;
      return http(originalRequest);
    }

    isRefreshing = true;
    originalRequest._retry = true;
    try {
      const refreshToken = await tokenStorage.getRefreshToken();
      if (!refreshToken) {
        throw new Error("missing_refresh_token");
      }
      const nextTokens = await refreshHandler(refreshToken);
      await tokenStorage.setTokens(nextTokens.accessToken, nextTokens.refreshToken);
      processQueue(nextTokens.accessToken);
      originalRequest.headers = {
        ...originalRequest.headers,
        Authorization: `Bearer ${nextTokens.accessToken}`
      };
      return http(originalRequest);
    } catch (refreshError) {
      processQueue(null);
      await logoutHandler();
      return Promise.reject(refreshError);
    } finally {
      isRefreshing = false;
    }
  }
);
