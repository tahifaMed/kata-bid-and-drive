<script setup lang="ts">
import { computed } from 'vue'
import { formatCurrency, formatTimeLeft } from '../format'
import type { AuctionResponse } from '../types'

const props = defineProps<{
  auction: AuctionResponse
  /** Epoch milliseconds the time left is measured from. */
  now: number
}>()

// The detail says "Ended" once the end has passed; the list keeps "Ending" (spec 005).
const timeLeft = computed(() =>
  Date.parse(props.auction.endsAt) <= props.now
    ? 'Ended'
    : formatTimeLeft(props.auction.endsAt, props.now),
)
</script>

<template>
  <dl class="grid grid-cols-[auto_1fr] gap-x-6 gap-y-2">
    <dt class="text-ink-muted">Current bid</dt>
    <dd class="text-title font-bold">{{ formatCurrency(auction.currentBid) }}</dd>
    <dt class="text-ink-muted">Minimum next bid</dt>
    <dd>{{ formatCurrency(auction.minNextBid) }}</dd>
    <dt class="text-ink-muted">Bids</dt>
    <dd>{{ auction.bidCount }}</dd>
    <dt class="text-ink-muted">Time left</dt>
    <dd>{{ timeLeft }}</dd>
  </dl>
</template>
