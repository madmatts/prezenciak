<script setup lang="ts">
import { CircleAlert } from '@lucide/vue'
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    id: string
    label: string
    type?: 'text' | 'email'
    autocomplete?: string
    hint?: string
    /** Already translated error message. */
    error?: string
    required?: boolean
    maxlength?: number
  }>(),
  {
    type: 'text',
    autocomplete: undefined,
    hint: undefined,
    error: undefined,
    maxlength: undefined,
  },
)

const model = defineModel<string>({ required: true })

const describedBy = computed(
  () =>
    [props.error ? `${props.id}-error` : null, props.hint ? `${props.id}-hint` : null]
      .filter(Boolean)
      .join(' ') || undefined,
)
</script>

<template>
  <div class="field">
    <label class="label" :for="id">{{ label }}</label>
    <input
      :id="id"
      v-model="model"
      class="input"
      :class="{ 'is-error': error }"
      :type="type"
      :autocomplete="autocomplete"
      :required="required"
      :maxlength="maxlength"
      :aria-invalid="error ? 'true' : undefined"
      :aria-describedby="describedBy"
    />
    <span v-if="error" :id="`${id}-error`" class="error-text">
      <CircleAlert class="icon icon-sm" aria-hidden="true" />{{ error }}
    </span>
    <span v-if="hint" :id="`${id}-hint`" class="hint">{{ hint }}</span>
  </div>
</template>
