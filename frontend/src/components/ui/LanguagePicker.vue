<script setup lang="ts">
import { useI18n } from 'vue-i18n'

import { useLanguage } from '@/composables/useLanguage'
import type { AppLocale } from '@/i18n/locale'

const { t } = useI18n()
const { locale, locales, changeLanguage } = useLanguage()

function select(next: AppLocale): void {
  void changeLanguage(next)
}
</script>

<template>
  <fieldset class="segmented m-0 border-0">
    <legend class="sr-only">{{ t('common.language.label') }}</legend>
    <label v-for="code in locales" :key="code" :lang="code">
      <input
        type="radio"
        name="app-language"
        :value="code"
        :checked="locale === code"
        data-testid="language-option"
        @change="select(code)"
      />
      {{ t(`common.language.${code}`) }}
    </label>
  </fieldset>
</template>
