import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'

import { plugins } from '@/views/__tests__/testing'
import type { Me } from '@/types/api'
import SignInMethodsCard from '../SignInMethodsCard.vue'

vi.mock('@/api/auth', () => ({
  GOOGLE_LOGIN_URL: '/oauth2/authorization/google',
  authApi: {
    config: vi
      .fn<(...args: unknown[]) => Promise<unknown>>()
      .mockResolvedValue({ googleEnabled: true }),
  },
}))

const base: Me = {
  id: 1,
  email: 'ania@example.com',
  displayName: 'Ania',
  locale: 'pl',
  emailVerified: true,
  hasPassword: false,
  linkedLogins: [{ provider: 'GOOGLE', email: 'ania@gmail.com' }],
}

describe('SignInMethodsCard', () => {
  it('protects Google when it is the only sign-in method (design P1 last method)', () => {
    const wrapper = mount(SignInMethodsCard, {
      props: { user: base },
      global: { plugins: plugins() },
    })

    expect(wrapper.text()).toContain('Nie ustawione')
    expect(wrapper.text()).toContain('Połączone · ania@gmail.com')
    expect(wrapper.get('[data-testid="last-method"]').text()).toContain('Najpierw ustaw hasło')
    const unlink = wrapper.findAll('button').find((button) => button.text() === 'Odłącz')
    expect(unlink?.attributes('disabled')).toBeDefined()
  })

  it('allows removing either method when both exist', () => {
    const wrapper = mount(SignInMethodsCard, {
      props: { user: { ...base, hasPassword: true } },
      global: { plugins: plugins() },
    })

    expect(wrapper.find('[data-testid="last-method"]').exists()).toBe(false)
    const remove = wrapper.findAll('button').find((button) => button.text() === 'Usuń')
    expect(remove?.attributes('disabled')).toBeUndefined()
  })
})
