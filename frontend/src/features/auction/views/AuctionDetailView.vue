<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import AppHeader from '../../../components/AppHeader.vue'
import AppToast from '../../../components/AppToast.vue'
import BidPanel from '../../bidding/components/BidPanel.vue'
import { useIdentityStore } from '../../identity/stores/identity'
import BidHistory from '../components/BidHistory.vue'
import CarDetails from '../components/CarDetails.vue'
import PriceSummary from '../components/PriceSummary.vue'
import { useNow } from '../../../composables/useNow'
import { useAuctionDetail } from '../composables/useAuctionDetail'
import { useAuctionStream } from '../composables/useAuctionStream'
import { formatCurrency } from '../format'
import type { BidPlacedEvent } from '../types'

const route = useRoute()
const { state, auction, load, retry, refresh, applyBidPlaced } = useAuctionDetail()
const now = useNow()
const identity = useIdentityStore()

// The same view is reused when only the id changes, so watch the param instead of loading once.
watch(
  () => route.params.id,
  (id) => load(typeof id === 'string' ? id : ''),
  { immediate: true },
)

const biddable = computed(() => Boolean(auction.value?._links['place-bid']))

// Only an auction that was found gets a stream, so an unknown id does not retry against a 404.
const streamedId = computed(() => (state.value === 'ready' ? (auction.value?.id ?? null) : null))
const outbidMessage = ref<string | null>(null)

/** Applies the event, and tells the selected user when it took the lead from them. */
function onBidPlaced(event: BidPlacedEvent): void {
  const me = identity.currentUserId
  const wasHighest = auction.value?.highestBidderId === me
  if (applyBidPlaced(event) && wasHighest && event.data.highestBidderId !== me) {
    outbidMessage.value = `You have been outbid. The minimum bid is now ${formatCurrency(event.data.minNextBid)}.`
  }
}

// The message belongs to one user on one auction.
watch([() => identity.currentUserId, streamedId], () => {
  outbidMessage.value = null
})

const { status: streamStatus } = useAuctionStream(streamedId, {
  onBidPlaced,
  onReconnected: refresh,
})

const liveLabel = computed(() => {
  switch (streamStatus.value) {
    case 'live':
      return 'Live'
    case 'reconnecting':
      return 'Reconnecting'
    case 'unavailable':
      return 'Live updates unavailable'
    default:
      return ''
  }
})
</script>

<template>
  <div class="min-h-dvh">
    <AppHeader />

    <main class="container-app stack-section py-6">
      <p>
        <RouterLink to="/auctions">Back to auctions</RouterLink>
      </p>

      <p v-if="state === 'loading'" role="status" class="text-ink-muted">Loading auction…</p>

      <p v-else-if="state === 'not-found'">This auction was not found.</p>

      <div v-else-if="state === 'error'" role="alert" class="flex flex-wrap items-center gap-3">
        <p>Could not load this auction.</p>
        <button type="button" class="btn btn-outline" @click="retry()">Try again</button>
      </div>

      <div
        v-else-if="auction"
        class="grid gap-(--gap-grid) lg:grid-cols-[minmax(0,2fr)_minmax(0,1fr)]"
      >
        <CarDetails :car="auction.car" :seller-name="auction.seller.displayName" />

        <div class="stack-section">
          <section class="stack" aria-labelledby="price-title">
            <div class="flex flex-wrap items-baseline justify-between gap-x-4 gap-y-1">
              <h2 id="price-title" class="text-title font-bold">Price</h2>
              <p role="status" class="text-meta text-ink-muted">{{ liveLabel }}</p>
            </div>
            <PriceSummary :auction="auction" :now="now" />
          </section>

          <!-- Only an auction that takes bids gets the bid panel. -->
          <section v-if="biddable" class="stack" aria-labelledby="bid-panel-title">
            <h2 id="bid-panel-title" class="text-title font-bold">Place a bid</h2>
            <BidPanel :auction="auction" @placed="refresh" />
          </section>

          <section class="stack" aria-labelledby="history-title">
            <h2 id="history-title" class="text-title font-bold">Bid history</h2>
            <BidHistory :bids="auction.bids" />
          </section>
        </div>
      </div>
    </main>

    <!-- Mounted with the auction, before any message, so the message is announced when it appears. -->
    <AppToast
      v-if="state === 'ready'"
      :message="outbidMessage"
      @dismiss="outbidMessage = null"
    />
  </div>
</template>
