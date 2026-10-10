import { defineStore } from 'pinia'
import { ref } from 'vue'
import { fetchUsers } from '../api'
import type { UserSummary } from '../types'

const STORAGE_KEY = 'bidanddrive.currentUserId'
const DEFAULT_USER_ID = 1

function readStoredUserId(): number {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    const id = Number(raw)
    return raw !== null && Number.isInteger(id) && id > 0 ? id : DEFAULT_USER_ID
  } catch {
    return DEFAULT_USER_ID
  }
}

function writeStoredUserId(id: number): void {
  try {
    localStorage.setItem(STORAGE_KEY, String(id))
  } catch {
    // Storage unavailable: the choice still holds for this page session.
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
