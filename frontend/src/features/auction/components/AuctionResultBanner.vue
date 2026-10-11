<script setup lang="ts">
import { computed } from 'vue'
import { formatCurrency } from '../format'
import type { AuctionResponse } from '../types'

const props = defineProps<{ auction: AuctionResponse }>()

// Every viewer sees the same result, the seller and the winner included (spec 011).
const message = computed(() =>
  props.auction.outcome === 'SOLD'
    ? `Sold to ${props.auction.winnerName} for ${formatCurrency(props.auction.currentBid)}`
    : 'No bids',
)
</script>

<template>
  <section
    class="stack rounded-md border border-border bg-surface-subtle p-4"
    aria-labelledby="result-title"
  >
    <h2 id="result-title" class="text-title font-bold">Result</h2>
    <p>{{ message }}</p>
  </section>
</template>
