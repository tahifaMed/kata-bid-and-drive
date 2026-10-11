import { defineStore } from 'pinia'
import { ref } from 'vue'
import { fetchUsers } from '../api'
import type { UserSummary } from '../types'

// Per tab (ADR 0001): two tabs can act as two users, and a reload keeps the tab's choice.
const STORAGE_KEY = 'bidanddrive.currentUserId'
const DEFAULT_USER_ID = 1

function readStoredUserId(): number {
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY)
    const id = Number(raw)
    return raw !== null && Number.isInteger(id) && id > 0 ? id : DEFAULT_USER_ID
  } catch {
    return DEFAULT_USER_ID
  }
}

function writeStoredUserId(id: number): void {
  try {
    sessionStorage.setItem(STORAGE_KEY, String(id))
  } catch {
    // Storage unavailable: the choice still holds until the page reloads.
  }
}

export const useIdentityStore = defineStore('identity', () => {
  const currentUserId = ref(readStoredUserId())
  const users = ref<UserSummary[]>([])

  function setUser(id: number): void {
    currentUserId.value = id
    writeStoredUserId(id)
  }

  async function loadUsers(): Promise<void> {
    users.value = await fetchUsers()
    if (!users.value.some((user) => user.id === currentUserId.value)) {
      setUser(DEFAULT_USER_ID)
    }
  }

  return { currentUserId, users, setUser, loadUsers }
})
