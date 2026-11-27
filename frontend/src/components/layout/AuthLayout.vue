<script setup lang="ts">
import { appConfig } from '@/config'
import { ArrowLeft, Gift, Link2, Users } from '@lucide/vue'
import { useI18n } from 'vue-i18n'

import LanguageToggle from '@/components/ui/LanguageToggle.vue'
import AppToasts from './AppToasts.vue'

withDefaults(
  defineProps<{
    /** Desktop brand pitch next to the form (login, register). */
    withPitch?: boolean
    /** "← Logowanie" back link instead of the wordmark (forgot password). */
    backToLogin?: boolean
  }>(),
  { withPitch: false, backToLogin: false },
)

const { t } = useI18n()
</script>

<template>
  <div class="min-h-dvh bg-canvas lg:grid" :class="{ 'lg:grid-cols-2': withPitch }">
    <aside
      v-if="withPitch"
      class="hidden flex-col justify-center gap-8 bg-primary-50 px-16 py-12 lg:flex"
    >
      <span class="wordmark text-3xl">{{ appConfig.appName }}</span>
      <h2 class="t-display max-w-xl">{{ t('auth.pitch.title') }}</h2>
      <p class="t-body-lg text-text-muted max-w-xl">{{ t('auth.pitch.subtitle') }}</p>
      <ul class="m-0 flex list-none flex-col gap-4 p-0">
        <li class="flex items-start gap-3">
          <span class="thumb size-12"><Link2 class="icon" aria-hidden="true" /></span>
          <strong class="t-body">{{ t('auth.pitch.paste') }}</strong>
        </li>
        <li class="flex items-start gap-3">
          <span class="thumb size-12"><Users class="icon" aria-hidden="true" /></span>
          <strong class="t-body">{{ t('auth.pitch.share') }}</strong>
        </li>
        <li class="flex items-start gap-3">
          <span class="thumb size-12"><Gift class="icon" aria-hidden="true" /></span>
          <strong class="t-body">{{ t('auth.pitch.surprise') }}</strong>
        </li>
      </ul>
    </aside>
    <div class="flex flex-col px-4 py-4 lg:items-center lg:justify-center lg:px-8">
      <header class="flex w-full max-w-md items-center justify-between gap-2 lg:mb-4">
        <RouterLink
          v-if="backToLogin"
          :to="{ name: 'login' }"
          class="btn btn-ghost -ml-3 gap-1 px-3 no-underline"
        >
          <ArrowLeft class="icon" aria-hidden="true" />
          <span class="t-label">{{ t('auth.login.title') }}</span>
        </RouterLink>
        <span v-else class="wordmark text-2xl" :class="{ 'lg:invisible': withPitch }">
          {{ appConfig.appName }}
        </span>
        <LanguageToggle />
      </header>
      <main id="main" class="mt-6 flex w-full max-w-md flex-col gap-5 lg:mt-0">
        <slot />
      </main>
    </div>
    <AppToasts />
  </div>
</template>
