import { computed, toValue, type MaybeRefOrGetter } from 'vue'

export const PASSWORD_MIN_LENGTH = 10

export type StrengthLevel = 0 | 1 | 2 | 3 | 4

const COMMON_PARTS = ['password', 'haslo', 'hasło', 'qwerty', '12345', 'wishka', 'admin', 'letmein']

/**
 * Password strength for the 4-segment meter (design P0). A deliberately simple, explicit heuristic: length first,
 * then variety, with a penalty for obvious patterns. The server only enforces the minimum length.
 *
 * 0 = empty, 1 = weak, 2 = medium, 3 = good, 4 = very good.
 */
export function passwordStrength(password: string): StrengthLevel {
  if (password.length === 0) {
    return 0
  }
  if (password.length < PASSWORD_MIN_LENGTH) {
    return 1
  }
  const lower = password.toLowerCase()
  if (COMMON_PARTS.some((part) => lower.includes(part)) || /^(.)\1+$/.test(password)) {
    return 1
  }
  const classes = [/[a-ząćęłńóśźż]/, /[A-ZĄĆĘŁŃÓŚŹŻ]/, /\d/, /[^\p{L}\d]/u].filter((pattern) =>
    pattern.test(password),
  ).length
  let score = password.length >= 16 ? 2 : password.length >= 12 ? 1 : 0
  score += classes >= 3 ? 2 : classes === 2 ? 1 : 0
  return score >= 4 ? 4 : score >= 2 ? 3 : 2
}

const LABELS = ['', 'weak', 'medium', 'good', 'veryGood'] as const

/** Level plus i18n keys for the meter's word and hint. */
export function usePasswordStrength(password: MaybeRefOrGetter<string>) {
  const level = computed(() => passwordStrength(toValue(password)))
  const labelKey = computed(() =>
    level.value === 0 ? '' : `auth.password.strength.${LABELS[level.value]}`,
  )
  const hintKey = computed(() =>
    level.value === 0 ? '' : `auth.password.strengthHint.${LABELS[level.value]}`,
  )
  return { level, labelKey, hintKey }
}
