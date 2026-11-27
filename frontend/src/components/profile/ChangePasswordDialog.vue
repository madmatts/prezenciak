<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'

import { authApi } from '@/api/auth'
import BaseDialog from '@/components/ui/BaseDialog.vue'
import FormAlert from '@/components/ui/FormAlert.vue'
import PasswordField from '@/components/ui/PasswordField.vue'
import { useFormErrors } from '@/composables/useFormErrors'
import { PASSWORD_MIN_LENGTH } from '@/composables/usePasswordStrength'

/** Sets a first password (Google-only account) or changes the current one. */
const props = defineProps<{ hasPassword: boolean }>()
const emit = defineEmits<{ close: []; saved: [] }>()

const { t } = useI18n()
const form = reactive({ current: '', password: '', repeat: '' })
const saving = ref(false)
const { fieldErrors, formError, setField, reset, applyApiError } = useFormErrors([
  'current',
  'password',
  'repeat',
] as const)

async function save(): Promise<void> {
  reset()
  setField(
    'current',
    !props.hasPassword || form.current ? null : 'auth.validation.passwordRequired',
  )
  setField(
    'password',
    form.password.length >= PASSWORD_MIN_LENGTH ? null : 'auth.validation.passwordTooShort',
  )
  setField('repeat', form.password === form.repeat ? null : 'auth.validation.passwordsDiffer')
  if (fieldErrors.value.current || fieldErrors.value.password || fieldErrors.value.repeat) {
    return
  }
  saving.value = true
  try {
    await authApi.changePassword(form.password, props.hasPassword ? form.current : undefined)
    emit('saved')
  } catch (error) {
    if (applyApiError(error) === 'CURRENT_PASSWORD_INVALID') {
      formError.value = null
      setField('current', 'errors.CURRENT_PASSWORD_INVALID')
    }
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <BaseDialog
    :title="
      hasPassword ? t('profile.signIn.changePasswordTitle') : t('profile.signIn.setPasswordTitle')
    "
    @close="emit('close')"
  >
    <FormAlert v-if="formError" :title="t(`errors.${formError}`)" />
    <form class="flex flex-col gap-4" novalidate @submit.prevent="save">
      <PasswordField
        v-if="hasPassword"
        id="password-current"
        v-model="form.current"
        autocomplete="current-password"
        :label="t('auth.fields.currentPassword')"
        :error="fieldErrors.current && t(fieldErrors.current)"
      />
      <PasswordField
        id="password-new"
        v-model="form.password"
        autocomplete="new-password"
        show-strength
        :label="t('auth.fields.newPassword')"
        :hint="t('auth.password.rule')"
        :error="fieldErrors.password && t(fieldErrors.password)"
      />
      <PasswordField
        id="password-repeat"
        v-model="form.repeat"
        autocomplete="new-password"
        :label="t('auth.fields.repeatPassword')"
        :error="fieldErrors.repeat && t(fieldErrors.repeat)"
      />
      <button type="submit" class="btn btn-primary btn-block" :disabled="saving">
        <span v-if="saving" class="spinner" aria-hidden="true"></span>{{ t('auth.reset.submit') }}
      </button>
      <p v-if="hasPassword" class="t-caption">{{ t('profile.signIn.otherDevicesNote') }}</p>
    </form>
  </BaseDialog>
</template>
