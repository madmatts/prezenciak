<script setup lang="ts">
import { BadgeCheck, Clock } from '@lucide/vue'
import { storeToRefs } from 'pinia'
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'

import { authApi } from '@/api/auth'
import AuthLayout from '@/components/layout/AuthLayout.vue'
import ResultPanel from '@/components/ui/ResultPanel.vue'
import { useApiErrorToast } from '@/composables/useApiErrorToast'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'

type State = 'verifying' | 'verified' | 'expired'

const { t } = useI18n()
const route = useRoute()
const auth = useAuthStore()
const { user, isAuthenticated } = storeToRefs(auth)
const toast = useToastStore()
const { showError } = useApiErrorToast()
const state = ref<State>('verifying')
const sending = ref(false)

onMounted(async () => {
  const token = typeof route.query.token === 'string' ? route.query.token : ''
  try {
    await authApi.verifyEmail(token)
    state.value = 'verified'
    if (isAuthenticated.value) {
      await auth.refresh()
    }
  } catch {
    state.value = 'expired'
  }
})

async function sendNewLink(): Promise<void> {
  sending.value = true
  try {
    await authApi.resendVerification()
    toast.show('info', 'auth.verify.resent', { email: user.value?.email })
  } catch (error) {
    showError(error)
  } finally {
    sending.value = false
  }
}
</script>

<template>
  <AuthLayout>
    <p v-if="state === 'verifying'" class="flex items-center gap-3" role="status">
      <span class="spinner text-primary" aria-hidden="true"></span>{{ t('auth.verify.verifying') }}
    </p>

    <ResultPanel
      v-else-if="state === 'verified'"
      :icon="BadgeCheck"
      tone="success"
      :title="t('auth.verify.successTitle')"
    >
      <p class="t-body-lg text-text-muted">{{ t('auth.verify.successText') }}</p>
      <template #actions>
        <RouterLink
          :to="{ name: isAuthenticated ? 'items' : 'login' }"
          class="btn btn-primary btn-lg btn-block"
        >
          {{ t('auth.verify.start') }}
        </RouterLink>
      </template>
    </ResultPanel>

    <ResultPanel v-else :icon="Clock" tone="warning" :title="t('auth.verify.expiredTitle')">
      <p class="t-body-lg text-text-muted">{{ t('auth.verify.expiredText') }}</p>
      <p v-if="user" class="t-caption">{{ t('auth.verify.newLinkTo', { email: user.email }) }}</p>
      <template #actions>
        <button
          v-if="isAuthenticated"
          type="button"
          class="btn btn-primary btn-lg btn-block"
          :disabled="sending"
          @click="sendNewLink"
        >
          {{ t('auth.verify.sendNewLink') }}
        </button>
        <RouterLink
          v-else
          :to="{ name: 'login', query: { redirect: '/items' } }"
          class="btn btn-primary btn-lg btn-block"
        >
          {{ t('auth.verify.loginToResend') }}
        </RouterLink>
        <RouterLink v-if="!isAuthenticated" :to="{ name: 'login' }" class="btn btn-ghost btn-block">
          {{ t('auth.backToLogin') }}
        </RouterLink>
      </template>
    </ResultPanel>
  </AuthLayout>
</template>
