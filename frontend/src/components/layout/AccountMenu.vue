<script setup lang="ts">
import { Bell, Languages, LogOut, User } from '@lucide/vue'
import { onBeforeUnmount, onMounted, ref, useTemplateRef } from 'vue'
import { useI18n } from 'vue-i18n'

import UserAvatar from '@/components/ui/UserAvatar.vue'
import { useLanguage } from '@/composables/useLanguage'
import type { Me } from '@/types/api'

defineProps<{ user: Me }>()
const emit = defineEmits<{ logout: [] }>()

const { t } = useI18n()
const { locale, changeLanguage } = useLanguage()
const open = ref(false)
const root = useTemplateRef<HTMLElement>('root')

function toggleLanguage(): void {
  void changeLanguage(locale.value === 'pl' ? 'en' : 'pl')
}

function onDocumentClick(event: MouseEvent): void {
  if (open.value && root.value && !root.value.contains(event.target as Node)) {
    open.value = false
  }
}

onMounted(() => document.addEventListener('click', onDocumentClick))
onBeforeUnmount(() => document.removeEventListener('click', onDocumentClick))
</script>

<template>
  <div ref="root" class="relative" @keydown.esc="open = false">
    <button
      type="button"
      class="btn btn-ghost btn-icon"
      :aria-label="t('nav.accountMenu')"
      aria-haspopup="menu"
      :aria-expanded="open"
      data-testid="account-menu"
      @click="open = !open"
    >
      <UserAvatar :name="user.displayName" size="sm" />
    </button>
    <div
      v-if="open"
      class="menu absolute right-0 z-40 mt-2"
      role="menu"
      :aria-label="t('nav.accountMenu')"
    >
      <div class="flex items-center gap-3 p-3">
        <UserAvatar :name="user.displayName" />
        <div class="flex min-w-0 flex-col">
          <span class="t-label truncate">{{ user.displayName }}</span>
          <span class="t-caption truncate">{{ user.email }}</span>
        </div>
      </div>
      <div class="menu-sep"></div>
      <RouterLink
        :to="{ name: 'profile' }"
        class="menu-item no-underline"
        role="menuitem"
        @click="open = false"
      >
        <User class="icon" aria-hidden="true" />{{ t('nav.profile') }}
      </RouterLink>
      <RouterLink
        :to="{ name: 'notifications' }"
        class="menu-item no-underline"
        role="menuitem"
        @click="open = false"
      >
        <Bell class="icon" aria-hidden="true" />{{ t('nav.notifications') }}
      </RouterLink>
      <button type="button" class="menu-item" role="menuitem" @click="toggleLanguage">
        <Languages class="icon" aria-hidden="true" />
        {{ t('nav.languageCurrent', { language: t(`common.language.${locale}`) }) }}
      </button>
      <div class="menu-sep"></div>
      <button type="button" class="menu-item" role="menuitem" @click="emit('logout')">
        <LogOut class="icon" aria-hidden="true" />{{ t('nav.logout') }}
      </button>
    </div>
  </div>
</template>
