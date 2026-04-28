<template>
  <ion-page>
    <ion-header>
      <ion-toolbar><ion-title>История ордеров</ion-title></ion-toolbar>
      <app-nav />
    </ion-header>
    <ion-content class="ion-padding">
      <state-block v-if="store.error" :text="store.error" color="danger" />
      <ion-list>
        <ion-item v-for="order in store.orders" :key="order.orderId">
          <ion-label>
            <h2>{{ order.ticker }} {{ order.side }} {{ order.orderType }}</h2>
            <p>Qty: {{ order.quantity }}, Status: {{ order.status }}</p>
            <p>Created: {{ order.createdAt }}</p>
          </ion-label>
        </ion-item>
      </ion-list>
      <state-block v-if="!store.loading && store.orders.length === 0" text="Нет ордеров" color="medium" />
      <ion-button expand="block" :disabled="!store.nextCursor || store.loading" @click="loadMore">Загрузить ещё</ion-button>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import { IonButton, IonContent, IonHeader, IonItem, IonLabel, IonList, IonPage, IonTitle, IonToolbar } from "@ionic/vue";
import { onMounted } from "vue";
import AppNav from "../components/AppNav.vue";
import StateBlock from "../components/StateBlock.vue";
import { useOrderStore } from "../stores/orderStore";

const store = useOrderStore();

onMounted(async () => {
  await store.loadOrders(false);
});

const loadMore = async (): Promise<void> => {
  await store.loadOrders(true);
};
</script>
