<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

import { useLanguage } from '@/composables/useLanguage'

const { t } = useI18n()
const { locale, changeLanguage } = useLanguage()

/** Auth screens show a single button with the *other* language (design: "English" / "Polski"). */
const other = computed(() => (locale.value === 'pl' ? 'en' : 'pl'))
</script>

<template>
  <button
    type="button"
    class="btn btn-ghost btn-sm"
    :lang="other"
    data-testid="language-toggle"
    @click="changeLanguage(other)"
  >
    {{ t(`common.language.${other}`) }}
  </button>
</template>
