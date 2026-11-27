<script setup lang="ts">
import { useI18n } from 'vue-i18n'

import { useLocale } from '@/composables/useLocale'
import { isSupportedLocale } from '@/i18n/locale'

const { t } = useI18n()
const { locale, locales, setLocale } = useLocale()

function onChange(event: Event): void {
  const value = (event.target as HTMLSelectElement).value
  if (isSupportedLocale(value)) {
    setLocale(value)
  }
}
</script>

<template>
  <label class="language-switcher">
    <span class="language-switcher-label">{{ t('common.language.label') }}</span>
    <select
      class="language-switcher-select"
      data-testid="language-switcher"
      :value="locale"
      @change="onChange"
    >
      <option v-for="code in locales" :key="code" :value="code">
        {{ t(`common.language.${code}`) }}
      </option>
    </select>
  </label>
</template>

<style scoped>
@reference "../assets/main.css";

.language-switcher {
  @apply inline-flex items-center gap-2 text-sm;
}

.language-switcher-select {
  @apply min-h-11 rounded-md border border-gray-300 bg-white px-3 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue-600;
}
</style>
