import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

import { authApi } from '@/api/auth'
import { ApiError } from '@/api/problem'
import type { Me, RegisterRequest, UpdateMeRequest } from '@/types/api'

type SessionStatus = 'unknown' | 'anonymous' | 'authenticated'

const BANNER_DISMISSED_KEY = 'wishka.verifyBannerDismissed'

/**
 * The session user, shared by every view (router guards, shell, profile). The session itself is an HttpOnly
 * cookie: this store only mirrors GET /api/me.
 */
export const useAuthStore = defineStore('auth', () => {
  const user = ref<Me | null>(null)
  const status = ref<SessionStatus>('unknown')
  const verifyBannerDismissed = ref(sessionStorage.getItem(BANNER_DISMISSED_KEY) === '1')
  let loading: Promise<void> | null = null

  const isAuthenticated = computed(() => status.value === 'authenticated')
  const needsVerification = computed(() => user.value !== null && !user.value.emailVerified)

  /** Loads the session once (router guards call it on every navigation). */
  function ensureLoaded(): Promise<void> {
    if (status.value !== 'unknown') {
      return Promise.resolve()
    }
    loading ??= refresh().finally(() => (loading = null))
    return loading
  }

  async function refresh(): Promise<void> {
    try {
      setUser(await authApi.me())
    } catch (error) {
      if (error instanceof ApiError && error.status === 401) {
        clear()
        return
      }
      throw error
    }
  }

  async function login(email: string, password: string): Promise<void> {
    await authApi.login(email, password)
    resetBanner()
    await refresh()
  }

  async function register(request: RegisterRequest): Promise<void> {
    await authApi.register(request)
    resetBanner()
    await refresh()
  }

  async function logout(): Promise<void> {
    try {
      await authApi.logout()
    } finally {
      clear()
    }
  }

  async function updateProfile(changes: UpdateMeRequest): Promise<Me> {
    const updated = await authApi.updateMe(changes)
    setUser(updated)
    return updated
  }

  /** "×" on the verification banner hides it until the next login (design P0). */
  function dismissVerifyBanner(): void {
    verifyBannerDismissed.value = true
    sessionStorage.setItem(BANNER_DISMISSED_KEY, '1')
  }

  function setUser(me: Me): void {
    user.value = me
    status.value = 'authenticated'
  }

  function clear(): void {
    user.value = null
    status.value = 'anonymous'
  }

  function resetBanner(): void {
    verifyBannerDismissed.value = false
    sessionStorage.removeItem(BANNER_DISMISSED_KEY)
  }

  return {
    user,
    status,
    isAuthenticated,
    needsVerification,
    verifyBannerDismissed,
    ensureLoaded,
    refresh,
    login,
    register,
    logout,
    updateProfile,
    dismissVerifyBanner,
    clear,
  }
})
