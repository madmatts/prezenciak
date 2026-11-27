/** Response types of the Wishka API (mirror the backend DTO records). */

export interface PingResponse {
  message: string
  locale: string
}

export type AuthProvider = 'GOOGLE'

export interface LinkedLogin {
  provider: AuthProvider
  email: string | null
}

/** GET /api/me: the logged-in user's own profile (owner-facing, never reservation data). */
export interface Me {
  id: number
  email: string
  displayName: string
  locale: 'pl' | 'en'
  emailVerified: boolean
  hasPassword: boolean
  linkedLogins: LinkedLogin[]
}

export interface AuthConfig {
  googleEnabled: boolean
}

export interface PasswordResetCheck {
  email: string
}

export interface RegisterRequest {
  email: string
  password: string
  displayName: string
  acceptPrivacyPolicy: boolean
  locale: 'pl' | 'en'
}

export interface UpdateMeRequest {
  displayName?: string
  locale?: 'pl' | 'en'
}
