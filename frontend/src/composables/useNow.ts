import { onScopeDispose, ref } from 'vue'

/** The current time in epoch milliseconds, updated once per second, so a time left counts down (spec 010). */
export function useNow() {
  const now = ref(Date.now())
  const timer = setInterval(() => {
    now.value = Date.now()
  }, 1000)
  onScopeDispose(() => clearInterval(timer))
  return now
}
