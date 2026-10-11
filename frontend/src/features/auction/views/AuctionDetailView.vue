<script setup lang="ts">
import { computed, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import AppHeader from '../../../components/AppHeader.vue'
import BidPanel from '../../bidding/components/BidPanel.vue'
import BidHistory from '../components/BidHistory.vue'
import CarDetails from '../components/CarDetails.vue'
import PriceSummary from '../components/PriceSummary.vue'
import { useAuctionDetail } from '../composables/useAuctionDetail'

const route = useRoute()
const { state, auction, loadedAt, load, retry, refresh } = useAuctionDetail()

// The same view is reused when only the id changes, so watch the param instead of loading once.
watch(
  () => route.params.id,
  (id) => load(typeof id === 'string' ? id : ''),
  { immediate: true },
)

const biddable = computed(() => Boolean(auction.value?._links['place-bid']))
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
            <h2 id="price-title" class="text-title font-bold">Price</h2>
            <PriceSummary :auction="auction" :now="loadedAt" />
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
  </div>
</template>
