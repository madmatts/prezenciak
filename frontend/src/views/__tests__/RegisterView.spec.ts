import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { authApi } from '@/api/auth'
import { ApiError } from '@/api/problem'
import RegisterView from '../auth/RegisterView.vue'
import { plugins } from './testing'

vi.mock('@/api/auth', () => ({
  GOOGLE_LOGIN_URL: '/oauth2/authorization/google',
  authApi: {
    config: vi
      .fn<(...args: unknown[]) => Promise<unknown>>()
      .mockResolvedValue({ googleEnabled: false }),
    register: vi.fn<(...args: unknown[]) => Promise<unknown>>(),
    me: vi.fn<(...args: unknown[]) => Promise<unknown>>(),
  },
}))

describe('RegisterView', () => {
  beforeEach(() => {
    vi.mocked(authApi.register).mockReset()
  })

  it('summarises invalid fields with the Polish plural form', async () => {
    const wrapper = mount(RegisterView, { global: { plugins: plugins() } })
    await wrapper.get('#register-email').setValue('ania@example.com')
    await wrapper.get('#register-password').setValue('kot123')

    await wrapper.get('form').trigger('submit')

    expect(wrapper.get('[data-testid="register-error"]').text()).toContain('Popraw 3 pola')
    expect(wrapper.text()).toContain('Hasło jest za krótkie')
    expect(wrapper.text()).toContain('Zaznacz zgodę, żeby założyć konto.')
    expect(authApi.register).not.toHaveBeenCalled()
  })

  it('marks the email field when the address is already registered', async () => {
    vi.mocked(authApi.register).mockRejectedValue(
      new ApiError({ status: 409, code: 'EMAIL_TAKEN' }),
    )
    const wrapper = mount(RegisterView, { global: { plugins: plugins() } })
    await wrapper.get('#register-name').setValue('Ania')
    await wrapper.get('#register-email').setValue('ania@example.com')
    await wrapper.get('#register-password').setValue('kot-w-butach-27')
    await wrapper.get('[data-testid="register-consent"]').setValue(true)

    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(authApi.register).toHaveBeenCalledWith(
      expect.objectContaining({
        email: 'ania@example.com',
        locale: 'pl',
        acceptPrivacyPolicy: true,
      }),
    )
    expect(wrapper.text()).toContain('Konto z tym adresem już istnieje')
  })
})
