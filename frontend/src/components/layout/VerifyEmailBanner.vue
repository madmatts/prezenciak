<script setup lang="ts">
import { Mail, X } from '@lucide/vue'
import { storeToRefs } from 'pinia'
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'

import { authApi } from '@/api/auth'
import { useApiErrorToast } from '@/composables/useApiErrorToast'
import { useCooldown } from '@/composables/useCooldown'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'

const RESEND_COOLDOWN_S = 60

const { t } = useI18n()
const auth = useAuthStore()
const { user, needsVerification, verifyBannerDismissed } = storeToRefs(auth)
const toast = useToastStore()
const { showError } = useApiErrorToast()
const cooldown = useCooldown(RESEND_COOLDOWN_S)
const sending = ref(false)

const visible = computed(() => needsVerification.value && !verifyBannerDismissed.value)

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
  <div
    v-if="visible && user"
    class="banner banner-accent"
    role="region"
    :aria-label="t('auth.verify.bannerRegion')"
    data-testid="verify-banner"
  >
    <Mail class="icon" aria-hidden="true" />
    <div class="flex-1">
      <strong>{{ t('auth.verify.bannerTitle') }}</strong>
      <p>{{ t('auth.verify.bannerText', { email: user.email }) }}</p>
      <div class="actions">
        <button
          type="button"
          class="btn btn-secondary"
          :disabled="sending || cooldown.active.value"
          @click="resend"
        >
          {{
            cooldown.active.value
              ? t('auth.verify.resendIn', { time: cooldown.label.value })
              : t('auth.verify.resend')
          }}
        </button>
      </div>
    </div>
    <button
      type="button"
      class="btn btn-ghost btn-icon banner-close"
      :aria-label="t('auth.verify.bannerDismiss')"
      @click="auth.dismissVerifyBanner()"
    >
      <X class="icon" aria-hidden="true" />
    </button>
  </div>
</template>
