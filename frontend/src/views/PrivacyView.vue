<script setup lang="ts">
import { useI18n } from 'vue-i18n'

import AuthLayout from '@/components/layout/AuthLayout.vue'

const { t } = useI18n()
</script>

<template>
  <!-- The full privacy policy (PL/EN) is written in Phase 8, before public launch. -->
  <AuthLayout>
    <h1 class="t-h1">{{ t('privacy.title') }}</h1>
    <p class="t-body text-text-muted">{{ t('privacy.placeholder') }}</p>
  </AuthLayout>
</template>
