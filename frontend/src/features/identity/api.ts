import { apiFetch } from '../../api/client'
import type { UserSummary } from './types'

export function fetchUsers(): Promise<UserSummary[]> {
  return apiFetch<UserSummary[]>('/api/users')
}
