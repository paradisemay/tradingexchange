<template>
  <ion-card>
    <ion-card-content>
      <ion-text :color="color">{{ text }}</ion-text>
    </ion-card-content>
  </ion-card>
</template>

<script setup lang="ts">
import { IonCard, IonCardContent, IonText } from "@ionic/vue";

defineProps<{ text: string; color?: "danger" | "warning" | "medium" }>();
</script>
