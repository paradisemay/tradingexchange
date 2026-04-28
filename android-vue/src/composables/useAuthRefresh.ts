import { useAuthStore } from "../stores/authStore";

export const useAuthRefresh = () => {
  const authStore = useAuthStore();

  const restore = async (): Promise<void> => {
    await authStore.restoreSession();
  };

  return { restore };
};
