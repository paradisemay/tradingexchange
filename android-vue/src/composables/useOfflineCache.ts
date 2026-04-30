import { ref } from "vue";

export const useOfflineCache = () => {
  const isOffline = ref(!navigator.onLine);

  const update = (): void => {
    isOffline.value = !navigator.onLine;
  };

  window.addEventListener("online", update);
  window.addEventListener("offline", update);

  return { isOffline };
};
