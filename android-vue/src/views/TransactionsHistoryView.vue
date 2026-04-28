<template>
  <ion-page>
    <ion-header>
      <ion-toolbar><ion-title>История транзакций</ion-title></ion-toolbar>
      <app-nav />
    </ion-header>
    <ion-content class="ion-padding">
      <state-block v-if="store.error" :text="store.error" color="danger" />
      <ion-list>
        <ion-item v-for="transaction in store.transactions" :key="transaction.id">
          <ion-label>
            <h2>{{ transaction.type }} {{ transaction.ticker ?? "-" }}</h2>
            <p>Amount: {{ transaction.amount }}, Qty: {{ formatQuantity(transaction.quantity) }}</p>
            <p>Created: {{ transaction.createdAt }}</p>
          </ion-label>
        </ion-item>
      </ion-list>
      <state-block v-if="!store.loading && store.transactions.length === 0" text="Нет транзакций" color="medium" />
      <ion-button expand="block" :disabled="!store.nextCursor || store.loading" @click="loadMore">Загрузить ещё</ion-button>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import { IonButton, IonContent, IonHeader, IonItem, IonLabel, IonList, IonPage, IonTitle, IonToolbar } from "@ionic/vue";
import { onMounted } from "vue";
import AppNav from "../components/AppNav.vue";
import StateBlock from "../components/StateBlock.vue";
import { useTransactionStore } from "../stores/transactionStore";
import { formatQuantity } from "../utils/money";

const store = useTransactionStore();

onMounted(async () => {
  await store.loadTransactions(false);
});

const loadMore = async (): Promise<void> => {
  await store.loadTransactions(true);
};
</script>
