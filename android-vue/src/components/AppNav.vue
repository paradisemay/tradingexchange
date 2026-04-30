<template>
  <ion-toolbar>
    <ion-buttons slot="start">
      <ion-button router-link="/portfolio">Портфель</ion-button>
      <ion-button router-link="/instruments">Инструменты</ion-button>
      <ion-button router-link="/orders/create">Заявка</ion-button>
    </ion-buttons>
    <ion-buttons slot="end">
      <ion-button router-link="/orders">Ордеры</ion-button>
      <ion-button router-link="/transactions">Транзакции</ion-button>
      <ion-button router-link="/profile">Профиль</ion-button>
    </ion-buttons>
  </ion-toolbar>
</template>

<script setup lang="ts">
import { IonButton, IonButtons, IonToolbar } from "@ionic/vue";
</script>
