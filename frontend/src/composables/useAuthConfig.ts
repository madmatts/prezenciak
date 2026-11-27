import { ref } from 'vue'

import { authApi } from '@/api/auth'
import type { AuthConfig } from '@/types/api'

const config = ref<AuthConfig | null>(null)
let loading: Promise<void> | null = null

/** Which login methods the backend offers (the Google button is hidden when Google isn't configured). */
export function useAuthConfig() {
  if (!config.value && !loading) {
    loading = authApi
      .config()
      .then((value) => {
        config.value = value
      })
      .catch(() => {
        config.value = { googleEnabled: false }
      })
      .finally(() => {
        loading = null
      })
  }
  return { config }
}
