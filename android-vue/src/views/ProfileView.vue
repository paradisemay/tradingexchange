<template>
  <ion-page>
    <ion-header>
      <ion-toolbar><ion-title>Профиль</ion-title></ion-toolbar>
      <app-nav />
    </ion-header>
    <ion-content class="ion-padding">
      <state-block v-if="store.error" :text="store.error" color="danger" />
      <ion-list v-if="store.profile">
        <ion-item><ion-label>ID: <span class="mono">{{ store.profile.userId }}</span></ion-label></ion-item>
        <ion-item><ion-label>Email: {{ store.profile.email }}</ion-label></ion-item>
        <ion-item><ion-label>ФИО: {{ store.profile.fullName ?? "-" }}</ion-label></ion-item>
        <ion-item><ion-label>Роль: {{ store.profile.role }}</ion-label></ion-item>
      </ion-list>
      <ion-button expand="block" color="danger" @click="logout">Logout</ion-button>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import { IonButton, IonContent, IonHeader, IonItem, IonLabel, IonList, IonPage, IonTitle, IonToolbar } from "@ionic/vue";
import { onMounted } from "vue";
import AppNav from "../components/AppNav.vue";
import StateBlock from "../components/StateBlock.vue";
import { useProfileStore } from "../stores/profileStore";
import { useAuthStore } from "../stores/authStore";

const store = useProfileStore();
const auth = useAuthStore();

onMounted(async () => {
  await store.loadProfile();
});

const logout = async (): Promise<void> => {
  await auth.logout();
};
</script>
