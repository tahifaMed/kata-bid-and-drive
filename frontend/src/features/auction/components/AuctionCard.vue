<script setup lang="ts">
import { formatCurrency } from '../format'
import type { AuctionListing } from '../types'

defineProps<{ listing: AuctionListing }>()
</script>

<template>
  <article class="auction-card">
    <div class="auction-card-media">
      <img :src="listing.image" :alt="listing.imageAlt" width="960" height="640" loading="lazy" />

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
        <span>{{ listing.timeLeft }}</span>
        <span class="font-normal opacity-80">Bid</span>
        <span>{{ formatCurrency(listing.currentBid) }}</span>
      </p>
    </div>

    <div class="flex flex-col gap-1">
      <h3 class="auction-card-title">{{ listing.title }}</h3>
      <p v-if="listing.noReserve">
        <span class="badge-info">No reserve</span>
      </p>
      <p class="auction-card-spec">{{ listing.specs }}</p>
      <p class="auction-card-meta">{{ listing.location }}</p>
    </div>
  </article>
</template>
