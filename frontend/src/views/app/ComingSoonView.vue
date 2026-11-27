<script setup lang="ts">
import { Sparkles } from '@lucide/vue'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
</script>

<template>
  <!-- Placeholder for sections built in later phases. -->
  <section class="empty card">
    <span class="empty-art"><Sparkles class="icon icon-xl" aria-hidden="true" /></span>
    <h2 class="t-h2">{{ t('comingSoon.title') }}</h2>
    <p class="t-body text-text-muted max-w-md">{{ t('comingSoon.text') }}</p>
  </section>
</template>
