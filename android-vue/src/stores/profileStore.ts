import { defineStore } from "pinia";
import { ref } from "vue";
import type { UserProfile } from "../types/api";
import { profileApi } from "../api/profileApi";
import { mapApiError } from "./errors";

export const useProfileStore = defineStore("profileStore", () => {
  const profile = ref<UserProfile | null>(null);
  const loading = ref(false);
  const error = ref<string | null>(null);

  const loadProfile = async (): Promise<void> => {
    loading.value = true;
    error.value = null;
    try {
      profile.value = await profileApi.getMe();
    } catch (e) {
      error.value = mapApiError(e);
    } finally {
      loading.value = false;
    }
  };

  return { profile, loading, error, loadProfile };
});
