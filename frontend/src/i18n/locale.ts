export const SUPPORTED_LOCALES = ['pl', 'en'] as const
export type AppLocale = (typeof SUPPORTED_LOCALES)[number]
export const DEFAULT_LOCALE: AppLocale = 'pl'

const STORAGE_KEY = 'wishka.locale'

export function isSupportedLocale(value: unknown): value is AppLocale {
  return typeof value === 'string' && (SUPPORTED_LOCALES as readonly string[]).includes(value)
}

/** First supported language from the browser's preference list, e.g. `en-GB` -> `en`. */
export function localeFromBrowser(languages: readonly string[]): AppLocale | undefined {
  for (const language of languages) {
    const base = language.toLowerCase().split('-')[0]
    if (isSupportedLocale(base)) {
      return base
    }
  }
  return undefined
}

/**
 * Initial locale: the user's explicit choice on this device, otherwise the browser, otherwise Polish.
 * From Phase 1 the logged-in user's profile locale overrides this.
 */
export function detectInitialLocale(
  storage: Pick<Storage, 'getItem'> = window.localStorage,
  languages: readonly string[] = window.navigator.languages,
): AppLocale {
  const stored = storage.getItem(STORAGE_KEY)
  if (isSupportedLocale(stored)) {
    return stored
  }
  return localeFromBrowser(languages) ?? DEFAULT_LOCALE
}

export function rememberLocale(
  locale: AppLocale,
  storage: Pick<Storage, 'setItem'> = window.localStorage,
): void {
  storage.setItem(STORAGE_KEY, locale)
}
