<template>
  <ion-page>
    <ion-header>
      <ion-toolbar><ion-title>Вход</ion-title></ion-toolbar>
    </ion-header>
    <ion-content class="ion-padding">
      <form @submit.prevent="onSubmit">
        <ion-item>
          <ion-input v-model="email" label="Email" label-placement="stacked" type="email" />
        </ion-item>
        <div class="form-error">{{ errors.email }}</div>
        <ion-item>
          <ion-input v-model="password" label="Пароль" label-placement="stacked" type="password" />
        </ion-item>
        <div class="form-error">{{ errors.password }}</div>
        <state-block v-if="auth.error" :text="auth.error" color="danger" />
        <ion-button expand="block" type="submit" :disabled="auth.loading">Войти</ion-button>
      </form>
      <ion-button expand="block" fill="clear" router-link="/register">Нет аккаунта? Регистрация</ion-button>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import { IonButton, IonContent, IonHeader, IonInput, IonItem, IonPage, IonTitle, IonToolbar } from "@ionic/vue";
import { useForm } from "vee-validate";
import { toTypedSchema } from "@vee-validate/zod";
import { z } from "zod";
import { useAuthStore } from "../stores/authStore";
import StateBlock from "../components/StateBlock.vue";

const auth = useAuthStore();

const schema = toTypedSchema(
  z.object({
    email: z.string().email(),
    password: z.string().min(1)
  })
);

const { defineField, errors, handleSubmit } = useForm({
  validationSchema: schema,
  initialValues: { email: "", password: "" }
});
const [email] = defineField("email");
const [password] = defineField("password");

const onSubmit = handleSubmit(async (values) => {
  await auth.login(values);
});
</script>
