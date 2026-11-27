import { createI18n } from 'vue-i18n'

import { appConfig } from '@/config'
import en from '@/locales/en.json'
import pl from '@/locales/pl.json'
import { DEFAULT_LOCALE, rememberLocale, type AppLocale } from './locale'

/**
 * The brand name is injected from configuration as `app.name`, so translations reference it with the
 * linked message `@:app.name` instead of a literal (CLAUDE.md golden rule 4 + branding).
 */
const brand = { app: { name: appConfig.appName } }

export type MessageSchema = typeof pl & typeof brand

const numberFormats = {
  pl: {
    money: { style: 'currency', currency: 'PLN', currencyDisplay: 'symbol' },
    decimal: { style: 'decimal', minimumFractionDigits: 0, maximumFractionDigits: 2 },
  },
  en: {
    money: { style: 'currency', currency: 'PLN', currencyDisplay: 'code' },
    decimal: { style: 'decimal', minimumFractionDigits: 0, maximumFractionDigits: 2 },
  },
} as const

const datetimeFormats = {
  pl: {
    short: { year: 'numeric', month: '2-digit', day: '2-digit' },
    long: { year: 'numeric', month: 'long', day: 'numeric' },
  },
  en: {
    short: { year: 'numeric', month: 'short', day: 'numeric' },
    long: { year: 'numeric', month: 'long', day: 'numeric' },
  },
} as const

export function createAppI18n(locale: AppLocale = DEFAULT_LOCALE) {
  return createI18n<[MessageSchema], AppLocale, false>({
    legacy: false,
    locale,
    fallbackLocale: DEFAULT_LOCALE,
    messages: { pl: { ...pl, ...brand }, en: { ...en, ...brand } },
    numberFormats,
    datetimeFormats,
    missingWarn: import.meta.env.DEV,
    fallbackWarn: import.meta.env.DEV,
  })
}

export type AppI18n = ReturnType<typeof createAppI18n>

/** Switches the UI language, updates `<html lang>` and remembers the choice on this device. */
export function applyLocale(i18n: AppI18n, locale: AppLocale): void {
  i18n.global.locale.value = locale
  document.documentElement.lang = locale
  rememberLocale(locale)
}
