<template>
  <ion-page>
    <ion-header>
      <ion-toolbar><ion-title>Новая заявка</ion-title></ion-toolbar>
      <app-nav />
    </ion-header>
    <ion-content class="ion-padding">
      <form @submit.prevent="onSubmit">
        <ion-item><ion-input v-model="ticker" label="Ticker" label-placement="stacked" /></ion-item>
        <div class="form-error">{{ errors.ticker }}</div>
        <ion-item>
          <ion-select v-model="side" label="Side" label-placement="stacked">
            <ion-select-option value="BUY">BUY</ion-select-option>
            <ion-select-option value="SELL">SELL</ion-select-option>
          </ion-select>
        </ion-item>
        <ion-item>
          <ion-select v-model="orderType" label="Order Type" label-placement="stacked">
            <ion-select-option value="MARKET">MARKET</ion-select-option>
            <ion-select-option value="LIMIT">LIMIT</ion-select-option>
          </ion-select>
        </ion-item>
        <ion-item><ion-input v-model="quantity" label="Quantity" label-placement="stacked" /></ion-item>
        <div class="form-error">{{ errors.quantity }}</div>
        <ion-item>
          <ion-input v-model="limitPrice" :disabled="orderType === 'MARKET'" label="Limit Price" label-placement="stacked" />
        </ion-item>
        <div class="form-error">{{ errors.limitPrice }}</div>

        <state-block v-if="store.error" :text="store.error" color="danger" />
        <ion-button expand="block" type="submit" :disabled="store.loading">Отправить</ion-button>
      </form>
      <state-block
        v-if="store.lastOrder"
        :text="`Создано: ${store.lastOrder.orderId}, статус: ${store.lastOrder.status}`"
        color="medium"
      />
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import {
  IonButton,
  IonContent,
  IonHeader,
  IonInput,
  IonItem,
  IonPage,
  IonSelect,
  IonSelectOption,
  IonTitle,
  IonToolbar
} from "@ionic/vue";
import { useForm } from "vee-validate";
import { toTypedSchema } from "@vee-validate/zod";
import { z } from "zod";
import { useOrderStore } from "../stores/orderStore";
import AppNav from "../components/AppNav.vue";
import StateBlock from "../components/StateBlock.vue";

const store = useOrderStore();
const schema = toTypedSchema(
  z
    .object({
      ticker: z.string().min(1),
      side: z.enum(["BUY", "SELL"]),
      orderType: z.enum(["MARKET", "LIMIT"]),
      quantity: z.string().min(1),
      limitPrice: z.string().nullable()
    })
    .superRefine((value, ctx) => {
      if (value.orderType === "MARKET" && value.limitPrice !== null) {
        ctx.addIssue({ code: "custom", path: ["limitPrice"], message: "Для MARKET limitPrice должен быть null." });
      }
      if (value.orderType === "LIMIT" && (!value.limitPrice || value.limitPrice.trim().length === 0)) {
        ctx.addIssue({ code: "custom", path: ["limitPrice"], message: "Для LIMIT укажите limitPrice." });
      }
    })
);

const { defineField, errors, handleSubmit } = useForm({
  validationSchema: schema,
  initialValues: {
    ticker: "SBER",
    side: "BUY" as const,
    orderType: "MARKET" as const,
    quantity: "1",
    limitPrice: null as string | null
  }
});

const [ticker] = defineField("ticker");
const [side] = defineField("side");
const [orderType] = defineField("orderType");
const [quantity] = defineField("quantity");
const [limitPrice] = defineField("limitPrice");

const onSubmit = handleSubmit(async (values) => {
  await store.createOrder({
    ticker: values.ticker,
    side: values.side,
    orderType: values.orderType,
    quantity: values.quantity,
    limitPrice: values.orderType === "MARKET" ? null : values.limitPrice
  });
});
</script>
