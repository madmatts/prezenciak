<script setup lang="ts">
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
</script>

<template>
  <main class="not-found">
    <h1 class="not-found-title">{{ t('notFound.title') }}</h1>
    <RouterLink class="not-found-link" :to="{ name: 'home' }">
      {{ t('notFound.backHome') }}
    </RouterLink>
  </main>
</template>

<style scoped>
@reference "../assets/main.css";

.not-found {
  @apply mx-auto flex min-h-dvh max-w-2xl flex-col justify-center gap-4 px-4;
}

.not-found-title {
  @apply text-2xl font-bold;
}

.not-found-link {
  @apply text-blue-700 underline focus-visible:outline-2 focus-visible:outline-blue-600;
}
</style>
