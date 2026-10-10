<script setup lang="ts">
import { formatCurrency } from '../format'
import type { AuctionBid } from '../types'

defineProps<{
  /** Newest first, as the API sends them. */
  bids: AuctionBid[]
}>()

const time = new Intl.DateTimeFormat('en-US', { dateStyle: 'medium', timeStyle: 'short' })
</script>

<template>
  <p v-if="bids.length === 0" class="text-ink-muted">No bids yet.</p>
  <ol v-else class="flex flex-col gap-2">
    <!-- Bids have no id in the public history; amount and time together are unique per auction. -->
    <li
      v-for="bid in bids"
      :key="`${bid.placedAt}-${bid.amount}`"
      class="flex flex-wrap justify-between gap-x-4 border-b border-border pb-2"
    >
      <span class="font-semibold">{{ formatCurrency(bid.amount) }}</span>
      <span>{{ bid.bidderName }}</span>
      <time :datetime="bid.placedAt" class="text-meta text-ink-muted">
        {{ time.format(new Date(bid.placedAt)) }}
      </time>
    </li>
  </ol>
</template>
