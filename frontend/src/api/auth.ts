import type {
  AuthConfig,
  Me,
  PasswordResetCheck,
  RegisterRequest,
  UpdateMeRequest,
} from '@/types/api'
import { http } from './http'

/** Account endpoints (backend AuthController, MeController, AccountSecurityController). */
export const authApi = {
  config: () => http.get<AuthConfig>('/api/public/auth/config'),
  me: () => http.get<Me>('/api/me'),
  updateMe: (body: UpdateMeRequest) => http.patch<Me>('/api/me', body),

  register: (body: RegisterRequest) => http.post<void>('/api/auth/register', body),
  login: (email: string, password: string) =>
    http.post<void>('/api/auth/login', { email, password }),
  logout: () => http.post<void>('/api/auth/logout'),

  verifyEmail: (token: string) => http.post<void>('/api/auth/verify-email', { token }),
  resendVerification: () => http.post<void>('/api/auth/verify-email/resend'),

  requestPasswordReset: (email: string) =>
    http.post<void>('/api/auth/password-reset/request', { email }),
  checkPasswordReset: (token: string) =>
    http.post<PasswordResetCheck>('/api/auth/password-reset/check', { token }),
  confirmPasswordReset: (token: string, password: string) =>
    http.post<void>('/api/auth/password-reset/confirm', { token, password }),

  changePassword: (newPassword: string, currentPassword?: string) =>
    http.put<void>('/api/me/password', { currentPassword, newPassword }),
  removePassword: () => http.delete<void>('/api/me/password'),
  unlinkGoogle: () => http.delete<void>('/api/me/providers/google'),
}

/** Full-page navigation into Spring Security's Google login (works through the dev proxy and nginx). */
export const GOOGLE_LOGIN_URL = '/oauth2/authorization/google'
