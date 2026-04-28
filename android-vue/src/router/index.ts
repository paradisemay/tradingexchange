import { createRouter, createWebHistory } from "@ionic/vue-router";
import { tokenStorage } from "../utils/preferences";

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: "/", redirect: "/portfolio" },
    { path: "/login", component: () => import("../views/LoginView.vue"), meta: { guestOnly: true } },
    { path: "/register", component: () => import("../views/RegisterView.vue"), meta: { guestOnly: true } },
    { path: "/portfolio", component: () => import("../views/PortfolioView.vue"), meta: { requiresAuth: true } },
    { path: "/profile", component: () => import("../views/ProfileView.vue"), meta: { requiresAuth: true } },
    { path: "/instruments", component: () => import("../views/InstrumentsView.vue"), meta: { requiresAuth: true } },
    { path: "/orders/create", component: () => import("../views/OrderCreateView.vue"), meta: { requiresAuth: true } },
    { path: "/orders", component: () => import("../views/OrdersHistoryView.vue"), meta: { requiresAuth: true } },
    { path: "/transactions", component: () => import("../views/TransactionsHistoryView.vue"), meta: { requiresAuth: true } }
  ]
});

router.beforeEach(async (to) => {
  const token = await tokenStorage.getAccessToken();
  if (to.meta.requiresAuth && !token) {
    return "/login";
  }
  if (to.meta.guestOnly && token) {
    return "/portfolio";
  }
  return true;
});

export default router;
