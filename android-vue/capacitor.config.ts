import type { CapacitorConfig } from "@capacitor/cli";

const config: CapacitorConfig = {
  appId: "com.tradingexchange.androidvue",
  appName: "Trading Exchange Vue",
  webDir: "dist",
  server: {
    androidScheme: "https"
  }
};

export default config;
