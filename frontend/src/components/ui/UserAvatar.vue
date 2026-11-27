<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{ name: string; size?: 'sm' | 'md' | 'lg' }>(), {
  size: 'md',
})

/** Initials instead of a photo (design P1 profile): first letters of the first two words. */
const initials = computed(() =>
  props.name
    .trim()
    .split(/\s+/)
    .slice(0, 2)
    .map((word) => word.charAt(0).toLocaleUpperCase())
    .join(''),
)
</script>

<template>
  <span
    class="avatar"
    :class="{ 'avatar-sm': size === 'sm', 'avatar-lg': size === 'lg' }"
    aria-hidden="true"
  >
    {{ initials }}
  </span>
</template>
