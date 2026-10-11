<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import AppHeader from '../../../components/AppHeader.vue'
import { fetchOpenAuctions } from '../api'
import AuctionCard from '../components/AuctionCard.vue'
import type { AuctionListPage } from '../types'

// Static page: filters and sort tabs are not wired yet.
const filters = ['Years', 'Transmission', 'Body style']
const sortTabs = ['Ending soon', 'Newly listed', 'No reserve', 'Lowest mileage', 'Closest to me']

type LoadState = 'loading' | 'ready' | 'error'

const state = ref<LoadState>('loading')
// The page last asked for. Retry uses it, so a failed request is retried, not the page before it.
const requestedPage = ref(0)
const pageData = ref<AuctionListPage | null>(null)
// The page on screen comes from the last successful response, so a failed request does not move it.
const currentPage = computed(() => pageData.value?.page.number ?? 0)
// Time left on the cards is measured from when the data arrived.
const loadedAt = ref(Date.now())

const auctions = computed(() => pageData.value?._embedded.auctions ?? [])
const hasPrev = computed(() => Boolean(pageData.value?._links.prev))
const hasNext = computed(() => Boolean(pageData.value?._links.next))

async function load(page: number) {
  requestedPage.value = page
  state.value = 'loading'
  try {
    pageData.value = await fetchOpenAuctions(page)
    loadedAt.value = Date.now()
    state.value = 'ready'
  } catch {
    state.value = 'error'
  }
}

onMounted(() => load(0))
</script>

<template>
  <div class="min-h-dvh">
    <AppHeader>
      <template #search>
        <label for="auction-search" class="sr-only">Search auctions</label>
        <input
          id="auction-search"
          type="search"
          class="field sm:w-80"
          placeholder="Search for cars (ex. BMW, Audi, Ford)"
        />
      </template>
      <RouterLink to="/auctions/new" class="btn btn-outline">Sell a car</RouterLink>
      <button type="button" class="btn btn-primary">Sign up</button>
    </AppHeader>

    <main class="container-app stack-section py-6">
      <div class="flex flex-wrap items-center justify-between gap-4">
        <div class="flex flex-wrap items-center gap-3">
          <h1 class="text-display font-bold">Live auctions</h1>
          <button
            v-for="filter in filters"
            :key="filter"
            type="button"
            class="btn btn-outline px-4 text-meta font-normal"
          >
            {{ filter }}
            <svg
              aria-hidden="true"
              width="12"
              height="12"
              viewBox="0 0 12 12"
              fill="none"
              stroke="currentColor"
              stroke-width="1.5"
            >
              <path d="M2.5 4.5 6 8l3.5-3.5" />
            </svg>
          </button>
        </div>

        <div class="flex flex-wrap gap-x-4 gap-y-2" role="group" aria-label="Sort auctions">
          <button
            v-for="(tab, index) in sortTabs"
            :key="tab"
            type="button"
            :class="['tab', index === 0 && 'tab-active']"
            :aria-pressed="index === 0 ? 'true' : 'false'"
          >
            {{ tab }}
          </button>
        </div>
      </div>

      <p v-if="state === 'loading'" role="status" class="text-ink-muted">Loading auctions…</p>

      <div v-else-if="state === 'error'" role="alert" class="flex flex-wrap items-center gap-3">
        <p>Could not load auctions.</p>
        <button type="button" class="btn btn-outline" @click="load(requestedPage)">
          Try again
        </button>
      </div>

      <p v-else-if="auctions.length === 0" class="text-ink-muted">
        No auctions are open right now.
      </p>

      <ul v-else class="auction-grid">
        <li v-for="auction in auctions" :key="auction.id">
          <AuctionCard :auction="auction" :now="loadedAt" />
        </li>
      </ul>

      <nav
        v-if="pageData"
        class="flex items-center justify-between gap-4"
        aria-label="Auction pages"
      >
        <button
          type="button"
          class="btn btn-outline"
          :disabled="state === 'loading' || !hasPrev"
          @click="load(currentPage - 1)"
        >
          Previous
        </button>
        <span class="text-meta text-ink-muted">Page {{ currentPage + 1 }}</span>
        <button
          type="button"
          class="btn btn-outline"
          :disabled="state === 'loading' || !hasNext"
          @click="load(currentPage + 1)"
        >
          Next
        </button>
      </nav>
    </main>
  </div>
</template>
