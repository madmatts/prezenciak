import { ApiError, isProblemDetail, NETWORK_ERROR, UNKNOWN_ERROR } from './problem'

type Method = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'

const SAFE_METHODS: readonly Method[] = ['GET']
const CSRF_COOKIE = 'XSRF-TOKEN'
const CSRF_HEADER = 'X-XSRF-TOKEN'

export interface HttpOptions {
  /** Sent as `Accept-Language`, so backend texts (errors, emails) match the UI language. */
  locale?: string
  signal?: AbortSignal
}

function readCookie(name: string): string | undefined {
  const prefix = `${name}=`
  return document.cookie
    .split('; ')
    .find((cookie) => cookie.startsWith(prefix))
    ?.slice(prefix.length)
}

/**
 * Minimal JSON client for the Wishka API. Uses the session cookie (never tokens in storage) and echoes the
 * CSRF cookie in a header on state-changing requests. Every failure becomes an {@link ApiError} with a code.
 */
export async function request<T>(
  method: Method,
  path: string,
  body?: unknown,
  options: HttpOptions = {},
): Promise<T> {
  const headers: Record<string, string> = { Accept: 'application/json' }
  if (options.locale) {
    headers['Accept-Language'] = options.locale
  }
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json'
  }
  if (!SAFE_METHODS.includes(method)) {
    const token = readCookie(CSRF_COOKIE)
    if (token) {
      headers[CSRF_HEADER] = decodeURIComponent(token)
    }
  }

  let response: Response
  try {
    response = await fetch(path, {
      method,
      headers,
      credentials: 'include',
      body: body === undefined ? undefined : JSON.stringify(body),
      signal: options.signal,
    })
  } catch (cause) {
    if (cause instanceof DOMException && cause.name === 'AbortError') {
      throw cause
    }
    throw new ApiError({ status: 0, code: NETWORK_ERROR })
  }

  if (!response.ok) {
    throw await toApiError(response)
  }
  if (response.status === 204) {
    return undefined as T
  }
  return (await response.json()) as T
}

async function toApiError(response: Response): Promise<ApiError> {
  try {
    const payload: unknown = await response.json()
    if (isProblemDetail(payload)) {
      return new ApiError(payload)
    }
  } catch {
    // Not JSON (e.g. a proxy error page): fall through to a generic error.
  }
  return new ApiError({ status: response.status, code: UNKNOWN_ERROR })
}

export const http = {
  get: <T>(path: string, options?: HttpOptions) => request<T>('GET', path, undefined, options),
  post: <T>(path: string, body?: unknown, options?: HttpOptions) =>
    request<T>('POST', path, body, options),
  put: <T>(path: string, body?: unknown, options?: HttpOptions) =>
    request<T>('PUT', path, body, options),
  patch: <T>(path: string, body?: unknown, options?: HttpOptions) =>
    request<T>('PATCH', path, body, options),
  delete: <T>(path: string, options?: HttpOptions) =>
    request<T>('DELETE', path, undefined, options),
}
