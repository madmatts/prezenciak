import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { authApi } from '@/api/auth'
import { ApiError } from '@/api/problem'
import LoginView from '../auth/LoginView.vue'
import { plugins } from './testing'

vi.mock('@/api/auth', () => ({
  GOOGLE_LOGIN_URL: '/oauth2/authorization/google',
  authApi: {
    config: vi
      .fn<(...args: unknown[]) => Promise<unknown>>()
      .mockResolvedValue({ googleEnabled: true }),
    login: vi.fn<(...args: unknown[]) => Promise<unknown>>(),
    me: vi.fn<(...args: unknown[]) => Promise<unknown>>(),
  },
}))

describe('LoginView', () => {
  beforeEach(() => {
    vi.mocked(authApi.login).mockReset()
  })

  it('validates empty fields before calling the API', async () => {
    const wrapper = mount(LoginView, { global: { plugins: plugins() } })

    await wrapper.get('form').trigger('submit')

    expect(wrapper.text()).toContain('Wpisz adres e-mail.')
    expect(wrapper.text()).toContain('Wpisz hasło.')
    expect(authApi.login).not.toHaveBeenCalled()
  })

  it('shows the design error state for wrong credentials and clears the password', async () => {
    vi.mocked(authApi.login).mockRejectedValue(
      new ApiError({ status: 401, code: 'INVALID_CREDENTIALS' }),
    )
    const wrapper = mount(LoginView, { global: { plugins: plugins() } })
    await wrapper.get('#login-email').setValue('ania@example.com')
    await wrapper.get('#login-password').setValue('wrong-password')

    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(wrapper.get('[data-testid="login-error"]').text()).toContain('Nie udało się zalogować')
    expect(wrapper.text()).toContain('Wpisz hasło jeszcze raz.')
    expect((wrapper.get('#login-password').element as HTMLInputElement).value).toBe('')
  })

  it('offers Google login when it is configured', async () => {
    const wrapper = mount(LoginView, { global: { plugins: plugins('en') } })
    await flushPromises()

    expect(wrapper.get('a.gbtn').attributes('href')).toBe('/oauth2/authorization/google')
    expect(wrapper.text()).toContain('Continue with Google')
  })
})
