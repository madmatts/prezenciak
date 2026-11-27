import { describe, expect, it } from 'vitest'

import { passwordStrength } from '../usePasswordStrength'

describe('passwordStrength', () => {
  it('is 0 for an empty password', () => {
    expect(passwordStrength('')).toBe(0)
  })

  it('is weak below the 10-character minimum', () => {
    expect(passwordStrength('kot123')).toBe(1)
  })

  it('is weak for obvious passwords even when long enough', () => {
    expect(passwordStrength('password1234')).toBe(1)
    expect(passwordStrength('aaaaaaaaaaaa')).toBe(1)
  })

  it('rates passphrases from the design examples', () => {
    expect(passwordStrength('kotwbutach')).toBe(2)
    expect(passwordStrength('kot-w-butach-27')).toBe(3)
    expect(passwordStrength('kot-w-butach-i-pies-27')).toBe(4)
  })
})
