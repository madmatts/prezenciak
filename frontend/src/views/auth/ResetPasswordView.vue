<script setup lang="ts">
import { Clock } from '@lucide/vue'
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'

import { authApi } from '@/api/auth'
import AuthLayout from '@/components/layout/AuthLayout.vue'
import FormAlert from '@/components/ui/FormAlert.vue'
import PasswordField from '@/components/ui/PasswordField.vue'
import ResultPanel from '@/components/ui/ResultPanel.vue'
import { useFormErrors } from '@/composables/useFormErrors'
import { PASSWORD_MIN_LENGTH } from '@/composables/usePasswordStrength'
import { useAuthStore } from '@/stores/auth'

type State = 'checking' | 'ready' | 'expired'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const token = typeof route.query.token === 'string' ? route.query.token : ''
const state = ref<State>('checking')
const accountEmail = ref('')
const form = reactive({ password: '', repeat: '' })
const submitting = ref(false)
const { fieldErrors, formError, setField, reset, applyApiError } = useFormErrors([
  'password',
  'repeat',
] as const)

onMounted(async () => {
  try {
    accountEmail.value = (await authApi.checkPasswordReset(token)).email
    state.value = 'ready'
  } catch {
    state.value = 'expired'
  }
})

async function save(): Promise<void> {
  reset()
  setField(
    'password',
    form.password.length >= PASSWORD_MIN_LENGTH ? null : 'auth.validation.passwordTooShort',
  )
  setField('repeat', form.password === form.repeat ? null : 'auth.validation.passwordsDiffer')
  if (fieldErrors.value.password || fieldErrors.value.repeat) {
    return
  }
  submitting.value = true
  try {
    await authApi.confirmPasswordReset(token, form.password)
    auth.clear()
    await router.replace({ name: 'login', query: { reset: 'done' } })
  } catch (error) {
    if (applyApiError(error) === 'TOKEN_INVALID_OR_EXPIRED') {
      state.value = 'expired'
    }
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <AuthLayout>
    <p v-if="state === 'checking'" class="flex items-center gap-3" role="status">
      <span class="spinner text-primary" aria-hidden="true"></span>{{ t('common.loading') }}
    </p>

    <ResultPanel
      v-else-if="state === 'expired'"
      :icon="Clock"
      tone="warning"
      :title="t('auth.verify.expiredTitle')"
    >
      <p class="t-body-lg text-text-muted">{{ t('auth.reset.expiredText') }}</p>
      <template #actions>
        <RouterLink :to="{ name: 'forgot-password' }" class="btn btn-primary btn-lg btn-block">
          {{ t('auth.verify.sendNewLink') }}
        </RouterLink>
        <RouterLink :to="{ name: 'login' }" class="btn btn-ghost btn-block">
          {{ t('auth.backToLogin') }}
        </RouterLink>
      </template>
    </ResultPanel>

    <template v-else>
      <div class="flex flex-col gap-2">
        <h1 class="t-h1">{{ t('auth.reset.title') }}</h1>
        <p class="t-body text-text-muted">
          {{ t('auth.reset.forAccount', { email: accountEmail }) }}
        </p>
      </div>
      <FormAlert v-if="formError" :title="t(`errors.${formError}`)" />
      <form class="flex flex-col gap-4" novalidate @submit.prevent="save">
        <PasswordField
          id="reset-password"
          v-model="form.password"
          autocomplete="new-password"
          show-strength
          :label="t('auth.fields.newPassword')"
          :hint="t('auth.password.rule')"
          :error="fieldErrors.password && t(fieldErrors.password)"
        />
        <PasswordField
          id="reset-repeat"
          v-model="form.repeat"
          autocomplete="new-password"
          :label="t('auth.fields.repeatPassword')"
          :error="fieldErrors.repeat && t(fieldErrors.repeat)"
        />
        <button type="submit" class="btn btn-primary btn-lg btn-block" :disabled="submitting">
          <span v-if="submitting" class="spinner" aria-hidden="true"></span
          >{{ t('auth.reset.submit') }}
        </button>
        <p class="t-caption">{{ t('auth.reset.logoutNote') }}</p>
      </form>
    </template>
  </AuthLayout>
</template>
