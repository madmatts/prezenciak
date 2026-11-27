import { createRouter, createWebHistory, type RouteLocationNormalized } from 'vue-router'

import AppShell from '@/components/layout/AppShell.vue'
import { useAuthStore } from '@/stores/auth'

declare module 'vue-router' {
  interface RouteMeta {
    /** i18n key of the page title (shell header and document title). */
    titleKey?: string
    /** Only for logged-in users; others are sent to the login page. */
    requiresAuth?: boolean
    /** Only for visitors; logged-in users are sent into the app. */
    guestOnly?: boolean
  }
}

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', redirect: { name: 'items' } },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/auth/LoginView.vue'),
      meta: { guestOnly: true, titleKey: 'auth.login.title' },
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/views/auth/RegisterView.vue'),
      meta: { guestOnly: true, titleKey: 'auth.register.title' },
    },
    {
      path: '/forgot-password',
      name: 'forgot-password',
      component: () => import('@/views/auth/ForgotPasswordView.vue'),
      meta: { guestOnly: true, titleKey: 'auth.forgot.title' },
    },
    {
      path: '/reset',
      name: 'reset-password',
      component: () => import('@/views/auth/ResetPasswordView.vue'),
      meta: { titleKey: 'auth.reset.title' },
    },
    {
      path: '/verify',
      name: 'verify-email',
      component: () => import('@/views/auth/VerifyEmailView.vue'),
      meta: { titleKey: 'auth.verify.pageTitle' },
    },
    {
      path: '/welcome',
      name: 'check-inbox',
      component: () => import('@/views/auth/CheckInboxView.vue'),
      meta: { requiresAuth: true, titleKey: 'auth.inbox.title' },
    },
    {
      path: '/privacy',
      name: 'privacy',
      component: () => import('@/views/PrivacyView.vue'),
      meta: { titleKey: 'privacy.title' },
    },
    {
      path: '/auth/complete',
      name: 'oauth-complete',
      component: () => import('@/views/auth/OAuthCompleteView.vue'),
    },
    {
      path: '/',
      component: AppShell,
      meta: { requiresAuth: true },
      children: [
        {
          path: 'items',
          name: 'items',
          component: () => import('@/views/app/ComingSoonView.vue'),
          meta: { titleKey: 'nav.items' },
        },
        {
          path: 'lists',
          name: 'lists',
          component: () => import('@/views/app/ComingSoonView.vue'),
          meta: { titleKey: 'nav.lists' },
        },
        {
          path: 'circles',
          name: 'circles',
          component: () => import('@/views/app/ComingSoonView.vue'),
          meta: { titleKey: 'nav.circles' },
        },
        {
          path: 'shared',
          name: 'shared',
          component: () => import('@/views/app/ComingSoonView.vue'),
          meta: { titleKey: 'nav.shared' },
        },
        {
          path: 'reservations',
          name: 'reservations',
          component: () => import('@/views/app/ComingSoonView.vue'),
          meta: { titleKey: 'nav.reservations' },
        },
        {
          path: 'notifications',
          name: 'notifications',
          component: () => import('@/views/app/ComingSoonView.vue'),
          meta: { titleKey: 'nav.notifications' },
        },
        {
          path: 'profile',
          name: 'profile',
          component: () => import('@/views/app/ProfileView.vue'),
          meta: { titleKey: 'nav.profile' },
        },
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('@/views/NotFoundView.vue'),
      meta: { titleKey: 'notFound.title' },
    },
  ],
})

/** Where to send a visitor after login: only same-app paths, never an absolute URL (open redirect). */
export function safeRedirect(value: unknown): string | undefined {
  return typeof value === 'string' && value.startsWith('/') && !value.startsWith('//')
    ? value
    : undefined
}

router.beforeEach(async (to: RouteLocationNormalized) => {
  const auth = useAuthStore()
  await auth.ensureLoaded().catch(() => undefined)
  if (to.meta.requiresAuth && !auth.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.meta.guestOnly && auth.isAuthenticated) {
    return safeRedirect(to.query.redirect) ?? { name: 'items' }
  }
  return true
})

export default router
