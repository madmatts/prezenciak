import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'

import { fetchPing } from '@/api/ping'
import { useApiErrorToast } from './useApiErrorToast'

export type ApiStatus = 'checking' | 'ok' | 'error'

/** Phase 0 smoke check: is the backend reachable? Shows a translated toast when it is not. */
export function useApiStatus() {
  const status = ref<ApiStatus>('checking')
  const { locale } = useI18n()
  const { showError } = useApiErrorToast()

  async function check(): Promise<void> {
    status.value = 'checking'
    try {
      await fetchPing({ locale: locale.value })
      status.value = 'ok'
    } catch (error) {
      status.value = 'error'
      showError(error)
    }
  }

  onMounted(check)

  return { status, check }
}
