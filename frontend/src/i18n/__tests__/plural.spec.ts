import { describe, expect, it } from 'vitest'

import { createAppI18n, polishPlural } from '../index'

describe('Polish plural rule', () => {
  it.each([
    [1, 0],
    [2, 1],
    [4, 1],
    [5, 2],
    [12, 2],
    [14, 2],
    [22, 1],
    [25, 2],
  ])('%i -> form %i', (count, form) => {
    expect(polishPlural(count)).toBe(form)
  })

  it('is used by vue-i18n for Polish messages', () => {
    const { t } = createAppI18n('pl').global
    expect(t('auth.register.fixFields', { count: 1 }, 1)).toBe('Popraw 1 pole')
    expect(t('auth.register.fixFields', { count: 3 }, 3)).toBe('Popraw 3 pola')
    expect(t('auth.register.fixFields', { count: 5 }, 5)).toBe('Popraw 5 pól')
  })

  it('leaves English with the default two forms', () => {
    const { t } = createAppI18n('en').global
    expect(t('auth.register.fixFields', { count: 1 }, 1)).toBe('Fix 1 field')
    expect(t('auth.register.fixFields', { count: 3 }, 3)).toBe('Fix 3 fields')
  })
})
