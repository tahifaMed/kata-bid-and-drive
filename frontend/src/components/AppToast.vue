<script setup lang="ts">
import { onBeforeUnmount, watch } from 'vue'

const props = defineProps<{ message: string }>()
const emit = defineEmits<{ dismiss: [] }>()

/** Long enough to read a short sentence, short enough not to linger. */
const VISIBLE_MS = 6000

let timer: ReturnType<typeof setTimeout> | undefined

// A new message gets its own full time on screen.
watch(
  () => props.message,
  () => {
    clearTimeout(timer)
    timer = setTimeout(() => emit('dismiss'), VISIBLE_MS)
  },
  { immediate: true },
)

onBeforeUnmount(() => clearTimeout(timer))
</script>

<template>
  <div
    role="status"
    class="fixed inset-x-4 bottom-4 z-10 mx-auto flex max-w-md items-center justify-between gap-3 rounded-md border border-info bg-info-soft p-4 text-ink shadow-md"
  >
    <p>{{ message }}</p>
    <button type="button" class="btn btn-outline shrink-0" @click="emit('dismiss')">Dismiss</button>
  </div>
</template>
