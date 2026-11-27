import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it } from 'vitest'

import { createAppI18n } from '@/i18n'
import LanguageSwitcher from '../LanguageSwitcher.vue'

describe('LanguageSwitcher', () => {
  beforeEach(() => {
    window.localStorage.clear()
  })

  it('shows the label in the current language', () => {
    const wrapper = mount(LanguageSwitcher, { global: { plugins: [createAppI18n('pl')] } })

    expect(wrapper.text()).toContain('Język')
  })

  it('switches the UI language, <html lang> and remembers the choice', async () => {
    const i18n = createAppI18n('pl')
    const wrapper = mount(LanguageSwitcher, { global: { plugins: [i18n] } })

    await wrapper.get('[data-testid="language-switcher"]').setValue('en')

    expect(i18n.global.locale.value).toBe('en')
    expect(document.documentElement.lang).toBe('en')
    expect(window.localStorage.getItem('wishka.locale')).toBe('en')
    expect(wrapper.text()).toContain('Language')
  })
})
