import { describe, expect, it, vi } from 'vitest'

vi.mock('@/components/layout/AppShell.vue', () => ({ default: {} }))

import { safeRedirect } from '../index'

describe('safeRedirect', () => {
  it('accepts paths inside the app', () => {
    expect(safeRedirect('/profile')).toBe('/profile')
  })

  it('rejects absolute and protocol-relative URLs (open redirect)', () => {
    expect(safeRedirect('https://evil.example')).toBeUndefined()
    expect(safeRedirect('//evil.example')).toBeUndefined()
    expect(safeRedirect(undefined)).toBeUndefined()
  })
})
