<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'

import AuthLayout from '@/components/layout/AuthLayout.vue'
import FormAlert from '@/components/ui/FormAlert.vue'
import GoogleButton from '@/components/ui/GoogleButton.vue'
import OrDivider from '@/components/ui/OrDivider.vue'
import PasswordField from '@/components/ui/PasswordField.vue'
import TextField from '@/components/ui/TextField.vue'
import { useAuthConfig } from '@/composables/useAuthConfig'
import { useFormErrors } from '@/composables/useFormErrors'
import { safeRedirect } from '@/router'
import { useAuthStore } from '@/stores/auth'

const { t, te } = useI18n()
const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const { config } = useAuthConfig()

const form = reactive({ email: '', password: '' })
const submitting = ref(false)
const { fieldErrors, formError, setField, reset, applyApiError } = useFormErrors([
  'email',
  'password',
] as const)

const passwordChanged = computed(() => route.query.reset === 'done')
const oauthError = computed(() =>
  typeof route.query.oauthError === 'string' ? route.query.oauthError : null,
)

const alert = computed(() => {
  const code = formError.value ?? oauthError.value
  if (!code) {
    return null
  }
  if (code === 'INVALID_CREDENTIALS') {
    return { title: t('auth.login.failedTitle'), text: t('auth.login.failedText') }
  }
  const key = te(`errors.${code}`) ? `errors.${code}` : 'auth.login.oauthFailed'
  return { title: t('auth.login.failedTitle'), text: t(key) }
})

function validate(): boolean {
  setField('email', form.email.trim() ? null : 'auth.validation.emailRequired')
  setField('password', form.password ? null : 'auth.validation.passwordRequired')
  return !fieldErrors.value.email && !fieldErrors.value.password
}

async function submit(): Promise<void> {
  reset()
  if (!validate()) {
    return
  }
  submitting.value = true
  try {
    await auth.login(form.email.trim(), form.password)
    await router.replace(safeRedirect(route.query.redirect) ?? { name: 'items' })
  } catch (error) {
    if (applyApiError(error) === 'INVALID_CREDENTIALS') {
      form.password = ''
      setField('password', 'auth.login.retypePassword')
    }
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <AuthLayout with-pitch>
    <div class="flex flex-col gap-2">
      <h1 class="t-h1">{{ t('auth.login.title') }}</h1>
      <p class="text-text-muted">{{ t('auth.login.subtitle') }}</p>
    </div>

    <FormAlert
      v-if="passwordChanged && !alert"
      kind="success"
      :title="t('auth.reset.doneTitle')"
      :text="t('auth.reset.doneText')"
    />
    <FormAlert v-if="alert" :title="alert.title" :text="alert.text" data-testid="login-error" />

    <template v-if="config?.googleEnabled">
      <GoogleButton :return-to="safeRedirect(route.query.redirect)" />
      <OrDivider />
    </template>

    <form class="flex flex-col gap-4" novalidate @submit.prevent="submit">
      <TextField
        id="login-email"
        v-model="form.email"
        type="email"
        autocomplete="email"
        :label="t('auth.fields.email')"
        :error="fieldErrors.email && t(fieldErrors.email)"
      />
      <PasswordField
        id="login-password"
        v-model="form.password"
        autocomplete="current-password"
        :label="t('auth.fields.password')"
        :error="fieldErrors.password && t(fieldErrors.password)"
      >
        <template #label>
          <div class="flex items-center justify-between gap-2">
            <label class="label" for="login-password">{{ t('auth.fields.password') }}</label>
            <RouterLink :to="{ name: 'forgot-password' }" class="t-label">
              {{ t('auth.login.forgot') }}
            </RouterLink>
          </div>
        </template>
      </PasswordField>
      <button type="submit" class="btn btn-primary btn-lg btn-block" :disabled="submitting">
        <span v-if="submitting" class="spinner" aria-hidden="true"></span>
        {{ submitting ? t('auth.login.submitting') : t('auth.login.submit') }}
      </button>
    </form>

    <p class="t-body text-center">
      {{ t('auth.login.noAccount') }}
      <RouterLink :to="{ name: 'register' }">{{ t('auth.login.createAccount') }}</RouterLink>
    </p>
  </AuthLayout>
</template>
