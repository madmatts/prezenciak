<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { useI18n } from 'vue-i18n'

import { useToastStore } from '@/stores/toast'

const { t } = useI18n()
const store = useToastStore()
const { toasts } = storeToRefs(store)
</script>

<template>
  <div class="toasts" aria-live="polite" aria-atomic="false">
    <div
      v-for="toast in toasts"
      :key="toast.id"
      class="toast"
      :class="`toast--${toast.kind}`"
      :role="toast.kind === 'error' ? 'alert' : 'status'"
    >
      <p class="toast-message">{{ t(toast.messageKey, toast.params) }}</p>
      <button
        type="button"
        class="toast-close"
        :aria-label="t('common.close')"
        @click="store.dismiss(toast.id)"
      >
        <svg aria-hidden="true" viewBox="0 0 24 24" class="toast-close-icon">
          <path d="M6 6l12 12M18 6L6 18" stroke="currentColor" stroke-width="2" fill="none" />
        </svg>
      </button>
    </div>
  </div>
</template>

<style scoped>
@reference "../assets/main.css";

.toasts {
  @apply fixed inset-x-4 bottom-4 z-50 flex flex-col gap-2 sm:left-auto sm:w-96;
}

.toast {
  @apply flex items-start gap-3 rounded-lg border p-4 shadow-md;
}

.toast--error {
  @apply border-red-300 bg-red-50 text-red-900;
}

.toast--success {
  @apply border-green-300 bg-green-50 text-green-900;
}

.toast--info {
  @apply border-blue-300 bg-blue-50 text-blue-900;
}

.toast-message {
  @apply flex-1;
}

.toast-close {
  @apply -m-2 inline-flex size-11 items-center justify-center rounded-md focus-visible:outline-2 focus-visible:outline-current;
}

.toast-close-icon {
  @apply size-5;
}
</style>
