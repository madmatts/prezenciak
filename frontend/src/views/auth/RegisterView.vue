<script setup lang="ts">
import { CircleAlert } from '@lucide/vue'
import { computed, nextTick, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'

import AuthLayout from '@/components/layout/AuthLayout.vue'
import FormAlert from '@/components/ui/FormAlert.vue'
import GoogleButton from '@/components/ui/GoogleButton.vue'
import OrDivider from '@/components/ui/OrDivider.vue'
import PasswordField from '@/components/ui/PasswordField.vue'
import TextField from '@/components/ui/TextField.vue'
import { useAuthConfig } from '@/composables/useAuthConfig'
import { useFormErrors } from '@/composables/useFormErrors'
import { useLocale } from '@/composables/useLocale'
import { PASSWORD_MIN_LENGTH } from '@/composables/usePasswordStrength'
import { useAuthStore } from '@/stores/auth'

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

const { t } = useI18n()
const router = useRouter()
const auth = useAuthStore()
const { locale } = useLocale()
const { config } = useAuthConfig()

const form = reactive({ displayName: '', email: '', password: '', acceptPrivacyPolicy: false })
const submitting = ref(false)
const { fieldErrors, formError, errorCount, setField, reset, applyApiError } = useFormErrors([
  'displayName',
  'email',
  'password',
  'acceptPrivacyPolicy',
] as const)

const formAlertTitle = computed(() =>
  errorCount.value > 0
    ? t('auth.register.fixFields', { count: errorCount.value }, errorCount.value)
    : formError.value
      ? t(`errors.${formError.value}`)
      : null,
)

function validate(): boolean {
  setField('displayName', form.displayName.trim() ? null : 'auth.validation.displayNameRequired')
  setField(
    'email',
    !form.email.trim()
      ? 'auth.validation.emailRequired'
      : EMAIL_PATTERN.test(form.email.trim())
        ? null
        : 'auth.validation.emailInvalid',
  )
  setField(
    'password',
    form.password.length >= PASSWORD_MIN_LENGTH ? null : 'auth.validation.passwordTooShort',
  )
  setField(
    'acceptPrivacyPolicy',
    form.acceptPrivacyPolicy ? null : 'auth.validation.consentRequired',
  )
  return errorCount.value === 0
}

async function submit(): Promise<void> {
  reset()
  if (!validate()) {
    await focusAlert()
    return
  }
  submitting.value = true
  try {
    await auth.register({
      displayName: form.displayName.trim(),
      email: form.email.trim(),
      password: form.password,
      acceptPrivacyPolicy: form.acceptPrivacyPolicy,
      locale: locale.value,
    })
    await router.replace({ name: 'check-inbox' })
  } catch (error) {
    const code = applyApiError(error, {
      'password.Size': 'auth.validation.passwordTooShort',
      'acceptPrivacyPolicy.AssertTrue': 'auth.validation.consentRequired',
    })
    if (code === 'EMAIL_TAKEN') {
      formError.value = null
      setField('email', 'auth.register.emailTaken')
    }
    await focusAlert()
  } finally {
    submitting.value = false
  }
}

async function focusAlert(): Promise<void> {
  await nextTick()
  document.querySelector<HTMLElement>('[data-testid="register-error"]')?.focus()
}
</script>

<template>
  <AuthLayout with-pitch>
    <h1 class="t-h1">{{ t('auth.register.title') }}</h1>

    <FormAlert
      v-if="formAlertTitle"
      :title="formAlertTitle"
      :text="errorCount > 0 ? t('auth.register.fixFieldsText') : undefined"
      data-testid="register-error"
    />

    <template v-if="config?.googleEnabled">
      <GoogleButton />
      <OrDivider />
    </template>

    <form class="flex flex-col gap-4" novalidate @submit.prevent="submit">
      <TextField
        id="register-name"
        v-model="form.displayName"
        autocomplete="name"
        :maxlength="100"
        :label="t('auth.fields.displayName')"
        :hint="t('auth.register.displayNameHint')"
        :error="fieldErrors.displayName && t(fieldErrors.displayName)"
      />
      <TextField
        id="register-email"
        v-model="form.email"
        type="email"
        autocomplete="email"
        :label="t('auth.fields.email')"
        :error="fieldErrors.email && t(fieldErrors.email)"
      />
      <PasswordField
        id="register-password"
        v-model="form.password"
        autocomplete="new-password"
        show-strength
        :label="t('auth.fields.password')"
        :hint="t('auth.password.rule')"
        :error="fieldErrors.password && t(fieldErrors.password)"
      />
      <div class="flex flex-col gap-1">
        <label class="check">
          <input
            v-model="form.acceptPrivacyPolicy"
            type="checkbox"
            :aria-invalid="fieldErrors.acceptPrivacyPolicy ? 'true' : undefined"
            data-testid="register-consent"
          />
          <i18n-t keypath="auth.register.consent" tag="span" class="t-body" scope="global">
            <template #policy>
              <RouterLink :to="{ name: 'privacy' }" target="_blank">{{
                t('auth.register.policyLink')
              }}</RouterLink>
            </template>
          </i18n-t>
        </label>
        <span v-if="fieldErrors.acceptPrivacyPolicy" class="error-text">
          <CircleAlert class="icon icon-sm" aria-hidden="true" />{{
            t(fieldErrors.acceptPrivacyPolicy)
          }}
        </span>
      </div>
      <button type="submit" class="btn btn-primary btn-lg btn-block" :disabled="submitting">
        <span v-if="submitting" class="spinner" aria-hidden="true"></span>
        {{ submitting ? t('auth.register.submitting') : t('auth.register.submit') }}
      </button>
    </form>

    <p class="t-body text-center">
      {{ t('auth.register.haveAccount') }}
      <RouterLink :to="{ name: 'login' }">{{ t('auth.login.submit') }}</RouterLink>
    </p>
  </AuthLayout>
</template>
