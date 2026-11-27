<script setup lang="ts">
import { appConfig } from '@/config'
import { storeToRefs } from 'pinia'
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'

import { useApiErrorToast } from '@/composables/useApiErrorToast'
import { useAuthStore } from '@/stores/auth'
import AccountMenu from './AccountMenu.vue'
import AppBottomNav from './AppBottomNav.vue'
import AppSidebar from './AppSidebar.vue'
import AppToasts from './AppToasts.vue'
import MoreSheet from './MoreSheet.vue'
import NotificationsBell from './NotificationsBell.vue'
import VerifyEmailBanner from './VerifyEmailBanner.vue'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const { user } = storeToRefs(auth)
const { showError } = useApiErrorToast()
const moreOpen = ref(false)

const title = computed(() => (route.meta.titleKey ? t(route.meta.titleKey) : appConfig.appName))

async function logout(): Promise<void> {
  moreOpen.value = false
  try {
    await auth.logout()
  } catch (error) {
    showError(error)
  }
  await router.push({ name: 'login' })
}
</script>

<template>
  <div class="min-h-dvh lg:flex">
    <a
      href="#main"
      class="sr-only focus:not-sr-only focus:fixed focus:top-2 focus:left-2 focus:z-50"
    >
      {{ t('nav.skipToContent') }}
    </a>
    <AppSidebar class="hidden lg:flex" />
    <div class="flex min-h-dvh min-w-0 flex-1 flex-col">
      <header class="topbar sticky top-0 z-20 lg:hidden">
        <h1 class="topbar-title truncate">{{ title }}</h1>
        <NotificationsBell />
        <AccountMenu v-if="user" :user="user" @logout="logout" />
      </header>
      <main
        id="main"
        class="flex flex-1 flex-col gap-4 px-4 pt-2 pb-32 lg:gap-6 lg:px-10 lg:py-8"
        tabindex="-1"
      >
        <div class="hidden items-center justify-between gap-4 lg:flex">
          <h1 class="t-h1">{{ title }}</h1>
          <div class="topbar-desk">
            <NotificationsBell />
            <AccountMenu v-if="user" :user="user" @logout="logout" />
          </div>
        </div>
        <VerifyEmailBanner />
        <RouterView />
      </main>
    </div>
    <AppBottomNav class="lg:hidden" :more-open="moreOpen" @open-more="moreOpen = true" />
    <MoreSheet v-if="moreOpen" @close="moreOpen = false" @logout="logout" />
    <AppToasts above-bottom-nav />
  </div>
</template>
