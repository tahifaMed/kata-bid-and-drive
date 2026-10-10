<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink } from 'vue-router'
import UserSwitcher from '../../../components/UserSwitcher.vue'
import AuctionForm from '../components/AuctionForm.vue'
import type { AuctionResponse } from '../types'

const created = ref<AuctionResponse | null>(null)

function onCreated(auction: AuctionResponse): void {
  created.value = auction
}
</script>

<template>
  <div class="min-h-dvh">
    <header class="border-b border-border">
      <div class="container-app flex flex-wrap items-center gap-x-6 gap-y-3 py-4">
        <RouterLink to="/" class="text-lg font-bold no-underline">BidAndDrive</RouterLink>
        <nav class="flex flex-wrap items-center gap-x-4" aria-label="Main">
          <RouterLink to="/auctions" class="no-underline">Auctions</RouterLink>
        </nav>
        <div class="flex w-full flex-wrap items-center gap-3 sm:ml-auto sm:w-auto">
          <UserSwitcher />
        </div>
      </div>
    </header>

    <main class="container-app stack-section py-6">
      <h1 class="text-display font-bold">Sell a car</h1>

      <section v-if="created" class="stack" aria-labelledby="confirmation-title">
        <h2 id="confirmation-title" class="text-display font-bold">Your auction is open</h2>
        <p>
          {{ created.car.year }} {{ created.car.make }} {{ created.car.model }} is listed with a starting price of
          {{ created.startingPrice }}.
        </p>
        <p>
          <RouterLink :to="`/auctions/${created.id}`" class="font-bold">View your auction</RouterLink>
        </p>
      </section>

      <AuctionForm v-else @created="onCreated" />
    </main>
  </div>
</template>
