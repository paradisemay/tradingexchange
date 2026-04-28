import { defineStore } from "pinia";
import { computed, ref } from "vue";
import { authApi } from "../api/authApi";
import { tokenStorage } from "../utils/preferences";
import { configureAuthInterceptors } from "../api/http";
import router from "../router";
import type { LoginRequest, RegisterRequest, TokenPair } from "../types/api";
import { mapApiError } from "./errors";

export const useAuthStore = defineStore("authStore", () => {
  const accessToken = ref<string | null>(null);
  const refreshToken = ref<string | null>(null);
  const loading = ref(false);
  const error = ref<string | null>(null);

  const isAuthenticated = computed(() => Boolean(accessToken.value));

  const setTokens = async (tokens: TokenPair): Promise<void> => {
    accessToken.value = tokens.accessToken;
    refreshToken.value = tokens.refreshToken;
    await tokenStorage.setTokens(tokens.accessToken, tokens.refreshToken);
  };

  const restoreSession = async (): Promise<void> => {
    accessToken.value = await tokenStorage.getAccessToken();
    refreshToken.value = await tokenStorage.getRefreshToken();
  };

  const register = async (payload: RegisterRequest): Promise<void> => {
    loading.value = true;
    error.value = null;
    try {
      const result = await authApi.register(payload);
      await setTokens({ accessToken: result.accessToken, refreshToken: result.refreshToken });
      await router.replace("/portfolio");
    } catch (e) {
      error.value = mapApiError(e);
      throw e;
    } finally {
      loading.value = false;
    }
  };

  const login = async (payload: LoginRequest): Promise<void> => {
    loading.value = true;
    error.value = null;
    try {
      const result = await authApi.login(payload);
      await setTokens(result);
      await router.replace("/portfolio");
    } catch (e) {
      error.value = mapApiError(e);
      throw e;
    } finally {
      loading.value = false;
    }
  };

  const refresh = async (currentRefreshToken: string): Promise<TokenPair> => {
    const result = await authApi.refresh({ refreshToken: currentRefreshToken });
    await setTokens(result);
    return result;
  };

  const logout = async (): Promise<void> => {
    try {
      if (refreshToken.value) {
        await authApi.logout({ refreshToken: refreshToken.value });
      }
    } finally {
      accessToken.value = null;
      refreshToken.value = null;
      await tokenStorage.clear();
      await router.replace("/login");
    }
  };

  configureAuthInterceptors({
    onRefresh: refresh,
    onLogout: logout
  });

  return {
    accessToken,
    refreshToken,
    loading,
    error,
    isAuthenticated,
    register,
    login,
    logout,
    restoreSession
  };
});
