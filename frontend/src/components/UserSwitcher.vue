<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useIdentityStore } from '../features/identity/stores/identity'

const identity = useIdentityStore()
const loadFailed = ref(false)

const selectedId = computed({
  get: () => identity.currentUserId,
  set: (id: number) => identity.setUser(id),
})

onMounted(() => {
  identity.loadUsers().catch(() => {
    loadFailed.value = true
  })
})
</script>

<template>
  <label class="flex items-center gap-2">
    <span class="text-meta text-ink-muted">Acting as</span>
    <select v-model.number="selectedId" class="field" :disabled="loadFailed">
      <option v-for="user in identity.users" :key="user.id" :value="user.id">
        {{ user.displayName }}
      </option>
    </select>
  </label>
</template>
