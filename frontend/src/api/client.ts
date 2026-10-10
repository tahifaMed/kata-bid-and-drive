import { useIdentityStore } from '../features/identity/stores/identity'

/** An RFC 9457 error body. Features read their own extra properties, such as `minimumBid`. */
export interface ProblemDetail {
  title?: string
  detail?: string
  [property: string]: unknown
}

export class ApiError extends Error {
  constructor(
    readonly status: number,
    message: string,
    /** Undefined when the error body is not a JSON object, for example a proxy's HTML page. */
    readonly problem?: ProblemDetail,
  ) {
    super(message)
  }
}

async function readProblem(response: Response): Promise<ProblemDetail | undefined> {
  try {
    const body: unknown = await response.json()
    return typeof body === 'object' && body !== null && !Array.isArray(body) ? (body as ProblemDetail) : undefined
  } catch {
    return undefined
  }
}

// The only place that calls fetch. Adds the acting user to every request.
export async function apiFetch<T>(path: string, init: RequestInit = {}): Promise<T> {
  const identity = useIdentityStore()
  const headers = new Headers(init.headers)
  headers.set('X-User-Id', String(identity.currentUserId))

  const response = await fetch(path, { ...init, headers })
  if (!response.ok) {
    throw new ApiError(response.status, `Request failed with status ${response.status}`, await readProblem(response))
  }
  return (await response.json()) as T
}
