<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import { formatCurrency, formatTimeLeft } from '../format'
import type { AuctionListItem } from '../types'

// Used when the seller gave no photo. Decorative: the title already says what the car is.
const NO_PHOTO = '/images/auctions/no-photo.svg'

const props = defineProps<{
  auction: AuctionListItem
  /** Epoch milliseconds the time left is measured from. */
  now: number
}>()

const title = computed(() => {
  const { year, make, model } = props.auction.car
  return `${year} ${make} ${model}`
})
</script>

<template>
  <RouterLink :to="`/auctions/${auction.id}`" class="auction-card no-underline">
    <div class="auction-card-media">
      <img
        v-if="auction.car.imageUrl"
        :src="auction.car.imageUrl"
        :alt="title"
        width="960"
        height="640"
        loading="lazy"
      />
      <img v-else :src="NO_PHOTO" alt="" width="960" height="640" />

      <p class="image-chip absolute bottom-3 left-3">
        <svg
          aria-hidden="true"
          width="14"
          height="14"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2.5"
          stroke-linecap="round"
        >
          <circle cx="12" cy="12" r="9" />
          <path d="M12 7v5l3 2" />
        </svg>
        <span>{{ formatTimeLeft(auction.endsAt, now) }}</span>
        <span class="font-normal opacity-80">Bid</span>
        <span>{{ formatCurrency(auction.currentBid) }}</span>
      </p>
    </div>

    <div class="flex flex-col gap-1">
      <h3 class="auction-card-title">{{ title }}</h3>
    </div>
  </RouterLink>
</template>
