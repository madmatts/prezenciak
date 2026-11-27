import type { PingResponse } from '@/types/api'
import { http, type HttpOptions } from './http'

export function fetchPing(options?: HttpOptions): Promise<PingResponse> {
  return http.get<PingResponse>('/api/public/ping', options)
}
