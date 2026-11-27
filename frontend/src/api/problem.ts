/** RFC 9457 Problem Details as returned by the Wishka backend (see backend GlobalExceptionHandler). */
export interface ProblemDetail {
  status: number
  title?: string
  detail?: string
  /** Machine-readable error code, translated by the frontend as `errors.<code>`. */
  code: string
  params?: Record<string, unknown>
  errors?: FieldViolation[]
}

export interface FieldViolation {
  field: string
  /** Constraint name, translated as `validation.<code>`. */
  code: string
}

/** Code used when the backend could not be reached at all. */
export const NETWORK_ERROR = 'NETWORK'
export const UNKNOWN_ERROR = 'INTERNAL'

export class ApiError extends Error {
  readonly status: number
  readonly code: string
  readonly params: Record<string, unknown>
  readonly fieldErrors: FieldViolation[]

  constructor(problem: ProblemDetail) {
    super(problem.code)
    this.name = 'ApiError'
    this.status = problem.status
    this.code = problem.code
    this.params = problem.params ?? {}
    this.fieldErrors = problem.errors ?? []
  }
}

export function isProblemDetail(value: unknown): value is ProblemDetail {
  return (
    typeof value === 'object' &&
    value !== null &&
    typeof (value as { code?: unknown }).code === 'string' &&
    typeof (value as { status?: unknown }).status === 'number'
  )
}
