<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import { formatCurrency, formatTimeLeft } from '../../auction/format'
import type { BidderStatus, MyBid } from '../types'

// Used when the seller gave no photo. Decorative: the title already says what the car is.
const NO_PHOTO = '/images/auctions/no-photo.svg'

const props = defineProps<{
  item: MyBid
  /** Epoch milliseconds the time left is measured from. */
  now: number
}>()

const title = computed(() => {
  const { year, make, model } = props.item.car
  return `${year} ${make} ${model}`
})

const BADGES: Record<BidderStatus, { label: string; style: string }> = {
  WINNING: { label: 'Winning', style: 'status-winning' },
  OUTBID: { label: 'Outbid', style: 'status-outbid' },
  WON: { label: 'Won', style: 'status-winning' },
  LOST: { label: 'Lost', style: 'status-outbid' },
}

const badge = computed(() => BADGES[props.item.status])

// Ended auctions stay listed, with WON or LOST once the server sees them closed (spec 011).
const timeLeft = computed(() =>
  Date.parse(props.item.endsAt) <= props.now ? 'Ended' : formatTimeLeft(props.item.endsAt, props.now),
)
</script>

<template>
  <RouterLink :to="`/auctions/${item.auctionId}`" class="auction-card no-underline">
    <div class="auction-card-media">
      <img
        v-if="item.car.imageUrl"
        :src="item.car.imageUrl"
        :alt="title"
        width="960"
        height="640"
        loading="lazy"
      />
      <img v-else :src="NO_PHOTO" alt="" width="960" height="640" />

      <p class="image-chip absolute bottom-3 left-3">
        <span>{{ timeLeft }}</span>
      </p>
    </div>

    <div class="flex flex-col gap-1">
      <div class="flex flex-wrap items-center justify-between gap-2">
        <h2 class="auction-card-title">{{ title }}</h2>
        <span
          class="text-badge tracking-wide uppercase"
          :class="badge.style"
        >
          {{ badge.label }}
        </span>
      </div>
      <p class="text-meta">Your bid {{ formatCurrency(item.myHighestBid) }}</p>
      <p class="text-meta text-ink-muted">Current bid {{ formatCurrency(item.currentBid) }}</p>
    </div>
  </RouterLink>
</template>
