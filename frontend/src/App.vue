<script setup lang="ts">
import { appConfig } from '@/config'
import { storeToRefs } from 'pinia'
import { watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'

import { useLocale } from '@/composables/useLocale'
import { useAuthStore } from '@/stores/auth'

const { t } = useI18n()
const route = useRoute()
const { user } = storeToRefs(useAuthStore())
const { locale, setLocale } = useLocale()

// The profile's language wins over the browser's once the user is known (CLAUDE.md §7).
watch(
  () => user.value?.locale,
  (profileLocale) => {
    if (profileLocale && profileLocale !== locale.value) {
      setLocale(profileLocale)
    }
  },
  { immediate: true },
)

// Page title: "<page> · Wishka", translated.
watch(
  [() => route.meta.titleKey, locale],
  ([titleKey]) => {
    document.title = titleKey ? `${t(titleKey)} · ${appConfig.appName}` : appConfig.appName
  },
  { immediate: true },
)
</script>

<template>
  <RouterView />
</template>
