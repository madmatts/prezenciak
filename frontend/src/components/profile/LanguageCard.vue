<script setup lang="ts">
import { useI18n } from 'vue-i18n'

import LanguagePicker from '@/components/ui/LanguagePicker.vue'

const { t } = useI18n()
</script>

<template>
  <section class="section-card" aria-labelledby="profile-language-title">
    <h2 id="profile-language-title" class="t-h3">{{ t('profile.language.title') }}</h2>
    <LanguagePicker />
    <p class="t-caption">{{ t('profile.language.hint') }}</p>
  </section>
</template>
