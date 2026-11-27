import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { authApi } from '@/api/auth'
import { ApiError } from '@/api/problem'
import type { Me } from '@/types/api'
import { useAuthStore } from '../auth'

vi.mock('@/api/auth', () => ({
  authApi: {
    me: vi.fn<(...args: unknown[]) => Promise<unknown>>(),
    login: vi.fn<(...args: unknown[]) => Promise<unknown>>(),
    logout: vi.fn<(...args: unknown[]) => Promise<unknown>>(),
    register: vi.fn<(...args: unknown[]) => Promise<unknown>>(),
    updateMe: vi.fn<(...args: unknown[]) => Promise<unknown>>(),
  },
}))

const ME: Me = {
  id: 1,
  email: 'ania@example.com',
  displayName: 'Ania',
  locale: 'pl',
  emailVerified: false,
  hasPassword: true,
  linkedLogins: [],
}

describe('auth store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    sessionStorage.clear()
    vi.mocked(authApi.me).mockReset()
  })

  it('treats 401 from /api/me as anonymous', async () => {
    vi.mocked(authApi.me).mockRejectedValue(new ApiError({ status: 401, code: 'UNAUTHORIZED' }))
    const auth = useAuthStore()

    await auth.ensureLoaded()

    expect(auth.status).toBe('anonymous')
    expect(auth.isAuthenticated).toBe(false)
  })

  it('loads the session only once for concurrent navigations', async () => {
    vi.mocked(authApi.me).mockResolvedValue(ME)
    const auth = useAuthStore()

    await Promise.all([auth.ensureLoaded(), auth.ensureLoaded()])

    expect(authApi.me).toHaveBeenCalledTimes(1)
    expect(auth.needsVerification).toBe(true)
  })

  it('shows the verification banner again after a new login', async () => {
    vi.mocked(authApi.me).mockResolvedValue(ME)
    const auth = useAuthStore()
    auth.dismissVerifyBanner()

    await auth.login('ania@example.com', 'kot-w-butach-27')

    expect(auth.verifyBannerDismissed).toBe(false)
    expect(auth.user).toEqual(ME)
  })

  it('clears the user on logout even if the request fails', async () => {
    vi.mocked(authApi.me).mockResolvedValue(ME)
    vi.mocked(authApi.logout).mockRejectedValue(new ApiError({ status: 0, code: 'NETWORK' }))
    const auth = useAuthStore()
    await auth.refresh()

    await expect(auth.logout()).rejects.toBeInstanceOf(ApiError)

    expect(auth.user).toBeNull()
  })
})
