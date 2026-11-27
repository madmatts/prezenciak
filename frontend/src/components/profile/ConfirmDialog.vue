<script setup lang="ts">
import { useI18n } from 'vue-i18n'

import BaseDialog from '@/components/ui/BaseDialog.vue'

defineProps<{ title: string; text: string; confirmLabel: string; busy?: boolean }>()
const emit = defineEmits<{ close: []; confirm: [] }>()

const { t } = useI18n()
</script>

<template>
  <BaseDialog :title="title" @close="emit('close')">
    <p class="t-body">{{ text }}</p>
    <div class="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
      <button type="button" class="btn btn-ghost" @click="emit('close')">
        {{ t('common.cancel') }}
      </button>
      <button type="button" class="btn btn-danger" :disabled="busy" @click="emit('confirm')">
        {{ confirmLabel }}
      </button>
    </div>
  </BaseDialog>
</template>
