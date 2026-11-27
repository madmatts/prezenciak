import { useApiErrorToast } from '@/composables/useApiErrorToast'
import { useLocale } from '@/composables/useLocale'
import type { AppLocale } from '@/i18n/locale'
import { useAuthStore } from '@/stores/auth'

/**
 * Switches the UI language. For a logged-in user it's also saved in the profile, so emails and notifications use it
 * too (CLAUDE.md §7: locale from the user profile).
 */
export function useLanguage() {
  const { locale, locales, setLocale } = useLocale()
  const auth = useAuthStore()
  const { showError } = useApiErrorToast()

  async function changeLanguage(next: AppLocale): Promise<void> {
    const previous = locale.value
    setLocale(next)
    if (!auth.isAuthenticated || auth.user?.locale === next) {
      return
    }
    try {
      await auth.updateProfile({ locale: next })
    } catch (error) {
      setLocale(previous)
      showError(error)
    }
  }

  return { locale, locales, changeLanguage }
}
