import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory, type Router } from 'vue-router'

import { createAppI18n } from '@/i18n'

/** A router with the named routes the auth views link to; components are stubs. */
export function testRouter(): Router {
  const stub = { template: '<div />' }
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      'login',
      'register',
      'forgot-password',
      'items',
      'check-inbox',
      'privacy',
      'profile',
      'notifications',
    ].map((name) => ({ path: `/${name}`, name, component: stub })),
  })
}

export function plugins(locale: 'pl' | 'en' = 'pl') {
  const pinia = createPinia()
  setActivePinia(pinia)
  return [pinia, testRouter(), createAppI18n(locale)]
}
