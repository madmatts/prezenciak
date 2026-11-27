import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

import { isSupportedLocale, rememberLocale, SUPPORTED_LOCALES, type AppLocale } from '@/i18n/locale'

/** Current UI language and a switcher that also updates `<html lang>` and remembers the choice. */
export function useLocale() {
  const { locale } = useI18n({ useScope: 'global' })

  const current = computed<AppLocale>(() =>
    isSupportedLocale(locale.value) ? locale.value : SUPPORTED_LOCALES[0],
  )

  function setLocale(next: AppLocale): void {
    locale.value = next
    document.documentElement.lang = next
    rememberLocale(next)
  }

  return { locale: current, locales: SUPPORTED_LOCALES, setLocale }
}
