<script setup lang="ts">
import { Bell } from '@lucide/vue'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
</script>

<template>
  <!-- Unread count arrives with the notification center (Phase 7). -->
  <RouterLink
    :to="{ name: 'notifications' }"
    class="btn btn-ghost btn-icon relative"
    :aria-label="t('nav.notifications')"
  >
    <Bell class="icon" aria-hidden="true" />
  </RouterLink>
</template>
