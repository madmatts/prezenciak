import { existsSync, readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import process from 'node:process'

import { describe, expect, it } from 'vitest'

import en from '../en.json'
import pl from '../pl.json'

type Dictionary = { [key: string]: string | Dictionary }

function flatten(dictionary: Dictionary, prefix = ''): Record<string, string> {
  return Object.entries(dictionary).reduce<Record<string, string>>((flat, [key, value]) => {
    const path = prefix ? `${prefix}.${key}` : key
    return typeof value === 'string'
      ? { ...flat, [path]: value }
      : { ...flat, ...flatten(value, path) }
  }, {})
}

const polish = flatten(pl)
const english = flatten(en)

// Golden rule 4: every text exists in Polish and English, in the same commit.
describe('locale dictionaries', () => {
  it('have the same keys in Polish and English', () => {
    expect(Object.keys(english).sort()).toEqual(Object.keys(polish).sort())
  })

  const allEntries = [...Object.entries(polish), ...Object.entries(english)]

  it('have no empty translations', () => {
    const empty = allEntries.filter(([, value]) => value.trim() === '').map(([key]) => key)
    expect(empty).toEqual([])
  })

  it('never hard-code the brand name (use @:app.name)', () => {
    const branded = allEntries
      .filter(([, value]) => value.toLowerCase().includes('wishka'))
      .map(([key]) => key)
    expect(branded).toEqual([])
  })

  it('translate every error code the backend can return', () => {
    // Vitest runs from frontend/, so the backend sources are one level up.
    const errorCodeFile = resolve(
      process.cwd(),
      '../backend/src/main/java/pl/wishka/common/error/ErrorCode.java',
    )
    if (!existsSync(errorCodeFile)) {
      return
    }
    const source = readFileSync(errorCodeFile, 'utf8')
    const codes = [...source.matchAll(/^\s{4}([A-Z_]+)\(HttpStatus\./gm)].map((match) => match[1])

    expect(codes.length).toBeGreaterThan(0)
    const missing = codes.filter((code) => !polish[`errors.${code}`] || !english[`errors.${code}`])
    expect(missing).toEqual([])
  })
})
