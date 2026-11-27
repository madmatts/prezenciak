<script setup lang="ts">
import { useI18n } from 'vue-i18n'

import ApiStatusBadge from '@/components/ApiStatusBadge.vue'
import LanguageSwitcher from '@/components/LanguageSwitcher.vue'
import { useApiStatus } from '@/composables/useApiStatus'

const { t } = useI18n()
const { status } = useApiStatus()
</script>

<template>
  <main class="home">
    <header class="home-header">
      <LanguageSwitcher />
    </header>
    <section class="home-hero">
      <h1 class="home-title">{{ t('home.title') }}</h1>
      <p class="home-tagline">{{ t('home.tagline') }}</p>
      <p class="home-soon">{{ t('home.comingSoon') }}</p>
      <ApiStatusBadge :status="status" />
    </section>
  </main>
</template>

<style scoped>
@reference "../assets/main.css";

.home {
  @apply mx-auto flex min-h-dvh max-w-2xl flex-col px-4 py-6;
}

.home-header {
  @apply flex justify-end;
}

.home-hero {
  @apply flex flex-1 flex-col justify-center gap-4;
}

.home-title {
  @apply text-4xl font-bold;
}

.home-tagline {
  @apply text-lg;
}

.home-soon {
  @apply text-gray-700;
}
</style>
