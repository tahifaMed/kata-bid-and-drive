<script setup lang="ts">
import AuctionCard from '../components/AuctionCard.vue'
import { sampleAuctions } from '../data/sampleAuctions'

// Static page: filters and sort tabs are not wired yet.
const filters = ['Years', 'Transmission', 'Body style']
const sortTabs = ['Ending soon', 'Newly listed', 'No reserve', 'Lowest mileage', 'Closest to me']
</script>

<template>
  <div class="min-h-dvh">
    <header class="border-b border-border">
      <div class="container-app flex flex-wrap items-center gap-x-6 gap-y-3 py-4">
        <a href="/" class="text-lg font-bold no-underline">BidAndDrive</a>

        <nav class="flex flex-wrap items-center gap-x-5 gap-y-2" aria-label="Main">
          <a href="/auctions" class="no-underline">Auctions</a>
          <a href="#" class="no-underline">Watch list</a>
          <a href="#" class="no-underline">About</a>
        </nav>

        <div class="flex w-full flex-wrap items-center gap-3 sm:ml-auto sm:w-auto">
          <label for="auction-search" class="sr-only">Search auctions</label>
          <input
            id="auction-search"
            type="search"
            class="field sm:w-80"
            placeholder="Search for cars (ex. BMW, Audi, Ford)"
          />
          <button type="button" class="btn btn-outline">Sell a car</button>
          <button type="button" class="btn btn-primary">Sign up</button>
        </div>
      </div>
    </header>

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

        <nav class="flex flex-wrap gap-x-5 gap-y-2" aria-label="Sort auctions">
          <button
            v-for="(tab, index) in sortTabs"
            :key="tab"
            type="button"
            :class="['tab', index === 0 && 'tab-active']"
            :aria-current="index === 0 ? 'page' : undefined"
          >
            {{ tab }}
          </button>
        </nav>
      </div>

      <ul class="auction-grid">
        <li v-for="auction in sampleAuctions" :key="auction.id">
          <AuctionCard :listing="auction" />
        </li>
      </ul>
    </main>

    <footer class="border-t border-border">
      <div class="container-app py-6 text-meta text-ink-muted">
        <p class="mb-2">Sample photos from Wikimedia Commons:</p>
        <ul class="flex flex-col gap-1">
          <li v-for="auction in sampleAuctions" :key="auction.id">
            <a :href="auction.credit.url" target="_blank" rel="noopener">{{ auction.title }}</a
            >: {{ auction.credit.author }}, {{ auction.credit.license }}
          </li>
        </ul>
      </div>
    </footer>
  </div>
</template>
