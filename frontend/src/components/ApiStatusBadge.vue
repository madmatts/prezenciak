<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

import type { ApiStatus } from '@/composables/useApiStatus'

const props = defineProps<{ status: ApiStatus }>()

const { t } = useI18n()
const text = computed(() => t(`home.apiStatus.${props.status}`))
</script>

<template>
  <p class="api-status" :class="`api-status--${props.status}`" data-testid="api-status">
    <span class="api-status-label">{{ t('home.apiStatus.label') }}:</span>
    {{ text }}
  </p>
</template>

<style scoped>
@reference "../assets/main.css";

.api-status {
  @apply text-sm;
}

.api-status-label {
  @apply font-medium;
}

.api-status--ok {
  @apply text-green-800;
}

.api-status--error {
  @apply text-red-800;
}

.api-status--checking {
  @apply text-gray-700;
}
</style>
