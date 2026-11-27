<script setup lang="ts">
import { KeyRound } from '@lucide/vue'
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'

import { authApi, GOOGLE_LOGIN_URL } from '@/api/auth'
import GoogleLogo from '@/components/ui/GoogleLogo.vue'
import { useApiErrorToast } from '@/composables/useApiErrorToast'
import { useAuthConfig } from '@/composables/useAuthConfig'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import type { Me } from '@/types/api'
import ChangePasswordDialog from './ChangePasswordDialog.vue'
import ConfirmDialog from './ConfirmDialog.vue'

/** Sign-in methods (D-38): at least one must remain; the last one can't be removed. */
const props = defineProps<{ user: Me }>()

const { t } = useI18n()
const auth = useAuthStore()
const toast = useToastStore()
const { showError } = useApiErrorToast()
const { config } = useAuthConfig()

const passwordDialog = ref(false)
const confirming = ref<'password' | 'google' | null>(null)
const busy = ref(false)

const google = computed(() => props.user.linkedLogins.find((login) => login.provider === 'GOOGLE'))
const methodCount = computed(
  () => (props.user.hasPassword ? 1 : 0) + props.user.linkedLogins.length,
)
const isLastMethod = computed(() => methodCount.value <= 1)
const googleCaption = computed(() =>
  google.value
    ? t('profile.signIn.googleConnected', { email: google.value.email ?? '' })
    : t('profile.signIn.googleNotConnected'),
)

function connectGoogle(): void {
  sessionStorage.setItem('wishka.oauthReturnTo', '/profile')
  window.location.assign(GOOGLE_LOGIN_URL)
}

async function onPasswordSaved(): Promise<void> {
  passwordDialog.value = false
  toast.show('success', 'profile.signIn.passwordSaved')
  await auth.refresh()
}

async function confirmRemoval(): Promise<void> {
  busy.value = true
  try {
    if (confirming.value === 'password') {
      await authApi.removePassword()
      toast.show('success', 'profile.signIn.passwordRemoved')
    } else {
      await authApi.unlinkGoogle()
      toast.show('success', 'profile.signIn.googleUnlinked')
    }
    await auth.refresh()
  } catch (error) {
    showError(error)
  } finally {
    busy.value = false
    confirming.value = null
  }
}
</script>

<template>
  <section class="section-card" aria-labelledby="profile-signin-title">
    <div class="flex flex-col gap-1">
      <h2 id="profile-signin-title" class="t-h3">{{ t('profile.signIn.title') }}</h2>
      <p class="t-caption">{{ t('profile.signIn.atLeastOne') }}</p>
    </div>
    <div class="rows">
      <div class="row-item">
        <span class="lead"><KeyRound class="icon" aria-hidden="true" /></span>
        <div class="flex flex-1 flex-col">
          <span class="t-label">{{ t('auth.fields.password') }}</span>
          <span class="t-caption">
            {{
              user.hasPassword
                ? t('profile.signIn.passwordSet')
                : t('profile.signIn.passwordNotSet')
            }}
          </span>
        </div>
        <div class="flex gap-1">
          <button type="button" class="btn btn-secondary" @click="passwordDialog = true">
            {{ user.hasPassword ? t('profile.signIn.change') : t('profile.signIn.setPassword') }}
          </button>
          <button
            v-if="user.hasPassword"
            type="button"
            class="btn btn-ghost"
            :disabled="isLastMethod"
            @click="confirming = 'password'"
          >
            {{ t('profile.signIn.remove') }}
          </button>
        </div>
      </div>
      <div class="row-item">
        <span class="lead" aria-hidden="true"><GoogleLogo /></span>
        <div class="flex min-w-0 flex-1 flex-col">
          <span class="t-label">{{ t('profile.signIn.google') }}</span>
          <span class="t-caption truncate">{{ googleCaption }}</span>
        </div>
        <button
          v-if="google"
          type="button"
          class="btn btn-ghost"
          :disabled="isLastMethod"
          @click="confirming = 'google'"
        >
          {{ t('profile.signIn.unlink') }}
        </button>
        <button
          v-else-if="config?.googleEnabled"
          type="button"
          class="btn btn-secondary"
          @click="connectGoogle"
        >
          {{ t('profile.signIn.connect') }}
        </button>
      </div>
    </div>
    <div v-if="isLastMethod" class="banner banner-info w-full" data-testid="last-method">
      <p>
        {{ google ? t('profile.signIn.lastMethodGoogle') : t('profile.signIn.lastMethodPassword') }}
      </p>
    </div>

    <ChangePasswordDialog
      v-if="passwordDialog"
      :has-password="user.hasPassword"
      @close="passwordDialog = false"
      @saved="onPasswordSaved"
    />
    <ConfirmDialog
      v-if="confirming"
      :title="
        confirming === 'password'
          ? t('profile.signIn.removePasswordTitle')
          : t('profile.signIn.unlinkGoogleTitle')
      "
      :text="
        confirming === 'password'
          ? t('profile.signIn.removePasswordText')
          : t('profile.signIn.unlinkGoogleText')
      "
      :confirm-label="
        confirming === 'password' ? t('profile.signIn.remove') : t('profile.signIn.unlink')
      "
      :busy="busy"
      @close="confirming = null"
      @confirm="confirmRemoval"
    />
  </section>
</template>
