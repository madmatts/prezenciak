<script setup lang="ts">
import { appConfig } from '@/config'
import { storeToRefs } from 'pinia'
import { useI18n } from 'vue-i18n'

import UserAvatar from '@/components/ui/UserAvatar.vue'
import { useAuthStore } from '@/stores/auth'
import { NAV_ITEMS } from './navigation'

const { t } = useI18n()
const { user } = storeToRefs(useAuthStore())
</script>

<template>
  <nav class="sidebar sticky top-0 h-dvh" :aria-label="t('nav.main')">
    <RouterLink :to="{ name: 'items' }" class="mb-6 flex items-center gap-2 px-3 no-underline">
      <span class="wordmark text-2xl">{{ appConfig.appName }}</span>
    </RouterLink>
    <RouterLink
      v-for="item in NAV_ITEMS"
      :key="item.name"
      :to="{ name: item.name }"
      class="side-item"
    >
      <component :is="item.icon" class="icon" aria-hidden="true" />
      {{ t(item.labelKey) }}
    </RouterLink>
    <div class="flex-1"></div>
    <RouterLink v-if="user" :to="{ name: 'profile' }" class="side-item">
      <UserAvatar :name="user.displayName" size="sm" />
      <span class="flex min-w-0 flex-col">
        <span class="truncate">{{ user.displayName }}</span>
        <span class="t-caption">{{ t('nav.profile') }}</span>
      </span>
    </RouterLink>
  </nav>
</template>
