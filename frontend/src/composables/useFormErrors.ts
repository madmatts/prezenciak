import { computed, ref } from 'vue'

import { ApiError } from '@/api/problem'

export type FieldErrors<F extends string> = Partial<Record<F, string>>

/**
 * Per-field error messages (i18n keys) for a form: client-side checks plus the backend's `VALIDATION_FAILED`
 * field errors. Anything else becomes a form-level error code shown in the form's alert.
 */
export function useFormErrors<F extends string>(fields: readonly F[]) {
  const fieldErrors = ref<FieldErrors<F>>({}) as { value: FieldErrors<F> }
  const formError = ref<string | null>(null)

  const errorCount = computed(() => Object.values(fieldErrors.value).filter(Boolean).length)

  function setField(field: F, messageKey: string | null): void {
    fieldErrors.value = { ...fieldErrors.value, [field]: messageKey ?? undefined }
  }

  function reset(): void {
    fieldErrors.value = {}
    formError.value = null
  }

  /** Maps an API failure onto the form. Returns the error code. */
  function applyApiError(error: unknown, overrides: Partial<Record<string, string>> = {}): string {
    const code = error instanceof ApiError ? error.code : 'INTERNAL'
    if (error instanceof ApiError && error.fieldErrors.length > 0) {
      for (const violation of error.fieldErrors) {
        if ((fields as readonly string[]).includes(violation.field)) {
          setField(
            violation.field as F,
            overrides[`${violation.field}.${violation.code}`] ?? `validation.${violation.code}`,
          )
        }
      }
      return code
    }
    formError.value = code
    return code
  }

  return { fieldErrors, formError, errorCount, setField, reset, applyApiError }
}
