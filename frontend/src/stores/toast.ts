import { defineStore } from 'pinia'
import { ref } from 'vue'

export type ToastKind = 'success' | 'error' | 'info'

export interface ToastAction {
  /** i18n key of the button label, e.g. `common.retry`. */
  labelKey: string
  run: () => void
}

export interface Toast {
  id: number
  kind: ToastKind
  /** i18n key, never a literal text. */
  messageKey: string
  params: Record<string, unknown>
  action?: ToastAction
}

const AUTO_DISMISS_MS = 5000
const AUTO_DISMISS_WITH_ACTION_MS = 8000
const MAX_VISIBLE = 3

/**
 * Global toasts (design P0): success/info disappear after 5 s (8 s with an action), errors stay until closed or
 * retried, at most 3 at a time, newest at the bottom.
 */
export const useToastStore = defineStore('toast', () => {
  const toasts = ref<Toast[]>([])
  let nextId = 1

  function show(
    kind: ToastKind,
    messageKey: string,
    params: Record<string, unknown> = {},
    action?: ToastAction,
  ): number {
    const id = nextId++
    toasts.value = [...toasts.value, { id, kind, messageKey, params, action }].slice(-MAX_VISIBLE)
    if (kind !== 'error') {
      setTimeout(() => dismiss(id), action ? AUTO_DISMISS_WITH_ACTION_MS : AUTO_DISMISS_MS)
    }
    return id
  }

  function dismiss(id: number): void {
    toasts.value = toasts.value.filter((toast) => toast.id !== id)
  }

  function clear(): void {
    toasts.value = []
  }

  return { toasts, show, dismiss, clear }
})
