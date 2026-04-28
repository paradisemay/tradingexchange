<template>
  <ion-page>
    <ion-header>
      <ion-toolbar><ion-title>Портфель</ion-title></ion-toolbar>
      <app-nav />
    </ion-header>
    <ion-content class="ion-padding">
      <state-block v-if="portfolio.isOffline" text="offline: показаны кэшированные данные" color="warning" />
      <state-block v-if="portfolio.error" :text="portfolio.error" color="danger" />
      <ion-list v-if="portfolio.data">
        <ion-item>
          <ion-label>
            <h2>Кэш</h2>
            <p>{{ formatMoney(portfolio.data.cash.available, portfolio.data.cash.currency) }}</p>
          </ion-label>
        </ion-item>
        <ion-item v-for="position in portfolio.enrichedPositions" :key="position.ticker">
          <ion-label>
            <h2>{{ position.ticker }}</h2>
            <p>Количество: {{ formatQuantity(position.quantity) }}</p>
            <p>Средняя: {{ position.avgPriceFormatted }}</p>
            <p>Текущая: {{ position.currentPriceFormatted }}</p>
          </ion-label>
        </ion-item>
      </ion-list>
      <state-block
        v-if="!portfolio.loading && portfolio.data && portfolio.enrichedPositions.length === 0"
        text="Портфель пуст"
        color="medium"
      />
      <ion-spinner v-if="portfolio.loading" />
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import { IonContent, IonHeader, IonItem, IonLabel, IonList, IonPage, IonSpinner, IonTitle, IonToolbar } from "@ionic/vue";
import { onMounted, onUnmounted } from "vue";
import AppNav from "../components/AppNav.vue";
import StateBlock from "../components/StateBlock.vue";
import { usePortfolioStore } from "../stores/portfolioStore";
import { formatMoney, formatQuantity } from "../utils/money";
import { useWebSocket } from "../composables/useWebSocket";

const portfolio = usePortfolioStore();
const ws = useWebSocket();

onMounted(async () => {
  await portfolio.loadPortfolio();
  await ws.connect();
  const tickers = portfolio.enrichedPositions.map((item) => item.ticker);
  if (tickers.length > 0) {
    ws.subscribe(tickers);
  }
});

onUnmounted(() => {
  ws.disconnect();
});
</script>
