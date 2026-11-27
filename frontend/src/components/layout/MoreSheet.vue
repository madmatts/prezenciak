<script setup lang="ts">
import { ChevronRight, LogOut, User } from '@lucide/vue'
import { useI18n } from 'vue-i18n'

import { MORE_ITEMS } from './navigation'

const emit = defineEmits<{ close: []; logout: [] }>()
const { t } = useI18n()
</script>

<template>
  <Teleport to="body">
    <div class="scrim" aria-hidden="true" @click="emit('close')"></div>
    <div
      class="sheet"
      role="dialog"
      aria-modal="true"
      aria-labelledby="more-title"
      @keydown.esc="emit('close')"
    >
      <span class="sheet-handle" aria-hidden="true"></span>
      <h2 id="more-title" class="t-h2">{{ t('nav.more') }}</h2>
      <nav class="rows" :aria-label="t('nav.more')">
        <RouterLink
          v-for="item in MORE_ITEMS"
          :key="item.name"
          :to="{ name: item.name }"
          class="row-item"
          @click="emit('close')"
        >
          <span class="lead"><component :is="item.icon" class="icon" aria-hidden="true" /></span>
          <span class="t-body flex-1 font-semibold">{{ t(item.labelKey) }}</span>
          <ChevronRight class="icon" aria-hidden="true" />
        </RouterLink>
        <RouterLink :to="{ name: 'profile' }" class="row-item" @click="emit('close')">
          <span class="lead"><User class="icon" aria-hidden="true" /></span>
          <span class="t-body flex-1 font-semibold">{{ t('nav.profile') }}</span>
          <ChevronRight class="icon" aria-hidden="true" />
        </RouterLink>
      </nav>
      <button
        type="button"
        class="btn btn-ghost btn-block justify-start"
        data-testid="logout"
        @click="emit('logout')"
      >
        <LogOut class="icon" aria-hidden="true" />{{ t('nav.logout') }}
      </button>
    </div>
  </Teleport>
</template>
