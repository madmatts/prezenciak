import { useI18n } from 'vue-i18n'

import { ApiError, UNKNOWN_ERROR } from '@/api/problem'
import { useToastStore } from '@/stores/toast'

/** Shows an API failure as a translated error toast (`errors.<code>`, falling back to a generic text). */
export function useApiErrorToast() {
  const toast = useToastStore()
  const { te } = useI18n()

  function showError(error: unknown): void {
    const code = error instanceof ApiError ? error.code : UNKNOWN_ERROR
    const key = te(`errors.${code}`) ? `errors.${code}` : `errors.${UNKNOWN_ERROR}`
    const params = error instanceof ApiError ? error.params : {}
    toast.show('error', key, params)
  }

  return { showError }
}
