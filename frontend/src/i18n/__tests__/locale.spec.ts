import { describe, expect, it } from 'vitest'

import { detectInitialLocale, localeFromBrowser } from '../locale'

const emptyStorage = { getItem: () => null }

describe('locale detection', () => {
  it('prefers the choice remembered on this device', () => {
    expect(detectInitialLocale({ getItem: () => 'en' }, ['pl-PL'])).toBe('en')
  })

  it('ignores an unsupported remembered value', () => {
    expect(detectInitialLocale({ getItem: () => 'de' }, ['en-US'])).toBe('en')
  })

  it('uses the first supported browser language', () => {
    expect(localeFromBrowser(['de-DE', 'en-GB', 'pl'])).toBe('en')
  })

  it('falls back to Polish when nothing matches', () => {
    expect(detectInitialLocale(emptyStorage, ['de-DE', 'fr'])).toBe('pl')
    expect(detectInitialLocale(emptyStorage, [])).toBe('pl')
  })
})
