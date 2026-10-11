<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink } from 'vue-router'
import AppHeader from '../../../components/AppHeader.vue'
import AuctionForm from '../components/AuctionForm.vue'
import type { AuctionResponse } from '../types'

const created = ref<AuctionResponse | null>(null)

function onCreated(auction: AuctionResponse): void {
  created.value = auction
}
</script>

<template>
  <div class="min-h-dvh">
    <AppHeader />

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
