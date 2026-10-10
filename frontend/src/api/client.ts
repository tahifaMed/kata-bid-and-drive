import { useIdentityStore } from '../features/identity/stores/identity'

export class ApiError extends Error {
  constructor(
    readonly status: number,
    message: string,
  ) {
    super(message)
  }
}

// The only place that calls fetch. Adds the acting user to every request.
export async function apiFetch<T>(path: string, init: RequestInit = {}): Promise<T> {
  const identity = useIdentityStore()
  const headers = new Headers(init.headers)
  headers.set('X-User-Id', String(identity.currentUserId))

  const response = await fetch(path, { ...init, headers })
  if (!response.ok) {
    throw new ApiError(response.status, `Request failed with status ${response.status}`)
  }
  return (await response.json()) as T
}
