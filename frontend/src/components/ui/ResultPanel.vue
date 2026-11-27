<script setup lang="ts">
import type { Component } from 'vue'

withDefaults(
  defineProps<{
    icon: Component
    tone?: 'primary' | 'success' | 'warning'
    title: string
  }>(),
  { tone: 'primary' },
)

const TONES = {
  primary: 'bg-primary-50 text-primary-700',
  success: 'bg-success-50 text-success-700',
  warning: 'bg-warning-50 text-warning-700',
} as const
</script>

<template>
  <!-- Auth result screens (design P1: check inbox, verified, link expired). -->
  <section class="flex flex-col items-center gap-4 text-center">
    <span class="result-art" :class="TONES[tone]"
      ><component :is="icon" class="icon" aria-hidden="true"
    /></span>
    <h1 class="t-h1">{{ title }}</h1>
    <slot />
    <div class="mt-2 flex w-full flex-col gap-2">
      <slot name="actions" />
    </div>
  </section>
</template>
