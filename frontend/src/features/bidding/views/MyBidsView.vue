<script setup lang="ts">
import { watch } from 'vue'
import AppHeader from '../../../components/AppHeader.vue'
import { useIdentityStore } from '../../identity/stores/identity'
import MyBidItem from '../components/MyBidItem.vue'
import { useMyBids } from '../composables/useMyBids'

const identity = useIdentityStore()
const { state, items, loadedAt, load } = useMyBids()

// The list belongs to the selected user, so switching user loads it again.
watch(() => identity.currentUserId, load, { immediate: true })
</script>

<template>
  <div class="min-h-dvh">
    <AppHeader />

    <main class="container-app stack-section py-6">
      <h1 class="text-display font-bold">My bids</h1>

      <p v-if="state === 'loading'" role="status" class="text-ink-muted">Loading your bids…</p>

      <div v-else-if="state === 'error'" role="alert" class="flex flex-wrap items-center gap-3">
        <p>Could not load your bids.</p>
        <button type="button" class="btn btn-outline" @click="load()">Try again</button>
      </div>

      <p v-else-if="items.length === 0" class="text-ink-muted">You have not placed any bids yet.</p>

      <ul v-else class="auction-grid">
        <li v-for="item in items" :key="item.auctionId">
          <MyBidItem :item="item" :now="loadedAt" />
        </li>
      </ul>
    </main>
  </div>
</template>
