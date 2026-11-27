<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'

import TextField from '@/components/ui/TextField.vue'
import UserAvatar from '@/components/ui/UserAvatar.vue'
import { useApiErrorToast } from '@/composables/useApiErrorToast'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import type { Me } from '@/types/api'

const props = defineProps<{ user: Me }>()

const { t } = useI18n()
const auth = useAuthStore()
const toast = useToastStore()
const { showError } = useApiErrorToast()
const displayName = ref(props.user.displayName)
const error = ref<string | null>(null)
const saving = ref(false)

watch(
  () => props.user.displayName,
  (value) => (displayName.value = value),
)

const changed = computed(() => displayName.value.trim() !== props.user.displayName)

async function save(): Promise<void> {
  error.value = displayName.value.trim() ? null : 'auth.validation.displayNameRequired'
  if (error.value) {
    return
  }
  saving.value = true
  try {
    await auth.updateProfile({ displayName: displayName.value.trim() })
    toast.show('success', 'profile.saved')
  } catch (failure) {
    showError(failure)
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <section class="section-card" aria-labelledby="profile-details-title">
    <h2 id="profile-details-title" class="t-h3">{{ t('profile.details.title') }}</h2>
    <div class="flex items-center gap-4">
      <UserAvatar :name="user.displayName" size="lg" />
      <p class="t-caption">{{ t('profile.details.avatarHint') }}</p>
    </div>
    <form class="flex flex-col gap-4" novalidate @submit.prevent="save">
      <TextField
        id="profile-name"
        v-model="displayName"
        autocomplete="name"
        :maxlength="100"
        :label="t('profile.details.displayName')"
        :hint="t('profile.details.displayNameHint')"
        :error="error ? t(error) : undefined"
      />
      <div class="field">
        <span id="profile-email-label" class="label">{{ t('auth.fields.email') }}</span>
        <div
          class="input flex items-center gap-2 bg-surface-sunken"
          aria-labelledby="profile-email-label"
        >
          <span class="flex-1 truncate">{{ user.email }}</span>
          <span v-if="user.emailVerified" class="badge badge-success">{{
            t('profile.details.verified')
          }}</span>
          <span v-else class="badge badge-warning">{{ t('profile.details.notVerified') }}</span>
        </div>
        <span class="hint">{{ t('profile.details.emailHint') }}</span>
      </div>
      <div>
        <button type="submit" class="btn btn-primary" :disabled="!changed || saving">
          {{ t('profile.details.save') }}
        </button>
      </div>
    </form>
  </section>
</template>
