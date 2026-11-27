<script setup lang="ts">
import { Ellipsis } from '@lucide/vue'
import { useI18n } from 'vue-i18n'

import { BOTTOM_NAV_ITEMS } from './navigation'

defineProps<{ moreOpen: boolean }>()
const emit = defineEmits<{ openMore: [] }>()

const { t } = useI18n()
</script>

<template>
  <nav class="bottom-nav fixed inset-x-0 bottom-0 z-30" :aria-label="t('nav.main')">
    <RouterLink
      v-for="item in BOTTOM_NAV_ITEMS"
      :key="item.name"
      :to="{ name: item.name }"
      class="bnav-item"
    >
      <span class="pill"><component :is="item.icon" class="icon" aria-hidden="true" /></span>
      {{ t(item.shortLabelKey) }}
    </RouterLink>
    <button
      type="button"
      class="bnav-item border-0 bg-transparent"
      :aria-current="moreOpen ? 'page' : undefined"
      :aria-expanded="moreOpen"
      aria-haspopup="dialog"
      @click="emit('openMore')"
    >
      <span class="pill"><Ellipsis class="icon" aria-hidden="true" /></span>
      {{ t('nav.more') }}
    </button>
  </nav>
</template>
