<script setup lang="ts">
import { MailCheck } from '@lucide/vue'
import { storeToRefs } from 'pinia'
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'

import { authApi } from '@/api/auth'
import AuthLayout from '@/components/layout/AuthLayout.vue'
import ResultPanel from '@/components/ui/ResultPanel.vue'
import { useApiErrorToast } from '@/composables/useApiErrorToast'
import { useCooldown } from '@/composables/useCooldown'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'

const { t } = useI18n()
const { user } = storeToRefs(useAuthStore())
const toast = useToastStore()
const { showError } = useApiErrorToast()
const cooldown = useCooldown(60)
const sending = ref(false)

// The verification email was just sent by the registration.
onMounted(cooldown.start)

async function resend(): Promise<void> {
  sending.value = true
  try {
    await authApi.resendVerification()
    toast.show('info', 'auth.verify.resent', { email: user.value?.email })
    cooldown.start()
  } catch (error) {
    showError(error)
  } finally {
    sending.value = false
  }
}
</script>

<template>
  <AuthLayout>
    <ResultPanel :icon="MailCheck" :title="t('auth.inbox.title')">
      <i18n-t keypath="auth.inbox.text" tag="p" class="t-body-lg text-text-muted" scope="global">
        <template #email>
          <strong class="text-text">{{ user?.email }}</strong>
        </template>
      </i18n-t>
      <p class="t-caption">{{ t('auth.inbox.spamHint') }}</p>
      <template #actions>
        <button
          type="button"
          class="btn btn-secondary btn-lg btn-block"
          :disabled="sending || cooldown.active.value"
          @click="resend"
        >
          {{ t('auth.verify.resend') }}
        </button>
        <span v-if="cooldown.active.value" class="t-caption" aria-live="polite">
          {{ t('auth.inbox.resendIn', { time: cooldown.label.value }) }}
        </span>
        <RouterLink :to="{ name: 'items' }" class="btn btn-ghost btn-block">
          {{ t('auth.inbox.goToApp') }}
        </RouterLink>
      </template>
    </ResultPanel>
  </AuthLayout>
</template>
