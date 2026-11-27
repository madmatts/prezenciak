import { defineStore } from 'pinia'
import { ref } from 'vue'

export type ToastKind = 'success' | 'error' | 'info'

export interface Toast {
  id: number
  kind: ToastKind
  /** i18n key, never a literal text. */
  messageKey: string
  params: Record<string, unknown>
}

const AUTO_DISMISS_MS = 6000

/** Global notification toasts, shared by every view (the API error handler pushes here). */
export const useToastStore = defineStore('toast', () => {
  const toasts = ref<Toast[]>([])
  let nextId = 1

  function show(kind: ToastKind, messageKey: string, params: Record<string, unknown> = {}): number {
    const id = nextId++
    toasts.value.push({ id, kind, messageKey, params })
    if (kind !== 'error') {
      setTimeout(() => dismiss(id), AUTO_DISMISS_MS)
    }
    return id
  }

  function dismiss(id: number): void {
    toasts.value = toasts.value.filter((toast) => toast.id !== id)
  }

  return { toasts, show, dismiss }
})
