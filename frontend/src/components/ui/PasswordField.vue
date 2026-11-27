<script setup lang="ts">
import { CircleAlert, Eye, EyeOff } from '@lucide/vue'
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'

import { usePasswordStrength } from '@/composables/usePasswordStrength'

const props = withDefaults(
  defineProps<{
    id: string
    label: string
    autocomplete: 'current-password' | 'new-password'
    hint?: string
    /** Already translated error message. */
    error?: string
    showStrength?: boolean
  }>(),
  { hint: undefined, error: undefined, showStrength: false },
)

const model = defineModel<string>({ required: true })

const { t } = useI18n()
const visible = ref(false)
const { level, labelKey, hintKey } = usePasswordStrength(model)

const toggleLabel = computed(() => t(visible.value ? 'auth.password.hide' : 'auth.password.show'))
const describedBy = computed(
  () =>
    [props.error ? `${props.id}-error` : null, props.hint ? `${props.id}-hint` : null]
      .filter(Boolean)
      .join(' ') || undefined,
)
const meterVisible = computed(() => props.showStrength && level.value > 0)
</script>

<template>
  <div class="field">
    <slot name="label">
      <label class="label" :for="id">{{ label }}</label>
    </slot>
    <div class="control">
      <input
        :id="id"
        v-model="model"
        class="input pr-14"
        :class="{ 'is-error': error }"
        :type="visible ? 'text' : 'password'"
        :autocomplete="autocomplete"
        :aria-invalid="error ? 'true' : undefined"
        :aria-describedby="describedBy"
      />
      <button
        type="button"
        class="btn btn-ghost btn-icon absolute right-0.5"
        :aria-label="toggleLabel"
        :aria-pressed="visible"
        @click="visible = !visible"
      >
        <EyeOff v-if="visible" class="icon" aria-hidden="true" />
        <Eye v-else class="icon" aria-hidden="true" />
      </button>
    </div>
    <span v-if="error" :id="`${id}-error`" class="error-text">
      <CircleAlert class="icon icon-sm" aria-hidden="true" />{{ error }}
    </span>
    <div v-if="meterVisible" class="flex flex-col gap-1" aria-live="polite">
      <div class="pw-meter" :data-level="level" aria-hidden="true">
        <span></span><span></span><span></span><span></span>
      </div>
      <div class="pw-text">
        <i18n-t keypath="auth.password.strengthLabel" tag="span" scope="global">
          <template #level>
            <strong>{{ t(labelKey) }}</strong>
          </template>
        </i18n-t>
        <span>{{ t(hintKey) }}</span>
      </div>
    </div>
    <span v-if="hint" :id="`${id}-hint`" class="hint">{{ hint }}</span>
  </div>
</template>
