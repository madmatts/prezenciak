<script setup lang="ts">
import { CircleAlert, CircleCheck, Info } from '@lucide/vue'
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    kind?: 'danger' | 'success' | 'info'
    title: string
    text?: string
  }>(),
  { kind: 'danger', text: undefined },
)

const icon = computed(() =>
  props.kind === 'success' ? CircleCheck : props.kind === 'info' ? Info : CircleAlert,
)
</script>

<template>
  <div
    class="banner"
    :class="`banner-${kind}`"
    :role="kind === 'danger' ? 'alert' : 'status'"
    tabindex="-1"
  >
    <component :is="icon" class="icon" aria-hidden="true" />
    <div class="flex-1">
      <strong>{{ title }}</strong>
      <p v-if="text">{{ text }}</p>
    </div>
  </div>
</template>
