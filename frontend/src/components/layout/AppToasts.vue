<script setup lang="ts">
import { CircleAlert, CircleCheck, Info, X } from '@lucide/vue'
import { storeToRefs } from 'pinia'
import { useI18n } from 'vue-i18n'

import { useToastStore, type ToastKind } from '@/stores/toast'

withDefaults(defineProps<{ aboveBottomNav?: boolean }>(), { aboveBottomNav: false })

const { t } = useI18n()
const store = useToastStore()
const { toasts } = storeToRefs(store)

const ICONS = { success: CircleCheck, info: Info, error: CircleAlert } satisfies Record<
  ToastKind,
  unknown
>
</script>

<template>
  <!-- Mobile: above the bottom navigation. Desktop: bottom-right, max 3 (design P0). -->
  <div
    class="toast-region inset-x-4 lg:right-8 lg:bottom-8 lg:left-auto lg:w-96"
    :class="aboveBottomNav ? 'bottom-28' : 'bottom-4'"
    aria-live="polite"
    aria-atomic="false"
  >
    <div
      v-for="toast in toasts"
      :key="toast.id"
      class="toast"
      :class="{ 'toast-error': toast.kind === 'error', 'toast-info': toast.kind === 'info' }"
      :role="toast.kind === 'error' ? 'alert' : 'status'"
    >
      <component :is="ICONS[toast.kind]" class="icon" aria-hidden="true" />
      <span class="flex-1">{{ t(toast.messageKey, toast.params) }}</span>
      <button
        v-if="toast.action"
        type="button"
        class="btn"
        @click="(toast.action.run(), store.dismiss(toast.id))"
      >
        {{ t(toast.action.labelKey) }}
      </button>
      <button
        type="button"
        class="btn btn-icon text-text-inverse"
        :aria-label="t('common.close')"
        @click="store.dismiss(toast.id)"
      >
        <X class="icon text-text-inverse" aria-hidden="true" />
      </button>
    </div>
  </div>
</template>
