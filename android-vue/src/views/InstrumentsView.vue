<template>
  <ion-page>
    <ion-header>
      <ion-toolbar><ion-title>Инструменты</ion-title></ion-toolbar>
      <app-nav />
    </ion-header>
    <ion-content class="ion-padding">
      <ion-item>
        <ion-input v-model="query" placeholder="SBER" label="Поиск" label-placement="stacked" />
      </ion-item>
      <ion-button expand="block" @click="search">Искать</ion-button>
      <state-block v-if="store.isOffline" text="offline: показан кэш поиска" color="warning" />
      <state-block v-if="store.error" :text="store.error" color="danger" />
      <ion-list>
        <ion-item v-for="item in store.items" :key="item.ticker">
          <ion-label>
            <h2>{{ item.ticker }} — {{ item.name }}</h2>
            <p>Цена: {{ formatMoney(item.lastPrice, item.currency) }}</p>
          </ion-label>
        </ion-item>
      </ion-list>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import { IonButton, IonContent, IonHeader, IonInput, IonItem, IonLabel, IonList, IonPage, IonTitle, IonToolbar } from "@ionic/vue";
import { ref } from "vue";
import AppNav from "../components/AppNav.vue";
import StateBlock from "../components/StateBlock.vue";
import { useInstrumentsStore } from "../stores/instrumentsStore";
import { formatMoney } from "../utils/money";

const store = useInstrumentsStore();
const query = ref("SBER");

const search = async (): Promise<void> => {
  await store.search(query.value);
};
</script>
