import { Preferences } from "@capacitor/preferences";

const ACCESS_TOKEN_KEY = "auth.accessToken";
const REFRESH_TOKEN_KEY = "auth.refreshToken";

export const tokenStorage = {
  async getAccessToken(): Promise<string | null> {
    const value = await Preferences.get({ key: ACCESS_TOKEN_KEY });
    return value.value;
  },
  async getRefreshToken(): Promise<string | null> {
    const value = await Preferences.get({ key: REFRESH_TOKEN_KEY });
    return value.value;
  },
  async setTokens(accessToken: string, refreshToken: string): Promise<void> {
    await Preferences.set({ key: ACCESS_TOKEN_KEY, value: accessToken });
    await Preferences.set({ key: REFRESH_TOKEN_KEY, value: refreshToken });
  },
  async clear(): Promise<void> {
    await Preferences.remove({ key: ACCESS_TOKEN_KEY });
    await Preferences.remove({ key: REFRESH_TOKEN_KEY });
  }
};
