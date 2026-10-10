<script setup lang="ts">
import { computed } from 'vue'
import type { CarDetails } from '../types'

// Used when the seller gave no photo. Decorative: the heading already says what the car is.
const NO_PHOTO = '/images/auctions/no-photo.svg'

const mileage = new Intl.NumberFormat('en-US')

const props = defineProps<{
  car: CarDetails
  sellerName: string
}>()

const title = computed(() => `${props.car.year} ${props.car.make} ${props.car.model}`)
</script>

<template>
  <div class="stack">
    <h1 class="text-display font-bold">{{ title }}</h1>

    <div class="auction-card-media">
      <img v-if="car.imageUrl" :src="car.imageUrl" :alt="title" width="960" height="640" />
      <img v-else :src="NO_PHOTO" alt="" width="960" height="640" />
    </div>

    <dl class="grid grid-cols-[auto_1fr] gap-x-6 gap-y-2">
      <dt class="text-ink-muted">Mileage</dt>
      <dd>{{ mileage.format(car.mileageKm) }} km</dd>
      <dt class="text-ink-muted">Seller</dt>
      <dd>{{ sellerName }}</dd>
    </dl>

    <p v-if="car.description" class="max-w-[70ch]">{{ car.description }}</p>
  </div>
</template>
