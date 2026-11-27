<script setup lang="ts">
import { onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'

import { safeRedirect } from '@/router'
import { useAuthStore } from '@/stores/auth'

const { t } = useI18n()
const router = useRouter()
const auth = useAuthStore()

/** Landing page after Google login: load the session, then continue where the user started. */
onMounted(async () => {
  await auth.refresh().catch(() => undefined)
  const returnTo = safeRedirect(sessionStorage.getItem('wishka.oauthReturnTo'))
  sessionStorage.removeItem('wishka.oauthReturnTo')
  await router.replace(auth.isAuthenticated ? (returnTo ?? { name: 'items' }) : { name: 'login' })
})
</script>

<template>
  <main class="flex min-h-dvh items-center justify-center" role="status">
    <span class="spinner text-primary" aria-hidden="true"></span>
    <span class="sr-only">{{ t('common.loading') }}</span>
  </main>
</template>
