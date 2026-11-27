<script setup lang="ts">
import { X } from '@lucide/vue'
import { nextTick, onBeforeUnmount, onMounted, useTemplateRef } from 'vue'
import { useI18n } from 'vue-i18n'

defineProps<{ title: string }>()
const emit = defineEmits<{ close: [] }>()

const { t } = useI18n()
const panel = useTemplateRef<HTMLElement>('panel')
let previouslyFocused: HTMLElement | null = null

function onKeydown(event: KeyboardEvent): void {
  if (event.key === 'Escape') {
    emit('close')
  }
}

onMounted(async () => {
  previouslyFocused = document.activeElement as HTMLElement | null
  await nextTick()
  panel.value?.querySelector<HTMLElement>('input, button:not([data-dialog-close])')?.focus()
})

onBeforeUnmount(() => previouslyFocused?.focus())
</script>

<template>
  <Teleport to="body">
    <div class="scrim" aria-hidden="true" @click="emit('close')"></div>
    <!-- Bottom sheet on mobile, centred modal on desktop (design P0). -->
    <div
      ref="panel"
      class="sheet lg:inset-auto lg:top-1/2 lg:left-1/2 lg:w-[30rem] lg:-translate-x-1/2 lg:-translate-y-1/2 lg:rounded-xl lg:p-6"
      role="dialog"
      aria-modal="true"
      aria-labelledby="dialog-title"
      @keydown="onKeydown"
    >
      <span class="sheet-handle lg:hidden" aria-hidden="true"></span>
      <div class="flex items-center gap-2">
        <h2 id="dialog-title" class="t-h2 flex-1">{{ title }}</h2>
        <button
          type="button"
          class="btn btn-ghost btn-icon"
          data-dialog-close
          :aria-label="t('common.close')"
          @click="emit('close')"
        >
          <X class="icon" aria-hidden="true" />
        </button>
      </div>
      <slot />
    </div>
  </Teleport>
</template>
