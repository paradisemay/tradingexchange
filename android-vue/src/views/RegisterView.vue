<template>
  <ion-page>
    <ion-header>
      <ion-toolbar><ion-title>Регистрация</ion-title></ion-toolbar>
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
        <ion-item>
          <ion-input v-model="fullName" label="ФИО" label-placement="stacked" />
        </ion-item>
        <state-block v-if="auth.error" :text="auth.error" color="danger" />
        <ion-button expand="block" type="submit" :disabled="auth.loading">Создать аккаунт</ion-button>
      </form>
      <ion-button expand="block" fill="clear" router-link="/login">Уже есть аккаунт? Вход</ion-button>
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
    password: z.string().min(8),
    fullName: z.string().optional()
  })
);

const { defineField, errors, handleSubmit } = useForm({
  validationSchema: schema,
  initialValues: { email: "", password: "", fullName: "" }
});

const [email] = defineField("email");
const [password] = defineField("password");
const [fullName] = defineField("fullName");

const onSubmit = handleSubmit(async (values) => {
  await auth.register({
    email: values.email,
    password: values.password,
    fullName: values.fullName ? values.fullName : null
  });
});
</script>
