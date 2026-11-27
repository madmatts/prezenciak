import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { http } from '../http'
import { ApiError } from '../problem'

function jsonResponse(status: number, body: unknown, contentType = 'application/json'): Response {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': contentType } })
}

describe('http client', () => {
  const fetchMock = vi.fn<typeof fetch>()

  beforeEach(() => {
    vi.stubGlobal('fetch', fetchMock)
    document.cookie = 'XSRF-TOKEN=token%20value'
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    fetchMock.mockReset()
    document.cookie = 'XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT'
  })

  it('sends the session cookie and Accept-Language, without CSRF header on GET', async () => {
    fetchMock.mockResolvedValue(jsonResponse(200, { ok: true }))

    await expect(http.get('/api/public/ping', { locale: 'en' })).resolves.toEqual({ ok: true })

    const [, init] = fetchMock.mock.calls[0]!
    const headers = init?.headers as Record<string, string>
    expect(init?.credentials).toBe('include')
    expect(headers['Accept-Language']).toBe('en')
    expect(headers['X-XSRF-TOKEN']).toBeUndefined()
  })

  it('echoes the CSRF cookie in a header on state-changing requests', async () => {
    fetchMock.mockResolvedValue(new Response(null, { status: 204 }))

    await http.post('/api/something', { name: 'Anna' })

    const [, init] = fetchMock.mock.calls[0]!
    const headers = init?.headers as Record<string, string>
    expect(headers['X-XSRF-TOKEN']).toBe('token value')
    expect(headers['Content-Type']).toBe('application/json')
    expect(init?.body).toBe('{"name":"Anna"}')
  })

  it('turns Problem Details into an ApiError with code, params and field errors', async () => {
    fetchMock.mockResolvedValue(
      jsonResponse(
        400,
        {
          status: 400,
          code: 'VALIDATION_FAILED',
          params: { max: 5 },
          errors: [{ field: 'email', code: 'Email' }],
        },
        'application/problem+json',
      ),
    )

    const error = await http.post('/api/x', {}).catch((e: unknown) => e)

    expect(error).toBeInstanceOf(ApiError)
    expect(error).toMatchObject({
      status: 400,
      code: 'VALIDATION_FAILED',
      params: { max: 5 },
      fieldErrors: [{ field: 'email', code: 'Email' }],
    })
  })

  it('maps a non-JSON error response to a generic INTERNAL error', async () => {
    fetchMock.mockResolvedValue(new Response('<html>Bad gateway</html>', { status: 502 }))

    await expect(http.get('/api/x')).rejects.toMatchObject({ status: 502, code: 'INTERNAL' })
  })

  it('maps a network failure to the NETWORK error code', async () => {
    fetchMock.mockRejectedValue(new TypeError('Failed to fetch'))

    await expect(http.get('/api/x')).rejects.toMatchObject({ status: 0, code: 'NETWORK' })
  })
})
