<script setup lang="ts">
import { useI18n } from 'vue-i18n'

import { GOOGLE_LOGIN_URL } from '@/api/auth'
import GoogleLogo from './GoogleLogo.vue'

const props = withDefaults(
  defineProps<{
    /** Where to go after a successful Google login (remembered across the redirect). */
    returnTo?: string
  }>(),
  { returnTo: undefined },
)

const { t } = useI18n()

function rememberReturnPath(): void {
  if (props.returnTo) {
    sessionStorage.setItem('wishka.oauthReturnTo', props.returnTo)
  } else {
    sessionStorage.removeItem('wishka.oauthReturnTo')
  }
}
</script>

<template>
  <!-- Google Identity branding: white pill, official "G", approved text (design P0). -->
  <a class="gbtn w-full no-underline" :href="GOOGLE_LOGIN_URL" @click="rememberReturnPath">
    <GoogleLogo />
    {{ t('auth.google.continue') }}
  </a>
</template>
