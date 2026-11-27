<script setup lang="ts">
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
</script>

<template>
  <div class="flex items-center gap-3 text-sm text-text-muted" role="separator">
    <span class="h-px flex-1 bg-border"></span>
    {{ t('auth.orEmail') }}
    <span class="h-px flex-1 bg-border"></span>
  </div>
</template>
