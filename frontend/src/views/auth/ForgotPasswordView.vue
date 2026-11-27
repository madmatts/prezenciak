<script setup lang="ts">
import { MailCheck } from '@lucide/vue'
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'

import { authApi } from '@/api/auth'
import AuthLayout from '@/components/layout/AuthLayout.vue'
import FormAlert from '@/components/ui/FormAlert.vue'
import ResultPanel from '@/components/ui/ResultPanel.vue'
import TextField from '@/components/ui/TextField.vue'
import { useFormErrors } from '@/composables/useFormErrors'

const { t } = useI18n()
const email = ref('')
const sent = ref(false)
const submitting = ref(false)
const { fieldErrors, formError, setField, reset, applyApiError } = useFormErrors(['email'] as const)

async function send(): Promise<void> {
  reset()
  if (!email.value.trim()) {
    setField('email', 'auth.validation.emailRequired')
    return
  }
  submitting.value = true
  try {
    await authApi.requestPasswordReset(email.value.trim())
    sent.value = true
  } catch (error) {
    applyApiError(error)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <AuthLayout back-to-login>
    <ResultPanel v-if="sent" :icon="MailCheck" :title="t('auth.inbox.title')">
      <i18n-t
        keypath="auth.forgot.sentText"
        tag="p"
        class="t-body-lg text-text-muted"
        scope="global"
      >
        <template #email>
          <strong class="text-text">{{ email }}</strong>
        </template>
      </i18n-t>
      <p class="t-caption">{{ t('auth.forgot.sentHint') }}</p>
      <template #actions>
        <RouterLink :to="{ name: 'login' }" class="btn btn-primary btn-lg btn-block">
          {{ t('auth.backToLogin') }}
        </RouterLink>
        <button type="button" class="btn btn-ghost btn-block" :disabled="submitting" @click="send">
          {{ t('auth.verify.resend') }}
        </button>
      </template>
    </ResultPanel>

    <template v-else>
      <div class="flex flex-col gap-2">
        <h1 class="t-h1">{{ t('auth.forgot.title') }}</h1>
        <p class="t-body text-text-muted">{{ t('auth.forgot.text') }}</p>
      </div>
      <FormAlert v-if="formError" :title="t(`errors.${formError}`)" />
      <form class="flex flex-col gap-4" novalidate @submit.prevent="send">
        <TextField
          id="forgot-email"
          v-model="email"
          type="email"
          autocomplete="email"
          :label="t('auth.fields.email')"
          :error="fieldErrors.email && t(fieldErrors.email)"
        />
        <button type="submit" class="btn btn-primary btn-lg btn-block" :disabled="submitting">
          <span v-if="submitting" class="spinner" aria-hidden="true"></span
          >{{ t('auth.forgot.submit') }}
        </button>
      </form>
      <i18n-t keypath="auth.forgot.googleHint" tag="p" class="t-caption" scope="global">
        <template #link>
          <RouterLink :to="{ name: 'login' }">{{ t('auth.forgot.googleHintLink') }}</RouterLink>
        </template>
      </i18n-t>
    </template>
  </AuthLayout>
</template>
